package com.wanderwildwood.kinokocho.schema

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * These run against the schema that actually ships, not a fixture. A schema that
 * cannot be loaded is a phone that asks no questions, and that should break here.
 */
@RunWith(RobolectricTestRunner::class)
class SchemaLoaderTest {

    private val schema: CharacterSchema = SchemaLoader.load(
        ApplicationProvider.getApplicationContext<android.content.Context>().assets
    )

    @Test
    fun `the shipped schema loads and validates`() {
        assertEquals(1, schema.version)
        assertTrue(schema.characters.size > 30)
        assertEquals(16, schema.colours.size)
    }

    @Test
    fun `the root character is answerable before anything else is known`() {
        assertTrue(schema.isApplicable("fruitbody_type", emptyMap()))
        assertTrue(schema.isApplicable("substrate", emptyMap()))
    }

    @Test
    fun `gill questions do not exist until there are gills`() {
        assertFalse(schema.isApplicable("gill_attachment", emptyMap()))
        assertFalse(
            schema.isApplicable("gill_attachment", mapOf("fruitbody_type" to setOf("bolete")))
        )
        assertTrue(
            schema.isApplicable("gill_attachment", mapOf("fruitbody_type" to setOf("gilled_stemmed")))
        )
    }

    @Test
    fun `stem questions do not exist when there is no stem`() {
        val stemless = mapOf("stipe_presence" to setOf("absent_attached"))
        listOf("ring", "stipe_base", "stipe_surface", "stipe_flesh").forEach {
            assertFalse("$it should not apply without a stem", schema.isApplicable(it, stemless))
        }
        val stemmed = mapOf("stipe_presence" to setOf("central"))
        listOf("ring", "stipe_base", "stipe_surface", "stipe_flesh").forEach {
            assertTrue("$it should apply with a stem", schema.isApplicable(it, stemmed))
        }
    }

    @Test
    fun `the follow-up to bruising only appears once bruising is confirmed`() {
        assertFalse(schema.isApplicable("bruising_colour", mapOf("bruising" to setOf("no"))))
        assertTrue(schema.isApplicable("bruising_colour", mapOf("bruising" to setOf("yes"))))
    }

    @Test
    fun `a mushroom that oozes nothing is not asked what colour the ooze is`() {
        assertFalse(schema.isApplicable("latex_colour", mapOf("latex" to setOf("none"))))
        assertTrue(schema.isApplicable("latex_colour", mapOf("latex" to setOf("present"))))
    }

    @Test
    fun `colour-valued characters borrow the shared list`() {
        val capColour = schema.character("cap_colour")!!
        assertTrue(capColour.valuesFromColours)
        assertEquals(schema.colours.size, schema.valuesOf(capColour).size)
        assertTrue(schema.valuesOf(capColour).any { it.id == "red_brown" })
    }

    @Test
    fun `the spore print is the deferred character that matters`() {
        val print = schema.character("spore_print")!!
        assertEquals(Character.Availability.DEFERRED, print.availability)
        assertTrue("must be distinguishable from unanswered", print.notTested)
        assertEquals(5, print.power)
    }

    @Test
    fun `characters where not looking is not the same as no are marked`() {
        // Someone who never cut the mushroom has not established that it does not
        // bruise. If these lose their "not tested" state, that distinction is gone.
        listOf("bruising", "latex", "odour", "taste", "spore_print", "koh").forEach {
            assertTrue("$it must offer 'not tested'", schema.character(it)!!.notTested)
        }
    }

    @Test
    fun `the volva character offers an honest way to say the base was not dug up`() {
        val base = schema.character("stipe_base")!!
        assertTrue(schema.valuesOf(base).any { it.id == "buried" })
        assertEquals(5, base.power)
    }

    @Test
    fun `gill attachment keeps iNaturalist's controlled list so it can be exported`() {
        val values = schema.valuesOf(schema.character("gill_attachment")!!).map { it.id }
        assertEquals(
            listOf("free", "adnexed", "adnate", "notched", "decurrent", "no_stem"),
            values,
        )
    }

    @Test
    fun `taste is present but low priority, since it stays gated`() {
        val taste = schema.character("taste")!!
        assertTrue(taste.notTested)
        assertTrue("must not be asked early", taste.power <= 3)
    }

    @Test
    fun `regional characters are flagged so a pack knows what it may refine`() {
        val regional = schema.characters.filter { it.regional }.map { it.id }
        assertEquals(listOf("habitat", "associated_tree"), regional)
    }

    @Test
    fun `every dependency resolves and there are no cycles`() {
        // Loading already validates; this asserts the dependency graph is non-trivial
        // so the test cannot pass against an empty one.
        assertTrue(schema.dependencies.size >= 15)
        schema.dependencies.forEach {
            assertNotNull(schema.character(it.character))
            assertNotNull(schema.character(it.requiresCharacter))
        }
    }

    @Test
    fun `a broken dependency is refused rather than quietly ignored`() {
        val broken = """
            {"schemaVersion":1,"colours":[],
             "characters":[{"id":"a","label":"A","cardinality":"single","availability":"field",
                            "power":1,"values":[{"id":"x","label":"X"}]}],
             "dependencies":[{"character":"a","requires":{"character":"nope","anyOf":["x"]}}]}
        """.trimIndent()
        val failure = runCatching { SchemaLoader.parse(broken) }.exceptionOrNull()
        assertNotNull("a dependency on a missing character must fail", failure)
    }
}
