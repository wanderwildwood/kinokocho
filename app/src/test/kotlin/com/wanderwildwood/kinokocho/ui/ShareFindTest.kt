package com.wanderwildwood.kinokocho.ui

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
 * The find, in words, for somebody who was not there.
 *
 * This is the app's stated purpose rather than a nicety — it records an observation so
 * that someone who can identify it has something worth looking at — so what actually
 * comes out of it is worth asserting rather than assuming.
 */
@RunWith(RobolectricTestRunner::class)
class ShareFindTest {

    private val assets = ApplicationProvider.getApplicationContext<Context>().assets
    private val schema = SchemaLoader.load(assets)
    private val pack = PackLoader.load(assets, "packs/southern-appalachia-v1.json")
    private val engine = KeyEngine(schema, pack)

    private fun aFind() = JournalViewModel.Draft(
        observationId = 1,
        uuid = "u",
        recordedAt = 1_756_000_000_000L,
        answers = KeyEngine.Answers(
            values = mapOf(
                "fruitbody_type" to setOf("gilled_stemmed"),
                "substrate" to setOf("soil"),
                "stipe_base" to setOf("sac_volva"),
                "hymenophore_colour" to setOf("white"),
                "cap_colour" to setOf("white"),
            ),
            notTested = setOf("spore_print"),
            measurements = mapOf("cap_width_mm" to 70),
            month = 9,
        ),
        placeNote = "the big oak below the spring",
        identifiedAs = "an Amanita, one of the white ones",
    )

    @Test
    fun `it reads as a description and not as an identification`() {
        val text = ShareFind.summary(schema, engine, aFind())
        println("\n----- shared find -----\n$text-----------------------")

        assertTrue("no place", text.contains("the big oak below the spring"))
        assertTrue("no recorded name", text.contains("an Amanita, one of the white ones"))
        assertTrue("no size", text.contains("70 mm"))
        assertTrue("no what-was-seen", text.contains("What I could see"))
        assertTrue("no could-not-say", text.contains("Looked at and could not say"))
        assertTrue("nothing left unruled", text.contains("Not ruled out"))

        // The claim it must never make, in any wording.
        assertFalse(text.contains("identified", ignoreCase = true) &&
            !text.contains("Nothing here is an identification"))
        assertTrue(text.contains("Nothing here is an identification"))

        // Character ids must never leak: whoever is being asked reads English.
        listOf("gilled_stemmed", "sac_volva", "hymenophore_colour", "cap_width_mm")
            .forEach { assertFalse("raw id `$it` leaked", text.contains(it)) }
    }

    @Test
    fun `a find with nothing recorded still says something`() {
        val bare = aFind().copy(
            answers = KeyEngine.Answers(month = 9),
            placeNote = "",
            identifiedAs = "",
        )
        val text = ShareFind.summary(schema, engine, bare)
        assertTrue(text.contains("A mushroom"))
        assertTrue(text.contains("Nothing here is an identification"))
        assertFalse("an empty find should claim no shortlist", text.contains("Not ruled out"))
    }
}
