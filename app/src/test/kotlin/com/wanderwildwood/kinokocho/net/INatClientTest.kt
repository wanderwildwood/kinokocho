package com.wanderwildwood.kinokocho.net

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The plumbing, against a real socket.
 *
 * Everything else about publishing is tested without a network, which is right for the
 * parts that are decisions. This part is not a decision — it is a set of bytes leaving a
 * socket in an order somebody has to get right — and reasoning about it is exactly how a
 * multipart body ends up one CRLF short and is rejected only by the real server, in a
 * wood, weeks later.
 *
 * So this stands a real socket in front of the client — see [TestHttpServer], which is
 * hand-written because `com.sun.net.httpserver` is not on the Android unit-test
 * classpath — and reads what actually arrives.
 */
@RunWith(RobolectricTestRunner::class)
class INatClientTest {

    private lateinit var server: TestHttpServer
    private lateinit var account: INatAccount

    @Before
    fun start() {
        server = TestHttpServer()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("inaturalist", Context.MODE_PRIVATE).edit().clear().commit()
        account = INatAccount(context)
    }

    @After
    fun stop() = server.stop()

    // No courtesy pause: it is a second of real time per call, and what is under test
    // here is the bytes rather than the manners.
    private fun client() =
        INatClient(account, site = server.base, api = server.base, courtesyPause = 0)

    private fun header(name: String): String? = server.headers[name.lowercase()]

    // ---- the bug this file was written for ----------------------------------------

    /**
     * A redirect is a signed-out, not a puzzling success.
     *
     * ⚠ This does not reproduce something the live server was seen doing. It came from
     * `users_controller.rb`, where `api_token` opens `redirect_to login_path if
     * !current_user` — but the running API returns **401** for a bad Bearer, a malformed
     * one and no header at all, because Doorkeeper answers first. Checked, after the
     * guard was written, rather than assumed either way.
     *
     * It is worth holding regardless, because the failure it describes is silent:
     * followed, a redirect arrives as 200 and a page of HTML, the JSON parse finds
     * nothing, and the app reports a vague "did not return an API token" while keeping a
     * sign-in that has just been proved dead. And `HttpURLConnection` re-issues a
     * redirected POST as a GET with no body, so the same setting is what stops a publish
     * from quietly becoming a request that did nothing.
     */
    @Test
    fun `a redirect to the login page is a signed-out, not a puzzling success`() = runBlocking {
        account.signIn("dead-token")
        server.status = 302
        server.location = "${server.base}/login"
        server.response = "<html>You are being redirected.</html>"

        val result = client().apiToken()

        assertTrue(result is INatClient.Result.Failed)
        assertTrue((result as INatClient.Result.Failed).signedOut)
        // And the dead sign-in is actually let go of, which is what unsticks it.
        assertFalse(account.signedIn)
        // One request. Following the redirect would have made two.
        assertEquals(1, server.hits)
    }

    // ---- what actually goes out ----------------------------------------------------

    @Test
    fun `every request names this app and asks for json`() = runBlocking {
        server.response = """{"api_token":"jwt"}"""
        account.signIn("access")
        client().apiToken()

        assertTrue(header("User-Agent").orEmpty().startsWith("kinokocho/"))
        assertEquals("application/json", header("Accept"))
        // The access token goes to the Rails site as a Bearer token.
        assertEquals("Bearer access", header("Authorization"))
        assertEquals("/users/api_token", server.path)
    }

    @Test
    fun `the api token is minted once and then reused`() = runBlocking {
        server.response = """{"api_token":"jwt"}"""
        account.signIn("access")
        assertEquals("jwt", (client().apiToken() as INatClient.Result.Ok).value)
        assertEquals("jwt", (client().apiToken() as INatClient.Result.Ok).value)
        assertEquals("a second call went back to the server", 1, server.hits)
    }

    /** The JWT goes to the v1 API bare, as their swagger's apiKey security says. */
    @Test
    fun `an observation is posted as json with the token in the header`() = runBlocking {
        server.response = """[{"id":7,"uuid":"abc"}]"""
        val r = client().createObservation("thejwt", """{"observation":{}}""")

        assertEquals("POST", server.method)
        assertEquals("/observations", server.path)
        assertEquals("application/json", header("Content-Type"))
        assertEquals("thejwt", header("Authorization"))
        assertEquals(INatPayload.Created(7, "abc"), (r as INatClient.Result.Ok).value)
    }

    /**
     * The multipart body, read back off the wire.
     *
     * The uuid field is the one that matters: their controller looks for an existing
     * observation photo carrying it before making a new one, so a push retried after a
     * lost connection updates rather than duplicating. Misspelt or missing, every retry
     * would add another copy of the same photograph and nothing on this phone would say
     * so.
     */
    @Test
    fun `a photograph goes up as multipart, with the uuid that makes a retry safe`() = runBlocking {
        val file = File.createTempFile("cap", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        server.response = """{"photo":{"id":99}}"""

        val r = client().uploadPhoto("thejwt", observationId = 7, photoUuid = "PHOTO-UUID", file = file)

        val contentType = header("Content-Type").orEmpty()
        assertTrue(contentType, contentType.startsWith("multipart/form-data; boundary="))
        val boundary = contentType.substringAfter("boundary=")

        // ISO-8859-1 so the raw photograph bytes survive being looked at as text.
        val sent = server.body.toString(Charsets.ISO_8859_1)
        val crlf = "\r\n"
        assertTrue(sent, sent.startsWith("--$boundary$crlf"))
        assertTrue(sent, sent.endsWith("$crlf--$boundary--$crlf"))
        assertTrue(sent, sent.contains("name=\"observation_photo[observation_id]\""))
        assertTrue(sent, sent.contains("$crlf$crlf" + "7" + crlf))
        assertTrue(sent, sent.contains("name=\"observation_photo[uuid]\""))
        // Lower-cased, for the same reason the observation's uuid is.
        assertTrue(sent, sent.contains("photo-uuid"))
        assertTrue(sent, sent.contains("name=\"file\"; filename=\"${file.name}\""))
        assertTrue(sent, sent.contains("Content-Type: image/jpeg"))
        // The four bytes themselves arrived, unmangled, after the blank line that ends
        // the part's headers. Asserted on the bytes rather than on the text: a
        // photograph is not text, and a check that decoded it would pass while the file
        // went up corrupted.
        val fileStarts = indexOfBytes(server.body, byteArrayOf(1, 2, 3, 4))
        assertTrue("the file bytes are not in the body", fileStarts >= 0)
        assertEquals(
            "the bytes do not follow the blank line that ends the part headers",
            crlf + crlf,
            server.body.copyOfRange(fileStarts - 4, fileStarts).toString(Charsets.ISO_8859_1),
        )

        // Before the last assertion, not after it: a test whose final expression is
        // `file.delete()` returns a Boolean, and JUnit will not run a method that is not
        // void — it fails the whole class with an initialization error naming a method
        // that looks perfectly fine.
        file.delete()
        assertEquals(99L, (r as INatClient.Result.Ok).value)
    }

    @Test
    fun `signing in posts the verifier as a form and never a secret`() = runBlocking {
        server.response = """{"access_token":"granted"}"""
        val r = client().exchange("thecode", "theverifier")

        assertEquals("application/x-www-form-urlencoded", header("Content-Type"))
        assertEquals("/oauth/token", server.path)
        val sent = server.body.toString(Charsets.UTF_8)
        assertTrue(sent, sent.contains("code_verifier=theverifier"))
        assertTrue(sent, sent.contains("grant_type=authorization_code"))
        assertFalse(sent, sent.contains("client_secret"))
        assertNull(header("Authorization"))
        assertEquals("granted", (r as INatClient.Result.Ok).value)
    }

    // ---- what comes back -----------------------------------------------------------

    /**
     * Their words when they have them, ours when they do not. An app that says "error
     * 422" for a message the server took the trouble to write throws away the only thing
     * that would have told somebody what to change.
     */
    @Test
    fun `a refusal is reported in iNaturalist's own words`() = runBlocking {
        server.status = 422
        server.response = """{"errors":{"observed_on":["is not a date"]}}"""
        val r = client().createObservation("jwt", "{}")
        assertEquals("observed_on is not a date", (r as INatClient.Result.Failed).said)
    }

    @Test
    fun `a rate limit and a server fault are said as things to wait out`() = runBlocking {
        server.status = 429
        server.response = ""
        val busy = client().createObservation("jwt", "{}") as INatClient.Result.Failed
        assertTrue(busy.said, busy.said.contains("slower pace"))
        assertFalse("a rate limit is not a signed-out", busy.signedOut)

        server.status = 503
        val down = client().createObservation("jwt", "{}") as INatClient.Result.Failed
        assertTrue(down.said, down.said.contains("Nothing was lost"))
        assertFalse(down.signedOut)
    }

    /**
     * A day-long token refused early — a clock that moved, a secret rotated at the far
     * end — drops the cached JWT without signing anybody out, so that pressing publish
     * again mints a fresh one rather than failing until the cache ages out on its own.
     */
    @Test
    fun `a token refused early is thrown away but the sign-in is kept`() = runBlocking {
        account.signIn("access")
        account.rememberApiToken("stale")
        server.status = 401
        server.response = ""

        val r = client().createObservation("stale", "{}")

        assertTrue((r as INatClient.Result.Failed).signedOut)
        assertNull("the stale token was kept", account.apiToken())
        assertTrue("the sign-in should survive a stale token", account.signedIn)
    }

    /** Where in [haystack] [needle] starts, or -1. */
    private fun indexOfBytes(haystack: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) if (haystack[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }

    @Test
    fun `nothing is thrown when there is nothing to reach`() = runBlocking {
        val gone = client()
        server.stop()
        val r = gone.createObservation("jwt", "{}")
        assertTrue((r as INatClient.Result.Failed).said.contains("Could not reach"))
    }
}
