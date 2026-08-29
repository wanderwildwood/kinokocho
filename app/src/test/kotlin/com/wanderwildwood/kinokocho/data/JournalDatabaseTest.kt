package com.wanderwildwood.kinokocho.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
class JournalDatabaseTest {

    private lateinit var db: JournalDatabase
    private lateinit var dao: JournalDao

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JournalDatabase::class.java,
        ).addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }).build()
        dao = db.journalDao()
    }

    @After
    fun close() = db.close()

    private suspend fun anObservation(uuid: String = "u1"): Long =
        dao.insert(
            Observation(
                uuid = uuid,
                recordedAt = 1_000L,
                updatedAt = 1_000L,
                schemaVersion = 1,
            )
        )

    @Test
    fun `an observation with nothing but a time is still a real entry`() = runTest {
        val id = anObservation()
        val found = dao.findOne(id)
        assertNotNull(found)
        assertEquals("", found!!.observation.note)
        assertTrue(found.characters.isEmpty())
        assertTrue(found.photos.isEmpty())
    }

    @Test
    fun `one character can hold several values at once`() = runTest {
        val id = anObservation()
        dao.addCharacter(ObservationCharacter(0, id, "cap_surface", "scaly", 1L))
        dao.addCharacter(ObservationCharacter(0, id, "cap_surface", "viscid", 1L))

        val values = dao.charactersOf(id).filter { it.characterId == "cap_surface" }
        assertEquals(setOf("scaly", "viscid"), values.map { it.valueId }.toSet())
    }

    @Test
    fun `recording the same value twice keeps the time it was first noticed`() = runTest {
        val id = anObservation()
        dao.addCharacter(ObservationCharacter(0, id, "hymenophore", "gills", 1_000L))
        dao.addCharacter(ObservationCharacter(0, id, "hymenophore", "gills", 9_000L))

        val rows = dao.charactersOf(id)
        assertEquals(1, rows.size)
        assertEquals(1_000L, rows.single().recordedAt)
    }

    @Test
    fun `a single-valued character replaces rather than accumulates`() = runTest {
        val id = anObservation()
        dao.setSingleCharacter(id, "hymenophore", "gills", 1L)
        dao.setSingleCharacter(id, "hymenophore", "pores", 2L)

        val rows = dao.charactersOf(id)
        assertEquals(1, rows.size)
        assertEquals("pores", rows.single().valueId)
    }

    @Test
    fun `the spore print can be added long after the walk`() = runTest {
        val id = anObservation()
        dao.addCharacter(ObservationCharacter(0, id, "hymenophore", "gills", 1_000L))
        // Next morning.
        dao.addCharacter(ObservationCharacter(0, id, "spore_print", "white", 90_000L))

        val print = dao.charactersOf(id).single { it.characterId == "spore_print" }
        assertEquals(90_000L, print.recordedAt)
        assertTrue("added later than the walk", print.recordedAt > 1_000L)
    }

    @Test
    fun `deleting an observation takes its characters and photographs with it`() = runTest {
        val id = anObservation()
        dao.addCharacter(ObservationCharacter(0, id, "hymenophore", "gills", 1L))
        dao.addPhoto(ObservationPhoto(0, id, "stipe_base", "a.jpg", 1L))

        dao.deleteObservation(id)

        assertNull(dao.findOne(id))
        assertTrue("characters orphaned", dao.charactersOf(id).isEmpty())
        assertTrue("photographs orphaned", dao.photosOf(id).isEmpty())
    }

    @Test
    fun `the push queue holds only what has not been published`() = runTest {
        val unpushed = anObservation("u1")
        val pushed = anObservation("u2")
        dao.update(
            dao.findOne(pushed)!!.observation.copy(
                inat = INatLink(uuid = "inat-uuid", pushedAt = 5_000L)
            )
        )

        val queue = dao.notYetPushed()
        assertEquals(listOf(unpushed), queue.map { it.observation.id })
    }

    @Test
    fun `only published observations are asked about`() = runTest {
        anObservation("u1")
        val pushed = anObservation("u2")
        dao.update(
            dao.findOne(pushed)!!.observation.copy(
                inat = INatLink(uuid = "inat-uuid", pushedAt = 5_000L)
            )
        )

        val waiting = dao.awaitingIdentification()
        assertEquals(listOf(pushed), waiting.map { it.id })

        // Once the community has named it, it drops out of the asking list.
        dao.update(
            dao.findOne(pushed)!!.observation.copy(
                inat = INatLink(
                    uuid = "inat-uuid",
                    pushedAt = 5_000L,
                    taxonId = 47347,
                    taxonName = "Amanita bisporigera",
                    identificationFetchedAt = 6_000L,
                )
            )
        )
        assertTrue(dao.awaitingIdentification().isEmpty())
    }

    @Test
    fun `the uuid minted in the field is what a retried push looks itself up by`() = runTest {
        anObservation("field-uuid")
        assertNotNull(dao.findByUuid("field-uuid"))
        assertNull(dao.findByUuid("never-written"))
    }

    @Test
    fun `a photograph can be taken back out by name`() = runTest {
        // By name, not by id: a picture taken a moment ago is still in the draft with
        // id 0, because addPhoto returns the new id and nothing writes it back. Deleting
        // by id left the row behind pointing at a file that had just been removed, and
        // the photograph came back as "(missing)" next time the entry was opened.
        val id = anObservation()
        dao.addPhoto(ObservationPhoto(0, id, "cap", "one.jpg", 1_000L))
        dao.addPhoto(ObservationPhoto(0, id, "underside", "two.jpg", 1_000L))
        assertEquals(2, dao.photosOf(id).size)

        dao.deletePhotoNamed(id, "one.jpg")
        val left = dao.photosOf(id)
        assertEquals(1, left.size)
        assertEquals("two.jpg", left.single().fileName)
    }

    @Test
    fun `removing a photograph leaves another observation's alone`() = runTest {
        val mine = anObservation("u1")
        val theirs = anObservation("u2")
        dao.addPhoto(ObservationPhoto(0, mine, "cap", "same.jpg", 1_000L))
        dao.addPhoto(ObservationPhoto(0, theirs, "cap", "same.jpg", 1_000L))

        dao.deletePhotoNamed(mine, "same.jpg")
        assertEquals(0, dao.photosOf(mine).size)
        assertEquals(1, dao.photosOf(theirs).size)
    }
}
