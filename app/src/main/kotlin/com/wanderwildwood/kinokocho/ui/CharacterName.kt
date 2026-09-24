package com.wanderwildwood.kinokocho.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import com.wanderwildwood.kinokocho.R
import com.wanderwildwood.kinokocho.key.KeyEngine
import com.wanderwildwood.kinokocho.schema.Character
import com.wanderwildwood.kinokocho.schema.CharacterSchema

/**
 * A character named for a list that has no heading over it.
 *
 * [Character.noun] is short on purpose — "Colour", "Surface", "Edge" — because the pages
 * that use it put a heading above saying which part of the mushroom is meant. A flat run
 * of names has no such heading, and "tells them apart: colour, colour" was the result:
 * two different characters, the cap's and the gills', rendering as the same word twice.
 *
 * So anywhere the names are run together, the part comes with them.
 *
 * The part is worded from strings.xml, in the reader's language. The noun is not: it
 * comes from the schema JSON, which is data and has not been taken up for translation.
 */
fun Character.inFull(resources: Resources): String =
    part()?.let { resources.getString(it.inFull, midSentence()) } ?: midSentence()

/** The noun as it reads inside a sentence: lower case, except an initialism like KOH. */
private fun Character.midSentence(): String =
    if (noun.length > 1 && noun.all { it.isUpperCase() }) noun else noun.lowercase()

/**
 * [inFull], always in English, whatever the phone is set to.
 *
 * For the description posted to iNaturalist, which goes into a public record rather
 * than onto this screen, and whose language is its own decision — see
 * [com.wanderwildwood.kinokocho.net.INatDescription]. It must read exactly as it did
 * before the interface text moved into strings.xml.
 */
fun Character.inFullInEnglish(): String =
    part()?.let { "${it.english} ${noun.lowercase()}" } ?: noun.lowercase()

/** Which parts are named in front of the noun. Whole, smell, spores and where are not. */
private enum class Part(@StringRes val inFull: Int, val english: String) {
    CAP(R.string.character_in_full_cap, "cap"),
    GILL(R.string.character_in_full_gill, "gill"),
    STEM(R.string.character_in_full_stem, "stem"),
    FLESH(R.string.character_in_full_flesh, "flesh"),
}

private fun Character.part(): Part? = when (group) {
    Character.Group.CAP -> Part.CAP
    Character.Group.GILLS -> Part.GILL
    Character.Group.STEM -> Part.STEM
    Character.Group.FLESH -> Part.FLESH
    else -> null
}

/**
 * Runs several labels together without them dissolving into one another.
 *
 * Two dozen value labels in the schema have a comma inside them — "Gilled, with a stem",
 * "Foetid, of rot or carrion", "Warty, with loose patches" — because that is how the
 * things are actually said. Joining those with a comma produces a run of fragments where
 * nobody can see where one answer ends and the next begins: a candidate page read
 * "Smell  Nothing distinctive, Foetid, of rot or carrion", which is two answers wearing
 * the shape of three.
 *
 * So the separator is chosen by what is being separated. Commas while the parts have
 * none, which is the ordinary case and reads as English; semicolons the moment any part
 * contains one, which is what semicolons have always been for. Nothing is rewritten and
 * no label has to be shortened to fit a punctuation mark.
 */
/**
 * What a candidate could not be checked on, for the row that names it — or null when it
 * was checked on everything answered. See [KeyEngine.Candidate.unchecked].
 */
fun KeyEngine.Candidate.uncheckedNote(schema: CharacterSchema, resources: Resources): String? =
    unchecked.takeIf { it.isNotEmpty() }
        ?.mapNotNull { schema.character(it)?.inFull(resources) }
        ?.let { resources.getString(R.string.candidate_unchecked, it.asPhrases()) }

fun List<String>.asPhrases(): String =
    joinToString(if (any { it.contains(',') }) "; " else ", ")
