package com.wanderwildwood.kinokocho.key

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The pack is read once, off the main thread, before the journal is drawn. What the view
 * model then asks for must be that same reading, not a second parse on the main thread.
 */
@RunWith(RobolectricTestRunner::class)
class KeyDataTest {

    private val assets = ApplicationProvider.getApplicationContext<Context>().assets

    @Test
    fun `the key is read once and the same copy is handed out after`() {
        val first = KeyData.get(assets)
        val second = KeyData.get(assets)
        assertSame(first, second)
        assertSame(first.engine, second.engine)
        assertTrue(first.pack.taxa.isNotEmpty())
        assertTrue(first.schema.characters.isNotEmpty())
    }
}
