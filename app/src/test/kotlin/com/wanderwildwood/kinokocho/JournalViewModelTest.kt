package com.wanderwildwood.kinokocho

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.data.JournalDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What actually reaches the database when somebody uses the app.
 *
 * This exists because of one bug, and the bug is the argument for the file.
 * `Observation.latitude` and `longitude` were declared, documented, exported, imported
 * and read by the iNaturalist payload — and **nothing ever wrote them**. The location
 * button formatted the fix into the place *text* and stopped, so the columns were dead
 * from the day they were added.
 *
 * Nothing noticed for two reasons. The coordinates were on the screen the whole time,
 * in the sentence beside the empty columns. And no test ever went from a person's action
 * to a stored row: the DAO tests wrote observations by hand, with coordinates, and
 * passed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class JournalViewModelTest {

    private lateinit var vm: JournalViewModel

    @Before
    fun setUp() {
        // Unconfined rather than a scheduler of our own, and runBlocking rather than
        // runTest, deliberately. The view model writes through Room, which does its work
        // on its own executors on real threads — so virtual time never advances past the
        // insert and every assertion below reads a row that has not been written yet.
        // These tests wait for the real thing to happen. See [settled].
        Dispatchers.setMain(Dispatchers.Unconfined)
        vm = JournalViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ApplicationProvider.getApplicationContext<Application>().deleteDatabase("journal.db")
    }

    private suspend fun storedDraft() =
        JournalDatabase.get(ApplicationProvider.getApplicationContext<Application>())
            .journalDao().findOne(vm.draft.value!!.observationId!!)!!.observation

    /**
     * Waits for the write the view model started, rather than assuming it landed.
     *
     * The wait is real seconds, not virtual ones, for the reason in the class comment:
     * Room writes on its own threads and virtual time runs straight past them. That makes
     * the budget a statement about the slowest machine this is allowed to run on rather
     * than about the code, so it is generous. Five seconds was not: the first insert opens
     * the database, and on a workstation busy with another build it missed, failing a test
     * about coordinates for reasons that had nothing to do with coordinates. The loop exits
     * as soon as the row lands, so a larger number costs nothing when things are well.
     */
    private suspend fun settled(until: () -> Boolean = { vm.draft.value?.observationId != null }) {
        withTimeout(30_000) {
            while (!until()) delay(10)
        }
        // The id appears on the draft before the row is finished being filled in, so one
        // more turn through the queue after the condition holds.
        delay(150)
    }

    @Test
    fun `a position taken from the phone is written to the columns, not only to the words`() = runBlocking {
        vm.startNewEntry()
        vm.answer("fruitbody_type", setOf("gilled_stemmed"))
        settled()

        vm.setPosition(35.61, -83.43)
        settled()

        val stored = storedDraft()
        assertEquals(35.61, stored.latitude!!, 0.0001)
        assertEquals(-83.43, stored.longitude!!, 0.0001)
    }

    /**
     * Typing the place is the default and asks for no permission, so most finds have
     * words and no numbers. That must stay a real state rather than becoming a zero.
     */
    @Test
    fun `a find whose place was only typed has no coordinates at all`() = runBlocking {
        vm.startNewEntry()
        vm.setPlaceNote("the big oak below the spring")
        settled()

        val stored = storedDraft()
        assertEquals("the big oak below the spring", stored.placeNote)
        assertNull(stored.latitude)
        assertNull(stored.longitude)
    }

    /** Reopening a find brings the numbers back, or the entry loses them on every edit. */
    @Test
    fun `coordinates survive the entry being closed and opened again`() = runBlocking {
        vm.startNewEntry()
        vm.setPosition(35.61, -83.43)
        settled()
        val id = vm.draft.value!!.observationId!!

        vm.close()
        vm.open(id)
        settled()

        assertEquals(35.61, vm.draft.value!!.latitude!!, 0.0001)

        // And an unrelated edit afterwards does not quietly blank them, which is what
        // would happen if the draft carried them and persist did not.
        vm.setNote("smelled of aniseed")
        settled()
        assertEquals(35.61, storedDraft().latitude!!, 0.0001)
    }

    private val dao
        get() = JournalDatabase.get(ApplicationProvider.getApplicationContext<Application>()).journalDao()

    /** Closing a find straight after typing keeps what was typed. */
    @Test
    fun `the last edit before a find is closed is written`() = runBlocking {
        vm.startNewEntry()
        val uuid = vm.draft.value!!.uuid
        vm.answer("fruitbody_type", setOf("gilled_stemmed"))
        vm.setNote("smelled of aniseed")
        vm.close()

        withTimeout(30_000) {
            while (dao.findByUuid(uuid)?.observation?.note != "smelled of aniseed") delay(10)
        }
        assertEquals(1, dao.count())
    }

    /** Save it, then straight back to the journal: the find is in the journal. */
    @Test
    fun `a find kept and closed at once is kept`() = runBlocking {
        vm.startNewEntry()
        val uuid = vm.draft.value!!.uuid
        vm.setNote("by the creek")
        vm.keep()
        vm.close()

        withTimeout(30_000) {
            while (dao.findByUuid(uuid)?.observation?.kept != true) delay(10)
        }
        assertEquals(1, dao.count())
    }

    /** Thrown away right after an answer, it does not come back as the find being keyed. */
    @Test
    fun `a find thrown away while its saves are queued stays gone`() = runBlocking {
        vm.startNewEntry()
        vm.answer("fruitbody_type", setOf("gilled_stemmed"))
        vm.setNote("the wrong mushroom")
        vm.discard()
        assertNull(vm.draft.value)

        // A later write goes through the same queue, so once it lands the discard has too.
        vm.startNewEntry()
        vm.setNote("the right one")
        settled()
        assertEquals(1, dao.count())
        assertEquals("the right one", dao.findUnclaimed()!!.observation.note)
    }

    /** An install that once signed in to iNaturalist keeps no trace of it once this version starts. */
    @Test
    fun `the old iNaturalist sign-in is deleted on start`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("inaturalist", android.content.Context.MODE_PRIVATE)
            .edit().putString("access_token", "test-token").putString("login", "ada_whitlock").commit()

        JournalViewModel(app)

        val left = app.getSharedPreferences("inaturalist", android.content.Context.MODE_PRIVATE)
        assertNull(left.getString("access_token", null))
        assertNull(left.getString("login", null))
    }
}
