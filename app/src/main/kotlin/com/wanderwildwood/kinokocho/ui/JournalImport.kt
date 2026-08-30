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
                            pictures[entry.name.removePrefix("photos/")] = zip.readBytes()
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
                    val name = photo.optString("file").takeIf { it.isNotBlank() } ?: continue
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
                        )
                    )
                }
            }
            added++
        }
        return Result(added = added, alreadyHere = already, photos = photos)
    }
}
