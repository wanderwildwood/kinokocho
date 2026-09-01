package com.wanderwildwood.kinokocho.net

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.data.JournalDao
import com.wanderwildwood.kinokocho.data.JournalDatabase
import com.wanderwildwood.kinokocho.data.Observation
import com.wanderwildwood.kinokocho.data.ObservationPhoto
import com.wanderwildwood.kinokocho.key.KeyEngine
import com.wanderwildwood.kinokocho.key.PackLoader
import com.wanderwildwood.kinokocho.schema.SchemaLoader
import com.wanderwildwood.kinokocho.ui.photoDir
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
 * Publishing, with the network replaced by a notebook.
 *
 * The properties asserted here are the ones that make a push over a bad connection safe,
 * and every one of them is a matter of *order* rather than of behaviour — so none of them
 * is visible in the source, and all of them survive being reordered into something that
 * reads better. That is what this file is for.
 */
@RunWith(RobolectricTestRunner::class)
class INatPushTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: JournalDatabase
    private lateinit var dao: JournalDao

    private val assets = context.assets
    private val schema = SchemaLoader.load(assets)
    private val pack = PackLoader.load(assets, "packs/southern-appalachia-v1.json")
    private val engine = KeyEngine(schema, pack)

    /** Every call made, and every write that had happened by the time it was made. */
    private val log = mutableListOf<String>()

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(context, JournalDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.journalDao()
    }

    @After
    fun close() {
        db.close()
        photoDir(context).listFiles()?.forEach { it.delete() }
    }

    /** Records what it was asked to do, and can be told to refuse from a given photo on. */
    private inner class Notebook(
        private val failFromPhoto: Int = Int.MAX_VALUE,
    ) : INatApi {
        var createdBody: String? = null

        override suspend fun apiToken() = INatClient.Result.Ok("jwt")

        override suspend fun createObservation(jwt: String, body: String):
            INatClient.Result<INatPayload.Created> {
            createdBody = body
            log += "create"
            return INatClient.Result.Ok(INatPayload.Created(id = 55, uuid = "inat-uuid"))
        }

        private var photos = 0

        override suspend fun uploadPhoto(
            jwt: String,
            observationId: Long,
            photoUuid: String,
            file: File,
        ): INatClient.Result<Long?> {
            photos++
            // The uuid the app sent, and whether it had written it down first. Read from
            // the database rather than from the argument, because the argument is what
            // it meant to send and the row is what would survive the process dying here.
            val stored = dao.photosOf(1).firstOrNull { it.fileName == file.name }?.uuid
            log += "photo:$photoUuid stored=$stored"
            if (photos >= failFromPhoto) return INatClient.Result.Failed("no signal")
            return INatClient.Result.Ok(900L + photos)
        }

        override suspend fun fetchObservation(jwt: String, uuid: String) =
            INatClient.Result.Ok("""{"results":[{"community_taxon":
                {"id":48701,"name":"Amanita bisporigera","rank":"species"}}]}""")
    }

    private suspend fun aFindWithPhotos(vararg names: String): JournalViewModel.Draft {
        val id = dao.insert(
            Observation(
                uuid = "FIELD-UUID-0001",
                recordedAt = 1_756_000_000_000L,
                updatedAt = 1_756_000_000_000L,
                placeNote = "the big oak below the spring",
                latitude = 35.89,
                longitude = -82.83,
                kept = true,
                schemaVersion = schema.version,
            )
        )
        photoDir(context).mkdirs()
        names.forEach { name ->
            File(photoDir(context), name).writeBytes(byteArrayOf(1, 2, 3))
            // No uuid: a photograph taken before there was anywhere to put one.
            dao.addPhoto(ObservationPhoto(observationId = id, slot = "cap", fileName = name, capturedAt = 1))
        }
        return JournalViewModel.Draft(
            observationId = id,
            uuid = "FIELD-UUID-0001",
            recordedAt = 1_756_000_000_000L,
            answers = KeyEngine.Answers(
                values = mapOf("fruitbody_type" to setOf("gilled_stemmed")),
                month = 9,
            ),
            photos = dao.photosOf(id),
        )
    }

    private fun push(api: INatApi) = INatPush(context, dao, INatAccount(context), api)

    /**
     * The find is findable before any picture is sent.
     *
     * If photographs went first, a connection dying halfway would leave pictures uploaded
     * against an observation this phone has no record of — nothing to find, nothing to
     * retry, and no way to tell it had happened.
     */
    @Test
    fun `the find is written down as published before a single photograph goes up`() = runTest {
        val draft = aFindWithPhotos("a.jpg", "b.jpg")
        val api = Notebook()

        var pushedAtFirstPhoto: String? = null
        val watched = object : INatApi by api {
            override suspend fun uploadPhoto(jwt: String, observationId: Long, photoUuid: String, file: File):
                INatClient.Result<Long?> {
                if (pushedAtFirstPhoto == null) {
                    pushedAtFirstPhoto = dao.findOne(draft.observationId!!)?.observation?.inat?.uuid
                }
                return api.uploadPhoto(jwt, observationId, photoUuid, file)
            }
        }

        push(watched).push(schema, engine, draft, INatAccount.OBSCURED)
        assertEquals("inat-uuid", pushedAtFirstPhoto)
    }

    /**
     * A photograph's uuid is on disk before the photograph is in the air.
     *
     * This is the picture the connection dies on. If the uuid were minted in memory and
     * written afterwards, the retry would mint a different one and iNaturalist would have
     * no way to know it had seen this photograph already — so the find would end up with
     * the same picture on it twice.
     */
    @Test
    fun `a photograph's uuid is written down before it is uploaded`() = runTest {
        val draft = aFindWithPhotos("a.jpg")
        push(Notebook()).push(schema, engine, draft, INatAccount.OBSCURED)

        val entry = log.single { it.startsWith("photo:") }
        val sent = entry.removePrefix("photo:").substringBefore(" stored=")
        val stored = entry.substringAfter("stored=")
        assertEquals("the uuid was not on disk when it was sent", sent, stored)
        assertTrue(sent.isNotBlank())
    }

    @Test
    fun `a push that stops halfway says how far it got and leaves the rest to send`() = runTest {
        val draft = aFindWithPhotos("a.jpg", "b.jpg", "c.jpg")
        val outcome = push(Notebook(failFromPhoto = 2))
            .push(schema, engine, draft, INatAccount.OBSCURED)

        outcome as INatPush.Outcome.Ok
        assertEquals(1, outcome.photosSent)
        assertEquals(3, outcome.photosTotal)
        // The find is up, so it can be opened and finished.
        assertEquals("inat-uuid", dao.findOne(draft.observationId!!)?.observation?.inat?.uuid)
        // And the two that did not go are still marked as not gone.
        assertEquals(2, dao.photosOf(draft.observationId!!).count { it.inatPhotoId == null })
    }

    /** Nothing is hammered: whatever refused one photograph will refuse the next four. */
    @Test
    fun `it stops at the first refusal rather than trying every remaining photograph`() = runTest {
        val draft = aFindWithPhotos("a.jpg", "b.jpg", "c.jpg")
        push(Notebook(failFromPhoto = 1)).push(schema, engine, draft, INatAccount.OBSCURED)
        assertEquals(1, log.count { it.startsWith("photo:") })
    }

    @Test
    fun `the field uuid is what goes up, so a retry is an update and not a second mushroom`() = runTest {
        val draft = aFindWithPhotos("a.jpg")
        val api = Notebook()
        push(api).push(schema, engine, draft, INatAccount.OBSCURED)
        assertTrue(api.createdBody!!.contains("field-uuid-0001"))
    }

    /**
     * The community's name, and never the app's own opinion, written where the reader's
     * own answer cannot be overwritten by it.
     */
    @Test
    fun `a name read back is kept apart from what the reader wrote down`() = runTest {
        val draft = aFindWithPhotos("a.jpg")
        val pusher = push(Notebook())
        pusher.push(schema, engine, draft, INatAccount.OBSCURED)

        val id = draft.observationId!!
        dao.update(dao.findOne(id)!!.observation.copy(identifiedAs = "some Russula, I thought"))

        assertEquals(1, pusher.refreshIdentifications())
        val after = dao.findOne(id)!!.observation
        assertEquals("Amanita bisporigera", after.inat.taxonName)
        assertEquals("some Russula, I thought", after.identifiedAs)
        assertNotNull(after.inat.identificationFetchedAt)
    }

    /** It never asks about a find it has not published. */
    @Test
    fun `nothing unpublished is ever asked about`() = runTest {
        dao.insert(
            Observation(uuid = "never-sent", recordedAt = 1, updatedAt = 1, kept = true, schemaVersion = 1)
        )
        assertEquals(0, push(Notebook()).refreshIdentifications())
        assertTrue(log.isEmpty())
        assertNull(dao.findByUuid("never-sent")!!.observation.inat.taxonName)
    }
}
