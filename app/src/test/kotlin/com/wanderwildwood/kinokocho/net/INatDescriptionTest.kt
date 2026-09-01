package com.wanderwildwood.kinokocho.net

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.key.KeyEngine
import com.wanderwildwood.kinokocho.key.PackLoader
import com.wanderwildwood.kinokocho.schema.SchemaLoader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What an identifier reads.
 *
 * The share sheet writes to a person the reader chose; this writes into a queue of
 * strangers working through it. The differences are deliberate and are the thing worth
 * asserting, because they are the sort of thing a later tidy-up would "fix" by making
 * the two share one function again.
 */
@RunWith(RobolectricTestRunner::class)
class INatDescriptionTest {

    private val assets = ApplicationProvider.getApplicationContext<Context>().assets
    private val schema = SchemaLoader.load(assets)
    private val pack = PackLoader.load(assets, "packs/southern-appalachia-v1.json")
    private val engine = KeyEngine(schema, pack)

    private fun aFind(identifiedAs: String = "", note: String = "") = JournalViewModel.Draft(
        observationId = 1,
        uuid = "u",
        recordedAt = 1_756_000_000_000L,
        identifiedAs = identifiedAs,
        note = note,
        answers = KeyEngine.Answers(
            values = mapOf(
                "fruitbody_type" to setOf("gilled_stemmed"),
                "substrate" to setOf("soil"),
                "stipe_base" to setOf("sac_volva"),
            ),
            notTested = setOf("odour"),
            measurements = mapOf("cap_width_mm" to 70),
            month = 9,
        ),
    )

    private fun text(draft: JournalViewModel.Draft = aFind()) =
        INatDescription.of(schema, engine, draft)

    @Test
    fun `the characters go, in words rather than ids`() {
        val out = text()
        assertTrue(out, out.contains("What I could see"))
        assertTrue(out, out.contains("bag-like sac", ignoreCase = true) || out.contains("sac"))
        // Never a raw id. A person is reading this.
        assertFalse(out, out.contains("gilled_stemmed"))
        assertFalse(out, out.contains("stipe_base"))
    }

    @Test
    fun `size goes with what was seen`() {
        assertTrue(text().contains("70 mm"))
    }

    @Test
    fun `what was looked at and could not be said is included`() {
        assertTrue(text().contains("Looked at and could not say"))
    }

    /**
     * The first thing anyone experienced asks is whether the base came up or whether
     * there is a print. On iNaturalist that round trip can take a week, so it is
     * answered before it is asked.
     */
    @Test
    fun `what was not recorded is included`() {
        assertTrue(text().contains("Not recorded"))
    }

    /**
     * The one real difference from the share text, and the reason this is its own
     * function.
     *
     * An identifier reads a list of names under an observation as a claim to argue with.
     * The app's whole position is that it narrows and does not decide, and its opinion
     * is not what that queue is short of.
     */
    @Test
    fun `the shortlist does not go to iNaturalist`() {
        val out = text()
        assertFalse(out, out.contains("Not ruled out"))
        // The find above is a volva'd, soil-dwelling gilled mushroom, so Amanita is
        // certainly on the shortlist. It must not be in the text.
        assertFalse(out, out.contains("Amanita"))
    }

    /** iNaturalist has its own fields for both and shows them above the description. */
    @Test
    fun `the date and the place are not repeated`() {
        val out = text()
        assertFalse(out, out.contains("2025"))
        assertFalse(out, out.contains("A mushroom,"))
    }

    @Test
    fun `what the reader was already told is passed on, and quoted as theirs`() {
        val out = text(aFind(identifiedAs = "probably a Russula"))
        assertTrue(out, out.contains("Recorded as: probably a Russula"))
    }

    @Test
    fun `a note the reader wrote goes too`() {
        assertTrue(text(aFind(note = "smelled of aniseed")).contains("smelled of aniseed"))
    }

    /**
     * An identifier reading a tidy list of characters needs to know whether a person
     * answered them or a machine guessed them, and that nothing underneath is a claim.
     */
    @Test
    fun `it says no identification is being claimed`() {
        assertTrue(text().contains("No identification is claimed here."))
    }
}
