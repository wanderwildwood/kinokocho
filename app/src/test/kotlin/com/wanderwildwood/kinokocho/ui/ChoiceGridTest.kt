package com.wanderwildwood.kinokocho.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoiceGridTest {

    @Test
    fun `rows that already fit keep their own height`() {
        assertNull(heightToFill(viewport = 600.dp, natural = 100.dp, count = 3, gap = 6.dp))
    }

    @Test
    fun `a small stretch divides the page into whole rows`() {
        // Three fit with 40dp to spare; the row grows by a sixth, well under the cap.
        val h = heightToFill(viewport = 400.dp, natural = 120.dp, count = 6, gap = 6.dp)
        assertNotNull(h)
        assertEquals(129.33f, h!!.value, 0.01f)
    }

    @Test
    fun `a row is never stretched past one and a half times its height`() {
        // Only one row fits; filling the page would make it 1.7 times as tall.
        val natural = 235.dp
        val h = heightToFill(viewport = 400.dp, natural = natural, count = 6, gap = 6.dp)
        assertNull(h)
        val ok = heightToFill(viewport = 400.dp, natural = 280.dp, count = 6, gap = 6.dp)
        assertTrue(ok == null || ok.value <= 280f * MAX_STRETCH)
    }
}
