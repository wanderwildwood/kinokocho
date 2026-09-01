package com.wanderwildwood.kinokocho.ui

import android.content.Context
import android.net.Uri
import com.wanderwildwood.kinokocho.data.JournalDao
import com.wanderwildwood.kinokocho.data.MeasurementRow
import com.wanderwildwood.kinokocho.data.Observation
import com.wanderwildwood.kinokocho.data.ObservationCharacter
import com.wanderwildwood.kinokocho.data.ObservationPhoto
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Reads a journal back in from a zip written by [JournalExport].
 *
 * The other half of a backup, and the dangerous half. Getting notes *out* can only fail
 * by producing nothing; getting them back in can fail by destroying what is already
 * there — so this only ever adds.
 *
 * **Nothing is deleted and nothing is overwritten.** A find whose uuid is already in the
 * journal is left exactly as it is and counted as already here. The uuid is minted once
 * in the field and never changes, which is what makes that test meaningful: the same find
 * imported twice is one find, and a find edited on the phone since the backup keeps the
 * edit. Importing the same file ten times leaves the journal as it was after the first.
 *
 * The worst case this is built against is somebody restoring an old backup onto a phone
 * that already has newer finds on it. They keep both.
 */
object JournalImport {

    data class Result(
        val added: Int = 0,
        val alreadyHere: Int = 0,
        val photos: Int = 0,
        /** Set when the file could not be read at all. Nothing was changed. */
        val failed: String? = null,
    )

    suspend fun read(context: Context, uri: Uri, dao: JournalDao): Result {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return Result(failed = "That file could not be opened.")

        // The whole zip is read before anything is written. A half-read archive must not
        // leave a half-restored journal.
        var journal: JSONObject? = null
        val pictures = mutableMapOf<String, ByteArray>()
        runCatching {
            ZipInputStream(bytes.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.name == "journal.json" ->
                            journal = JSONObject(zip.readBytes().decodeToString())
                        entry.name.startsWith("photos/") && !entry.isDirectory ->
                            entry.name.removePrefix("photos/")
                                .takeIf(::isPlainFileName)
                                ?.let { pictures[it] = zip.readBytes() }
                    }
                    zip.closeEntry()
                }
            }
        }.getOrElse { return Result(failed = "That does not look like a journal backup.") }

        val finds = journal?.optJSONArray("journal")
            ?: return Result(failed = "No journal inside that file.")

        var added = 0
        var already = 0
        var photos = 0
        val dir = photoDir(context)

        for (i in 0 until finds.length()) {
            val find = finds.getJSONObject(i)
            val uuid = find.optString("uuid").takeIf { it.isNotBlank() } ?: continue
            if (dao.findByUuid(uuid) != null) {
                already++
                continue
            }

            val id = dao.insert(
                Observation(
                    uuid = uuid,
                    recordedAt = find.optLong("recordedAt", System.currentTimeMillis()),
                    updatedAt = System.currentTimeMillis(),
                    note = find.optString("note"),
                    placeNote = find.optString("place"),
                    identifiedAs = find.optString("identifiedAs"),
                    latitude = if (find.has("latitude")) find.optDouble("latitude") else null,
                    longitude = if (find.has("longitude")) find.optDouble("longitude") else null,
                    // It was in somebody's journal, so it is kept. An unclaimed find is
                    // one that was still being keyed out, and that state does not survive
                    // a backup any more than it survives a walk.
                    kept = true,
                    schemaVersion = find.optInt("schemaVersion", 1),
                    // Carried across so a restored journal knows what is already
                    // published. Absent from any backup written before there was
                    // anything to publish to, which reads as "never pushed" — true.
                    inat = com.wanderwildwood.kinokocho.data.INatLink(
                        uuid = find.optString("inatUuid").takeIf { it.isNotBlank() },
                        pushedAt = find.optLong("inatPushedAt").takeIf { it != 0L },
                        taxonId = find.optLong("inatTaxonId").takeIf { it != 0L },
                        taxonName = find.optString("inatTaxonName").takeIf { it.isNotBlank() },
                    ),
                )
            )
            val now = System.currentTimeMillis()

            find.optJSONObject("characters")?.let { characters ->
                characters.keys().forEach { characterId ->
                    val values = characters.optJSONArray(characterId) ?: return@forEach
                    for (v in 0 until values.length()) {
                        dao.addCharacter(
                            ObservationCharacter(0, id, characterId, values.getString(v), now)
                        )
                    }
                }
            }
            find.optJSONObject("measurements")?.let { measurements ->
                measurements.keys().forEach { key ->
                    dao.addCharacter(
                        ObservationCharacter(
                            0, id, "size",
                            MeasurementRow.encode(key, measurements.getInt(key)), now,
                        )
                    )
                }
            }
            find.optJSONArray("lookedAtCouldNotSay")?.let { skipped ->
                for (s in 0 until skipped.length()) {
                    dao.addCharacter(
                        ObservationCharacter(
                            0, id, skipped.getString(s), MeasurementRow.NOT_TESTED, now,
                        )
                    )
                }
            }
            find.optJSONArray("photos")?.let { list ->
                for (p in 0 until list.length()) {
                    val photo = list.getJSONObject(p)
                    // The name comes out of the JSON inside the archive, and it is used
                    // to build a path. A backup written by this app carries a uuid and
                    // ".jpg", but nothing about reading a file somebody handed you
                    // guarantees that: "../../databases/journal.db" is a valid string in
                    // a JSON field, and File(photoDir, it) resolves happily out of the
                    // photo directory and into the rest of the app's own storage.
                    //
                    // Restoring a backup is the one moment this app takes a whole file
                    // from outside itself, and it is reached when something has already
                    // gone wrong and somebody is anxious — which is not when anyone
                    // inspects a zip before opening it.
                    val name = photo.optString("file")
                        .takeIf { it.isNotBlank() && isPlainFileName(it) } ?: continue
                    val file = File(dir, name)
                    // A file already on disk is left alone: names are uuids, so one that
                    // is there is the same picture, and rewriting it would be busywork at
                    // best and a truncated photograph at worst.
                    pictures[name]?.let { data ->
                        if (!file.exists()) {
                            runCatching { file.writeBytes(data) }.onSuccess { photos++ }
                        }
                    }
                    dao.addPhoto(
                        ObservationPhoto(
                            observationId = id,
                            slot = photo.optString("slot"),
                            fileName = name,
                            capturedAt = find.optLong("recordedAt", now),
                            uuid = photo.optString("uuid"),
                        )
                    )
                }
            }
            added++
        }
        return Result(added = added, alreadyHere = already, photos = photos)
    }

    /**
     * A bare file name, and nothing that can climb out of the directory it is joined to.
     *
     * Photographs written by [JournalExport] are a uuid and an extension, so this refuses
     * nothing a real backup contains. It refuses what a hand-made one could: a separator
     * of either slash, a `..` segment, an absolute path, and the two names that mean a
     * directory rather than a file.
     *
     * Applied to the zip's own entry names as well as to the names in the JSON, because
     * either half of the archive can be written by hand and the two are matched by that
     * string — a check on one of them is a check on neither.
     */
    internal fun isPlainFileName(name: String): Boolean =
        name.isNotBlank() &&
            !name.contains('/') &&
            !name.contains('\\') &&
            name != "." &&
            name != ".." &&
            !name.startsWith("..")
}
