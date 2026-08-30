package com.wanderwildwood.kinokocho.ui

import com.wanderwildwood.kinokocho.data.FullObservation
import com.wanderwildwood.kinokocho.data.Observation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * What a person remembers about a find they are trying to get back to.
 *
 * Not a date picker and not a taxon id: a name, a place, a month. Those are the terms the
 * memory actually arrives in — "that white one from September, below the spring" — and
 * they are what this has to match.
 */
class JournalSearchTest {

    private fun find(
        id: Long,
        identifiedAs: String = "",
        place: String = "",
        note: String = "",
        at: Long = 1_756_000_000_000L,
    ) = FullObservation(
        observation = Observation(
            id = id, uuid = "u$id", recordedAt = at, updatedAt = at,
            note = note, placeNote = place, identifiedAs = identifiedAs,
            kept = true, schemaVersion = 1,
        )
    )

    private val journal = listOf(
        find(1, identifiedAs = "Amanita bisporigera", place = "the big oak below the spring"),
        find(2, identifiedAs = "Cantharellus lateritius", place = "the ridge track"),
        find(3, note = "smelled strongly of aniseed", place = "the ridge track"),
    )

    @Test
    fun `an empty search is every find`() {
        assertEquals(3, journal.matching("").size)
        assertEquals(3, journal.matching("   ").size)
    }

    @Test
    fun `a name finds it`() {
        assertEquals(listOf(1L), journal.matching("amanita").map { it.observation.id })
        // Part of a name, and case does not matter: nobody types a binomial correctly
        // into a phone in a wood.
        assertEquals(listOf(2L), journal.matching("CANTHAREL").map { it.observation.id })
    }

    @Test
    fun `a place finds all of them`() {
        assertEquals(listOf(2L, 3L), journal.matching("ridge").map { it.observation.id })
    }

    @Test
    fun `something written in the note finds it`() {
        assertEquals(listOf(3L), journal.matching("aniseed").map { it.observation.id })
    }

    @Test
    fun `a month or a year finds what was found then`() {
        // The date is matched as the words it is shown as, so a month and a year both
        // work and neither needs a picker to say.
        val shown = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())
            .format(java.util.Date(1_756_000_000_000L))
        val month = shown.split(" ")[1]
        assertEquals(3, journal.matching(month).size)
        assertTrue(journal.matching("2025").isNotEmpty() || journal.matching("2026").isNotEmpty())
    }

    @Test
    fun `nothing matching finds nothing rather than everything`() {
        // The failure that matters: a search with no matches must not fall back to the
        // whole journal, or the reader is looking at three hundred finds believing they
        // are looking at the one they asked for.
        assertEquals(0, journal.matching("boletus").size)
    }
}
