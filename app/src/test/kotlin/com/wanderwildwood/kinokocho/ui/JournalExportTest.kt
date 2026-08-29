package com.wanderwildwood.kinokocho.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.data.FullObservation
import com.wanderwildwood.kinokocho.data.MeasurementRow
import com.wanderwildwood.kinokocho.data.Observation
import com.wanderwildwood.kinokocho.data.ObservationCharacter
import com.wanderwildwood.kinokocho.data.ObservationPhoto
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipFile

/**
 * A backup is the one file that has to be right when everything else has gone wrong, so
 * what comes out of it is asserted rather than assumed.
 */
@RunWith(RobolectricTestRunner::class)
class JournalExportTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun aFind() = FullObservation(
        observation = Observation(
            id = 1, uuid = "u1", recordedAt = 1_756_000_000_000L,
            updatedAt = 1_756_000_000_000L,
            note = "smelled of almonds", placeNote = "the big oak below the spring",
            identifiedAs = "an Amanita, one of the white ones",
            kept = true, schemaVersion = 1,
        ),
        characters = listOf(
            ObservationCharacter(0, 1, "fruitbody_type", "gilled_stemmed", 1L),
            ObservationCharacter(0, 1, "cap_colour", "white", 1L),
            ObservationCharacter(0, 1, "cap_colour", "cream", 1L),
            ObservationCharacter(0, 1, "spore_print", MeasurementRow.NOT_TESTED, 1L),
            ObservationCharacter(0, 1, "size", MeasurementRow.encode("cap_width_mm", 70), 1L),
        ),
        photos = listOf(ObservationPhoto(0, 1, "cap", "one.jpg", 1L)),
    )

    @Test
    fun `the zip holds the journal and the photographs it names`() {
        // A photograph the row names but the disk does not have is left out rather than
        // failing the whole backup: a broken reference is the wrong thing to discover
        // while trying to make a copy.
        File(photoDir(context), "one.jpg").writeBytes(byteArrayOf(1, 2, 3))
        val zip = JournalExport.write(context, listOf(aFind()))
        val names = ZipFile(zip).use { z -> z.entries().toList().map { it.name } }
        assertTrue("journal.json" in names)
        assertTrue("photos/one.jpg" in names)
    }

    @Test
    fun `a missing photograph does not fail the backup`() {
        File(photoDir(context), "one.jpg").delete()
        val zip = JournalExport.write(context, listOf(aFind()))
        val names = ZipFile(zip).use { z -> z.entries().toList().map { it.name } }
        assertTrue("journal.json" in names)
        assertTrue("photos/one.jpg" !in names)
    }

    @Test
    fun `the three kinds of answer come out as three different things`() {
        // States, measurements and "I looked and could not say" all live in one table as
        // character rows. A backup that flattened them back into one list would lose the
        // difference the schema goes out of its way to keep.
        val zip = JournalExport.write(context, listOf(aFind()))
        val json = ZipFile(zip).use { z ->
            JSONObject(z.getInputStream(z.getEntry("journal.json")).readBytes().decodeToString())
        }
        val find = json.getJSONArray("journal").getJSONObject(0)

        val characters = find.getJSONObject("characters")
        assertEquals("gilled_stemmed", characters.getJSONArray("fruitbody_type").getString(0))
        assertEquals(2, characters.getJSONArray("cap_colour").length())

        assertEquals(70, find.getJSONObject("measurements").getInt("cap_width_mm"))
        assertEquals("spore_print", find.getJSONArray("lookedAtCouldNotSay").getString(0))

        // And none of the internal encodings leak into a file meant to be read by a
        // person with a text editor and no app.
        val text = find.toString()
        assertTrue(MeasurementRow.NOT_TESTED !in text)
        assertTrue("cap_width_mm=70" !in text)
    }

    @Test
    fun `what a person wrote down survives`() {
        val zip = JournalExport.write(context, listOf(aFind()))
        val text = ZipFile(zip).use { z ->
            z.getInputStream(z.getEntry("journal.json")).readBytes().decodeToString()
        }
        listOf("smelled of almonds", "the big oak below the spring",
               "an Amanita, one of the white ones").forEach {
            assertTrue("lost: $it", it in text)
        }
    }
}
