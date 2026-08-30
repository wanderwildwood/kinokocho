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
