package com.wanderwildwood.kinokocho.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.TimeZone

/** Dates read the way the phone writes them, like the other apps. */
@RunWith(RobolectricTestRunner::class)
class FindDateTest {

    // 2026-11-26 13:10 UTC = 8:10 in the morning in New York.
    private val at = 1_795_698_600_000L

    private fun inNewYork(block: () -> Unit) {
        val was = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        try { block() } finally { TimeZone.setDefault(was) }
    }

    @Test
    fun `US English on a 12-hour clock`() = inNewYork {
        assertEquals("Nov 26, 2026, 8:10 AM".replace(' ', ' '),
            FindDate.format(at, Locale.US, false).replace(' ', ' '))
    }

    @Test
    fun `US English on a 24-hour clock`() = inNewYork {
        assertEquals("Nov 26, 2026, 08:10", FindDate.format(at, Locale.US, true))
    }

    @Test
    fun `British English keeps the day first`() = inNewYork {
        assertEquals("26 Nov 2026, 08:10", FindDate.format(at, Locale.UK, true))
    }
}
