package com.wanderwildwood.kinokocho.ui

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstAnswerTest {

    /** Requests held open until the test answers them, as the phone would later. */
    private class Pending {
        val answers = mutableMapOf<String, (String?) -> Unit>()
        val cancelled = mutableSetOf<String>()
        val asked = mutableListOf<String>()

        fun ask(source: String, answer: (String?) -> Unit): () -> Unit {
            asked += source
            answers[source] = answer
            return { cancelled += source }
        }
    }

    @Test
    fun `gps answers when the network provider finds nothing`() = runTest {
        // In a wood with no cell or Wi-Fi the network provider is on but has nothing.
        val pending = Pending()
        val result = async { firstAnswer<String>(listOf("network", "gps"), pending::ask) }
        testScheduler.runCurrent()
        assertEquals(listOf("network", "gps"), pending.asked)
        pending.answers.getValue("network")(null)
        pending.answers.getValue("gps")("here")
        assertEquals("here", result.await())
    }

    @Test
    fun `the first answer wins and the other request is cancelled`() = runTest {
        val pending = Pending()
        val result = async { firstAnswer<String>(listOf("network", "gps"), pending::ask) }
        testScheduler.runCurrent()
        pending.answers.getValue("gps")("from gps")
        assertEquals("from gps", result.await())
        assertTrue("network" in pending.cancelled)
        // A late answer from the cancelled one changes nothing and does not throw.
        pending.answers.getValue("network")("from network")
    }

    @Test
    fun `null when every source answers null`() = runTest {
        val pending = Pending()
        val result = async { firstAnswer<String>(listOf("network", "gps"), pending::ask) }
        testScheduler.runCurrent()
        pending.answers.getValue("network")(null)
        pending.answers.getValue("gps")(null)
        assertNull(result.await())
    }

    @Test
    fun `one source alone is asked alone`() = runTest {
        val pending = Pending()
        val result = async { firstAnswer<String>(listOf("network"), pending::ask) }
        testScheduler.runCurrent()
        assertEquals(listOf("network"), pending.asked)
        pending.answers.getValue("network")("here")
        assertEquals("here", result.await())
    }

    @Test
    fun `an answer given at once stops the rest being asked`() = runTest {
        val asked = mutableListOf<String>()
        val result = firstAnswer<String>(listOf("network", "gps")) { source, answer ->
            asked += source
            answer("at once")
            return@firstAnswer {}
        }
        assertEquals("at once", result)
        assertEquals(listOf("network"), asked)
    }

    @Test
    fun `a request that fails at once leaves the other to answer`() = runTest {
        val pending = Pending()
        val result = async {
            firstAnswer<String>(listOf("network", "gps")) { source, answer ->
                if (source == "network") {
                    answer(null)
                    return@firstAnswer {}
                }
                pending.ask(source, answer)
            }
        }
        testScheduler.runCurrent()
        pending.answers.getValue("gps")("here")
        assertEquals("here", result.await())
    }

    @Test
    fun `running out of time cancels every request still out`() = runTest {
        val pending = Pending()
        val result = withTimeoutOrNull(20_000L) {
            firstAnswer<String>(listOf("network", "gps"), pending::ask)
        }
        assertNull(result)
        assertEquals(setOf("network", "gps"), pending.cancelled)
    }
}
