package com.wanderwildwood.kinokocho.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

/**
 * The sign-in, checked against the specification rather than against itself.
 *
 * A PKCE implementation that is wrong in the encoding is wrong in a way nothing here
 * would notice on its own: the app would produce a challenge, the server would reject
 * the exchange, and the only symptom would be a sign-in that does not work with no
 * indication which end was at fault. So the first test below uses RFC 7636's own
 * worked example, which pins the whole chain — SHA-256, base64url, no padding — to a
 * value written down by somebody else.
 */
class INatAuthTest {

    /**
     * RFC 7636 Appendix B, verbatim.
     *
     * This is the single most valuable assertion in this file. Base64 has a url-safe
     * variant and a padded variant and the difference is two characters at the end,
     * which is exactly the sort of thing that looks right in a debugger.
     */
    @Test
    fun `challenge matches the RFC 7636 worked example`() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            INatAuth.challengeFor("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test
    fun `a verifier is the length the spec allows and has no padding in it`() {
        val verifier = INatAuth.newVerifier(SecureRandom())
        // RFC 7636 section 4.1: 43 to 128 characters. 32 random bytes is exactly 43.
        assertEquals(43, verifier.length)
        assertTrue(verifier.none { it == '=' || it == '+' || it == '/' })
    }

    @Test
    fun `two verifiers are not the same verifier`() {
        assertTrue(INatAuth.newVerifier() != INatAuth.newVerifier())
    }

    @Test
    fun `the authorize url carries what iNaturalist needs and no scope`() {
        val url = INatAuth.authorizeUrl("abc", "kinokocho://oauth", "chal")
        assertTrue(url.startsWith("https://www.inaturalist.org/oauth/authorize?"))
        assertTrue(url.contains("client_id=abc"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("code_challenge=chal"))
        // The redirect must survive being put in a query string.
        assertTrue(url.contains("redirect_uri=kinokocho%3A%2F%2Foauth"))
        // Their Doorkeeper's default_scopes are already write and login. Asking again
        // is one more thing that can drift out of step with their configuration.
        assertTrue(!url.contains("scope="))
    }

    @Test
    fun `the token request sends the verifier and never a secret`() {
        val body = INatAuth.tokenRequestBody("abc", "kinokocho://oauth", "thecode", "theverifier")
        assertTrue(body.contains("grant_type=authorization_code"))
        assertTrue(body.contains("code=thecode"))
        assertTrue(body.contains("code_verifier=theverifier"))
        // The entire point of PKCE. If this ever appears, the flow has been replaced by
        // one that cannot be used safely from an APK.
        assertTrue(!body.contains("client_secret"))
    }

    @Test
    fun `a code is read back out of the redirect`() {
        assertEquals("xyz", INatAuth.codeFrom("kinokocho://oauth?code=xyz"))
        assertEquals("xyz", INatAuth.codeFrom("kinokocho://oauth?state=1&code=xyz&other=2"))
    }

    /**
     * A refusal is an answer, not a fault.
     *
     * Tapping "no" on iNaturalist's consent screen comes back as `error=access_denied`.
     * Reading that as a code would have the app try to exchange the word "access_denied"
     * and report whatever the server said about it.
     */
    @Test
    fun `a refusal is not mistaken for a code`() {
        val refused = "kinokocho://oauth?error=access_denied"
        assertNull(INatAuth.codeFrom(refused))
        assertEquals("access_denied", INatAuth.errorFrom(refused))
    }

    /**
     * `code_challenge` is in the same query string as `code`, and a lazy pattern finds
     * it. The app would then try to exchange its own challenge for a token.
     */
    @Test
    fun `code_challenge is not mistaken for code`() {
        assertNull(
            INatAuth.codeFrom("https://www.inaturalist.org/oauth/authorize?code_challenge=abc")
        )
    }
}
