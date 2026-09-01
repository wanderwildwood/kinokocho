package com.wanderwildwood.kinokocho.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Two dozen labels in the schema have a comma inside them, because that is how the things
 * are said: "Gilled, with a stem", "Foetid, of rot or carrion". Joined with a comma they
 * become a run of fragments where nobody can see where one answer ends.
 */
class CharacterNameTest {

    @Test
    fun `plain labels are run together with commas, as English`() {
        assertEquals("Soil or ground, Wood", listOf("Soil or ground", "Wood").asPhrases())
        assertEquals("White", listOf("White").asPhrases())
        assertEquals("", emptyList<String>().asPhrases())
    }

    /** The case from the candidate page: two answers wearing the shape of three. */
    @Test
    fun `a label with a comma in it escalates the whole list to semicolons`() {
        assertEquals(
            "Nothing distinctive; Foetid, of rot or carrion",
            listOf("Nothing distinctive", "Foetid, of rot or carrion").asPhrases(),
        )
    }

    /** One item is one item however it is punctuated - no separator to choose. */
    @Test
    fun `a single comma-bearing label is left exactly as it is`() {
        assertEquals("Gilled, with a stem", listOf("Gilled, with a stem").asPhrases())
    }
}
