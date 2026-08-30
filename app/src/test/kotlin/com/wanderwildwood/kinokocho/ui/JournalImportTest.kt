package com.wanderwildwood.kinokocho.ui

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.data.FullObservation
import com.wanderwildwood.kinokocho.data.JournalDao
import com.wanderwildwood.kinokocho.data.JournalDatabase
import com.wanderwildwood.kinokocho.data.MeasurementRow
import com.wanderwildwood.kinokocho.data.Observation
import com.wanderwildwood.kinokocho.data.ObservationCharacter
import com.wanderwildwood.kinokocho.data.ObservationPhoto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Import is the dangerous half of a backup.
 *
 * Getting notes out can only fail by producing nothing. Getting them back in can fail by
 * destroying what is already there, so what it must *not* do is asserted harder than what
 * it does.
 */
@RunWith(RobolectricTestRunner::class)
class JournalImportTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: JournalDatabase
    private lateinit var dao: JournalDao

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(context, JournalDatabase::class.java).build()
        dao = db.journalDao()
    }

    @After
    fun close() = db.close()

    private fun aFind(uuid: String, note: String = "smelled of almonds") = FullObservation(
        observation = Observation(
            id = 1, uuid = uuid, recordedAt = 1_756_000_000_000L,
            updatedAt = 1_756_000_000_000L, note = note,
            placeNote = "the big oak", identifiedAs = "an Amanita",
            kept = true, schemaVersion = 1,
        ),
        characters = listOf(
            ObservationCharacter(0, 1, "fruitbody_type", "gilled_stemmed", 1L),
            ObservationCharacter(0, 1, "cap_colour", "white", 1L),
            ObservationCharacter(0, 1, "spore_print", MeasurementRow.NOT_TESTED, 1L),
            ObservationCharacter(0, 1, "size", MeasurementRow.encode("cap_width_mm", 70), 1L),
        ),
        photos = listOf(ObservationPhoto(0, 1, "cap", "one.jpg", 1L)),
    )

    private fun backupOf(vararg finds: FullObservation): Uri {
        File(photoDir(context), "one.jpg").writeBytes(byteArrayOf(1, 2, 3))
        return Uri.fromFile(JournalExport.write(context, finds.toList()))
    }

    @Test
    fun `a find survives the round trip whole`() = runTest {
        val result = JournalImport.read(context, backupOf(aFind("u1")), dao)
        assertNull(result.failed)
        assertEquals(1, result.added)

        val back = dao.findByUuid("u1")
        assertNotNull(back)
        assertEquals("smelled of almonds", back!!.observation.note)
        assertEquals("the big oak", back.observation.placeNote)
        assertEquals("an Amanita", back.observation.identifiedAs)
        assertEquals(1_756_000_000_000L, back.observation.recordedAt)
        assertTrue("an imported find is one somebody kept", back.observation.kept)

        // The three kinds of answer, still three kinds.
        val rows = back.characters
        assertTrue(rows.any { it.characterId == "cap_colour" && it.valueId == "white" })
        assertTrue(rows.any { it.characterId == "spore_print" && it.valueId == MeasurementRow.NOT_TESTED })
        assertTrue(rows.any { MeasurementRow.decode(it.valueId) == ("cap_width_mm" to 70) })
        assertEquals("one.jpg", back.photos.single().fileName)
    }

    @Test
    fun `reading the same copy twice adds nothing the second time`() = runTest {
        val backup = backupOf(aFind("u1"))
        assertEquals(1, JournalImport.read(context, backup, dao).added)
        val again = JournalImport.read(context, backup, dao)
        assertEquals(0, again.added)
        assertEquals(1, again.alreadyHere)
        assertEquals(1, dao.allKept().size)
    }

    @Test
    fun `an old copy never overwrites a newer find`() = runTest {
        // The worst case this is built against: restoring a backup onto a phone that has
        // been used since. The find on the phone wins, every time.
        val id = dao.insert(
            Observation(
                uuid = "u1", recordedAt = 1L, updatedAt = 1L,
                note = "what I actually wrote later", kept = true, schemaVersion = 1,
            )
        )
        val result = JournalImport.read(context, backupOf(aFind("u1", "the old note")), dao)
        assertEquals(0, result.added)
        assertEquals(1, result.alreadyHere)
        assertEquals("what I actually wrote later", dao.findOne(id)!!.observation.note)
    }

    @Test
    fun `nothing already in the journal is ever removed`() = runTest {
        dao.insert(Observation(uuid = "mine", recordedAt = 1L, updatedAt = 1L,
            kept = true, schemaVersion = 1))
        JournalImport.read(context, backupOf(aFind("u1")), dao)
        assertEquals(2, dao.allKept().size)
        assertNotNull(dao.findByUuid("mine"))
    }

    @Test
    fun `a file that is not a backup changes nothing and says so`() = runTest {
        dao.insert(Observation(uuid = "mine", recordedAt = 1L, updatedAt = 1L,
            kept = true, schemaVersion = 1))
        val junk = File(context.cacheDir, "not-a-backup.zip")
        junk.writeBytes("this is not a zip".toByteArray())

        val result = JournalImport.read(context, Uri.fromFile(junk), dao)
        assertNotNull("it should say what went wrong", result.failed)
        assertEquals(0, result.added)
        assertEquals(1, dao.allKept().size)
    }
}
