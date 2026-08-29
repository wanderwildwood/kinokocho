package com.wanderwildwood.kinokocho.data

/**
 * How a measurement is written into a character row.
 *
 * Measurements share the `observation_characters` table with states rather than living
 * in columns of their own, for the same reason states are rows: a region pack has to be
 * able to introduce a measurement without a database migration, and a number is no more
 * privileged than a state in that respect. So `cap_width_mm` and `45` are folded into
 * one stored string and unfolded on the way back.
 *
 * Kept here, apart from the view model, because a silent failure to unfold is a person's
 * measurements quietly disappearing between one session and the next — which is exactly
 * the kind of thing that has to be testable without an Android context.
 */
object MeasurementRow {

    /**
     * The stored value that means "I looked and could not say".
     *
     * Not a state of the mushroom and not a gap either — the schema is explicit that
     * these are different answers, because somebody who never cut the mushroom has not
     * established that it does not bruise. It was held in memory and never written
     * down, so it survived exactly as long as the app stayed open: reopen an entry and
     * every question you had looked at and given up on came back to be asked again.
     *
     * It rides in a character row like everything else. The marker begins with a
     * character no value id can contain — they are lower-case, digits and underscores —
     * so it can never be mistaken for a state some pack introduced later.
     */
    const val NOT_TESTED = "!not_tested"

    private const val SEPARATOR = '='

    fun encode(valueId: String, millimetres: Int): String = "$valueId$SEPARATOR$millimetres"

    /** The value id and its millimetres, or null if this row is not a measurement. */
    fun decode(stored: String): Pair<String, Int>? {
        val at = stored.indexOf(SEPARATOR)
        if (at <= 0) return null
        val millimetres = stored.substring(at + 1).toIntOrNull() ?: return null
        return stored.substring(0, at) to millimetres
    }
}
