package com.wanderwildwood.kinokocho.ui

import android.content.Context
import android.net.Uri
import com.wanderwildwood.kinokocho.R
import com.wanderwildwood.kinokocho.data.JournalDao
import com.wanderwildwood.kinokocho.data.MeasurementRow
import com.wanderwildwood.kinokocho.data.Observation
import com.wanderwildwood.kinokocho.data.ObservationCharacter
import com.wanderwildwood.kinokocho.data.ObservationPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
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

    /**
     * Off the main thread, and never more than one buffer of the file in memory at once.
     *
     * A backup carries every photograph full size, so a season's journal is easily larger
     * than the memory an app is given. Reading it whole failed exactly when it was needed
     * — the app could not read back the copy it had written — and below that size it held
     * the screen still for the whole restore. So the file is copied into the cache in
     * pieces and read from there as a stream, twice: once to check all of it and take the
     * journal, once to lay the photographs down one at a time.
     */
    suspend fun read(context: Context, uri: Uri, dao: JournalDao): Result =
        withContext(Dispatchers.IO) {
            val archive = copyToCache(context, uri)
                ?: return@withContext Result(failed = context.getString(R.string.import_could_not_open))
            try {
                restore(context, archive, dao)
            } finally {
                archive.delete()
            }
        }

    /** The picked file, copied into the cache in pieces. Null when it cannot be read. */
    private fun copyToCache(context: Context, uri: Uri): File? {
        val copy = runCatching { File.createTempFile("restore", ".zip", context.cacheDir) }
            .getOrNull() ?: return null
        val ok = runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: error("no stream")
            input.use { i -> copy.outputStream().use { i.copyTo(it) } }
        }.isSuccess
        if (!ok) copy.delete()
        return copy.takeIf { ok }
    }

    private suspend fun restore(context: Context, archive: File, dao: JournalDao): Result {
        // The whole zip is read through before anything is written, every entry inflated
        // and its checksum tested, though only the journal is kept. A half-read archive
        // must not leave a half-restored journal.
        val journal = runCatching { readJournal(archive) }
            .getOrElse { return Result(failed = context.getString(R.string.import_not_a_backup)) }

        val finds = journal?.optJSONArray("journal")
            ?: return Result(failed = context.getString(R.string.import_no_journal))

        var added = 0
        var already = 0
        val dir = photoDir(context)

        // The photographs the new finds will name, and that are not on disk already. They
        // go down before the rows that point at them, as they always have.
        val wanted = mutableSetOf<String>()
        for (i in 0 until finds.length()) {
            val find = finds.getJSONObject(i)
            val uuid = find.optString("uuid").takeIf { it.isNotBlank() } ?: continue
            if (dao.findByUuid(uuid) != null) continue
            val list = find.optJSONArray("photos") ?: continue
            for (p in 0 until list.length()) {
                val name = list.getJSONObject(p).optString("file")
                if (isPlainFileName(name) && !File(dir, name).exists()) wanted += name
            }
        }
        val written = writePhotos(archive, dir, wanted)

        for (i in 0 until finds.length()) {
            val find = finds.getJSONObject(i)
            val uuid = find.optString("uuid").takeIf { it.isNotBlank() } ?: continue
            if (dao.findByUuid(uuid) != null) {
                already++
                continue
            }

            val observation = Observation(
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
                // Carried across from a backup of a find an older version published
                // to iNaturalist, so nothing in the backup is lost. Unused now.
                inat = com.wanderwildwood.kinokocho.data.INatLink(
                    uuid = find.optString("inatUuid").takeIf { it.isNotBlank() },
                    pushedAt = find.optLong("inatPushedAt").takeIf { it != 0L },
                    taxonId = find.optLong("inatTaxonId").takeIf { it != 0L },
                    taxonName = find.optString("inatTaxonName").takeIf { it.isNotBlank() },
                ),
            )
            val now = System.currentTimeMillis()
            // Read whole before anything is written, with values that are not what this app
            // writes left out rather than thrown on, so one odd value cannot stop a restore
            // halfway through a find.
            val characters = mutableListOf<ObservationCharacter>()
            find.optJSONObject("characters")?.let { values ->
                values.keys().forEach { characterId ->
                    val list = values.optJSONArray(characterId) ?: return@forEach
                    for (v in 0 until list.length()) {
                        val value = list.optString(v).takeIf { it.isNotBlank() } ?: continue
                        characters += ObservationCharacter(0, 0, characterId, value, now)
                    }
                }
            }
            find.optJSONObject("measurements")?.let { measurements ->
                measurements.keys().forEach { key ->
                    val size = measurements.optInt(key, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE } ?: return@forEach
                    characters += ObservationCharacter(0, 0, "size", MeasurementRow.encode(key, size), now)
                }
            }
            find.optJSONArray("lookedAtCouldNotSay")?.let { skipped ->
                for (s in 0 until skipped.length()) {
                    val characterId = skipped.optString(s).takeIf { it.isNotBlank() } ?: continue
                    characters += ObservationCharacter(0, 0, characterId, MeasurementRow.NOT_TESTED, now)
                }
            }
            val photos = mutableListOf<ObservationPhoto>()
            find.optJSONArray("photos")?.let { list ->
                for (p in 0 until list.length()) {
                    val photo = list.optJSONObject(p) ?: continue
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
                    // A file another find already shows is that find's photograph; a
                    // backup cannot hand it to this one as well.
                    if (dao.photoFileUsed(name) > 0) continue
                    photos += ObservationPhoto(
                        observationId = 0,
                        slot = photo.optString("slot"),
                        fileName = name,
                        capturedAt = photo.optLong("capturedAt", find.optLong("recordedAt", now)),
                        uuid = photo.optString("uuid"),
                    )
                }
            }
            dao.importFind(observation, characters, photos)
            added++
        }
        return Result(added = added, alreadyHere = already, photos = written.size)
    }

    /** journal.json, after reading every entry of the archive through. */
    private fun readJournal(archive: File): JSONObject? {
        var journal: JSONObject? = null
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "journal.json") {
                    // Text only, so far below this however long the journal; a bigger one is
                    // not a backup this app wrote.
                    val bytes = readAtMost(zip, MAX_JOURNAL_BYTES + 1)
                    if (bytes.size > MAX_JOURNAL_BYTES) error("journal.json is too big")
                    journal = JSONObject(bytes.decodeToString())
                }
                // Closing an entry reads the rest of it, so a damaged photograph is found
                // here, before anything is written, as it was when the zip was read whole.
                zip.closeEntry()
            }
        }
        return journal
    }

    /**
     * Copies each wanted photograph straight out of the archive onto disk, one at a time,
     * and returns the names written.
     *
     * A file already on disk is left alone: names are uuids, so one that is there is the
     * same picture, and rewriting it would be busywork at best and a truncated photograph
     * at worst. For the same reason each one is written beside itself first and renamed
     * into place only once it is whole.
     */
    private fun writePhotos(archive: File, dir: File, wanted: Set<String>): Set<String> {
        val written = mutableSetOf<String>()
        if (wanted.isEmpty()) return written
        runCatching {
            ZipInputStream(archive.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name.removePrefix("photos/")
                    if (entry.name.startsWith("photos/") && !entry.isDirectory &&
                        isPlainFileName(name) && name in wanted && name !in written
                    ) {
                        val file = File(dir, name)
                        if (!file.exists() && writeOne(zip, file)) {
                            PhotoExif.stripLocation(file)
                            written += name
                        }
                    }
                    zip.closeEntry()
                }
            }
        }
        return written
    }

    private const val MAX_JOURNAL_BYTES = 32 * 1024 * 1024

    private fun readAtMost(input: InputStream, limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        while (out.size() < limit) {
            val n = input.read(buf, 0, minOf(buf.size, limit - out.size()))
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun writeOne(entry: InputStream, file: File): Boolean {
        val part = File(file.parentFile, ".${file.name}.part")
        val ok = runCatching {
            part.outputStream().use { entry.copyTo(it) }
            part.renameTo(file)
        }.getOrDefault(false)
        if (!ok) part.delete()
        return ok
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
