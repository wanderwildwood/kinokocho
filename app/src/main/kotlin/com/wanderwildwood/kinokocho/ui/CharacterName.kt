package com.wanderwildwood.kinokocho.ui

import com.wanderwildwood.kinokocho.schema.Character

/**
 * A character named for a list that has no heading over it.
 *
 * [Character.noun] is short on purpose — "Colour", "Surface", "Edge" — because the pages
 * that use it put a heading above saying which part of the mushroom is meant. A flat run
 * of names has no such heading, and "tells them apart: colour, colour" was the result:
 * two different characters, the cap's and the gills', rendering as the same word twice.
 *
 * So anywhere the names are run together, the part comes with them.
 */
fun Character.inFull(): String = when (group) {
    Character.Group.CAP -> "cap ${noun.lowercase()}"
    Character.Group.GILLS -> "gill ${noun.lowercase()}"
    Character.Group.STEM -> "stem ${noun.lowercase()}"
    Character.Group.FLESH -> "flesh ${noun.lowercase()}"
    else -> noun.lowercase()
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
fun List<String>.asPhrases(): String =
    joinToString(if (any { it.contains(',') }) "; " else ", ")
