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

    /**

     * True for a value that says the reader could not tell, rather than saying what the

     * mushroom is. Those must not be scored against a taxon.

     */

    fun isUncertainValue(characterId: String, valueId: String): Boolean =

        character(characterId)?.values?.firstOrNull { it.id == valueId }?.uncertain == true


    fun character(id: String): Character? = byId[id]

    /** The values a character offers, resolving the shared colour list. */
    fun valuesOf(character: Character): List<CharacterValue> =
        if (character.valuesFromColours) {
            colours.map { CharacterValue(it.id, it.label, gloss = it.gloss) }
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

/**
 * One state a character can take.
 *
 * [uncertain] marks a value that describes the observer rather than the mushroom —
 * "buried, I did not get the base out", "too old to tell". It must never eliminate a
 * taxon: scoring it as a mismatch would mean the honest answer is the one that hides
 * a deadly candidate, which is precisely backwards.
 */
data class CharacterValue(
    val id: String,
    val label: String,
    val uncertain: Boolean = false,
    /**
     * A few words saying what the term means, where the label alone will not.
     *
     * Carried by every colour term in the schema and shown by nothing, which made it the
     * one piece of data in the app that was written for exactly this screen and never
     * reached it. On a panel with no colour, "White" and "Cream / off-white" are two
     * words a person has to choose between with nothing to go on; "paper white, no tint"
     * against "whitish with a warm tint" is the whole difference.
     */
    val gloss: String? = null,
)

data class Character(
    val id: String,
    val label: String,
    /** Shown under the question. Plain language, and sometimes an instruction. */
    val hint: String? = null,
    /**
     * The same character as a noun, for reading rather than answering.
     *
     * [label] is a question because the key asks questions. On a page describing a
     * mushroom a question is the wrong shape — twenty-five rows each opening with a
     * whole sentence is what turned that page into prose with the answers lost in it.
     * Under a heading of [group] the noun can be very short: "Shape", "Edge", "Colour".
     */
    val noun: String,
    /** Which part of the mushroom this is about, so a description can be gathered up. */
    val group: Group,
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

    /**
     * The part of the mushroom a character belongs to.
     *
     * Declared order is reading order, and it is the order a person looks at a mushroom
     * in: what it is, then the cap, underneath, the stem, what is inside, what it
     * smells of, what the print says, and last where it was standing.
     */
    enum class Group(val heading: String) {
        WHOLE("The whole thing"),
        CAP("Cap"),
        GILLS("Underneath"),
        STEM("Stem"),
        FLESH("Flesh"),
        SMELL("Smell and taste"),
        SPORES("Spore print"),
        WHERE("Where and when"),
    }
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
