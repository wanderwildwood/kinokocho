package com.wanderwildwood.kinokocho.schema

/**
 * The global character schema: what a person can be asked about a mushroom.
 *
 * This is data, not code, and it is loaded from `assets/schema/characters-v*.json`.
 * A region pack supplies taxon data and may refine the characters marked [regional];
 * it never redefines these ids. That is why [Observation]'s character rows store
 * strings - the vocabulary has to be able to grow without a database migration.
 */
data class CharacterSchema(
    val version: Int,
    val colours: List<ColourTerm>,
    val characters: List<Character>,
    val dependencies: List<Dependency>,
) {
    private val byId = characters.associateBy { it.id }

    fun character(id: String): Character? = byId[id]

    /** The values a character offers, resolving the shared colour list. */
    fun valuesOf(character: Character): List<CharacterValue> =
        if (character.valuesFromColours) {
            colours.map { CharacterValue(it.id, it.label) }
        } else {
            character.values
        }

    /**
     * Whether a character can be asked at all, given what has been answered so far.
     *
     * A character whose controlling answer has not been given yet is *not* applicable -
     * there is no point asking about gill spacing before knowing there are gills. A
     * character with no dependency is always applicable.
     */
    fun isApplicable(characterId: String, answers: Map<String, Set<String>>): Boolean =
        dependencies.filter { it.character == characterId }
            .all { dep -> answers[dep.requiresCharacter].orEmpty().any { it in dep.requiresAnyOf } }
}

data class ColourTerm(val id: String, val label: String, val gloss: String)

data class CharacterValue(val id: String, val label: String)

data class Character(
    val id: String,
    val label: String,
    /** Shown under the question. Plain language, and sometimes an instruction. */
    val hint: String? = null,
    val kind: Kind,
    val cardinality: Cardinality,
    val availability: Availability,
    /**
     * A hint at how early to ask, 1-5, used only until enough has been answered for
     * information gain over the live candidate set to take over. Latex is the reason
     * this is only a hint: it is near-decisive when present and absent for most taxa,
     * so a fixed order asks it far too early.
     */
    val power: Int,
    /** True when answering this makes other characters applicable or not. */
    val controlling: Boolean = false,
    /**
     * True when "I looked and cannot say" is a distinct answer from having skipped it.
     *
     * This matters more than it looks. Someone who never cut the mushroom has not
     * established that it does not bruise, and treating those two as the same answer
     * would quietly eliminate every bruising taxon.
     */
    val notTested: Boolean = false,
    /** Refined by the region pack rather than fixed globally. */
    val regional: Boolean = false,
    /** Why this character is shaped the way it is. Not shown to the reader. */
    val note: String? = null,
    val valuesFromColours: Boolean = false,
    val values: List<CharacterValue> = emptyList(),
) {
    enum class Kind { STATE, MEASUREMENT }
    enum class Cardinality { SINGLE, MULTI }

    /** Whether the answer can be had while standing over the mushroom. */
    enum class Availability {
        /** Observable on the spot. */
        FIELD,

        /** Needs hours, or reagents. The spore print is the one that matters. */
        DEFERRED,
    }
}

data class Dependency(
    val character: String,
    val requiresCharacter: String,
    val requiresAnyOf: Set<String>,
)
