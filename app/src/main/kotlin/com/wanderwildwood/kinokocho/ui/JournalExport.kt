package com.wanderwildwood.kinokocho.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.wanderwildwood.kinokocho.data.FullObservation
import com.wanderwildwood.kinokocho.data.MeasurementRow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The whole journal, written out so it can be kept somewhere else.
 *
 * [JournalDatabase] says of itself that it holds "notes that cannot be taken again — the
 * mushroom is gone and the season is over", and until now there was no way to get any of
 * them off the phone. That is a strange thing for a book to be missing. A phone is lost,
 * dropped in a stream, or simply replaced, and a journal that lives in exactly one place
 * is a journal with a date on it.
 *
 * A zip, because the photographs are half of it. Inside: `journal.json` with every kept
 * find, and `photos/` with the pictures they refer to by name.
 *
 * **It reads rather than writes.** Nothing here can alter the journal, which is the
 * property that matters in a thing you reach for when something has already gone wrong.
 * There is no import yet, and the format is written to be legible without one: plain
 * JSON with the schema's own ids, so a person with the file and a text editor has their
 * notes back even if this app is gone.
 */
object JournalExport {

    /** The file, written into the cache so the system can clear it when it likes. */
    fun write(context: Context, entries: List<FullObservation>): File {
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val out = File(context.cacheDir, "mushroom-journal-$stamp.zip")
        val photos = photoDir(context)

        ZipOutputStream(out.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("journal.json"))
            zip.write(json(entries).toString(2).toByteArray())
            zip.closeEntry()

            // Only the files that are actually there. A photograph deleted off the disk
            // but still named in a row is a broken reference, and a backup is the wrong
            // place to discover that — the entry keeps its name and the zip says nothing.
            entries.flatMap { it.photos }.map { it.fileName }.distinct().forEach { name ->
                val file = File(photos, name)
                if (!file.isFile) return@forEach
                zip.putNextEntry(ZipEntry("photos/$name"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return out
    }

    private fun json(entries: List<FullObservation>): JSONObject {
        val array = JSONArray()
        entries.forEach { entry ->
            val o = entry.observation
            array.put(JSONObject().apply {
                put("uuid", o.uuid)
                put("recordedAt", o.recordedAt)
                put("recorded", DATE.format(Date(o.recordedAt)))
                put("identifiedAs", o.identifiedAs)
                put("place", o.placeNote)
                put("note", o.note)
                o.latitude?.let { put("latitude", it) }
                o.longitude?.let { put("longitude", it) }
                put("schemaVersion", o.schemaVersion)

                // Characters as the schema's own ids, so the file is readable against the
                // schema rather than against this app.
                val characters = JSONObject()
                val measurements = JSONObject()
                val notTested = JSONArray()
                entry.characters.forEach { row ->
                    when {
                        row.valueId == MeasurementRow.NOT_TESTED -> notTested.put(row.characterId)
                        MeasurementRow.decode(row.valueId) != null ->
                            MeasurementRow.decode(row.valueId)?.let { (key, mm) ->
                                measurements.put(key, mm)
                            }
                        else -> characters.optJSONArray(row.characterId)
                            ?.put(row.valueId)
                            ?: characters.put(row.characterId, JSONArray().put(row.valueId))
                    }
                }
                put("characters", characters)
                put("measurements", measurements)
                put("lookedAtCouldNotSay", notTested)
                put("photos", JSONArray().apply {
                    entry.photos.forEach { put(JSONObject().put("slot", it.slot).put("file", it.fileName)) }
                })
            })
        }
        return JSONObject()
            .put("app", "kinokocho")
            .put("exported", DATE.format(Date()))
            .put("finds", array.length())
            .put("journal", array)
    }

    /** Hands the file to whatever the person wants to keep it in. */
    fun intent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private val DATE = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
}
