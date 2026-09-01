package com.wanderwildwood.kinokocho.net

import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Signing in to iNaturalist without ever seeing a password.
 *
 * OAuth2 with PKCE (RFC 7636), which exists for exactly this case: an application that
 * cannot keep a secret because anyone can decompile it. Instead of proving who it is
 * with a shared secret baked into the APK, the app invents a random [verifier] per
 * sign-in, sends only its hash when asking for the code, and produces the original when
 * exchanging that code. An attacker who intercepts the redirect has the code and not
 * the verifier, so the code is worth nothing.
 *
 * ⚠ **The browser is the system browser, never a WebView.** Two reasons, and the second
 * is the one that actually bites. The principle: a WebView is this app drawing a login
 * form, and the whole point of the arrangement is that the password is typed somewhere
 * this app cannot read. The practical one: DuraSpeed on this device kills WebView
 * renderer processes, which cost a day on the birding app before anyone worked out why
 * a blank page was a blank page.
 *
 * Nothing here touches the network. It builds a URL and reads a response body, so all
 * of it can be tested without one — see `INatAuthTest`.
 */
object INatAuth {

    /**
     * A fresh code verifier: 32 random bytes, base64url, unpadded.
     *
     * RFC 7636 wants 43 to 128 characters from an unreserved alphabet, and 32 bytes
     * encodes to exactly 43. [SecureRandom] rather than [kotlin.random.Random], because
     * this is the only thing standing between a stolen redirect and a stolen account.
     */
    fun newVerifier(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return base64Url(bytes)
    }

    /**
     * The hash sent when asking for the code.
     *
     * S256, not `plain`. The verifier is hashed with SHA-256 over its *ASCII* bytes —
     * the verifier alphabet is ASCII by construction, so this is not a place where a
     * locale or a default charset can quietly change the answer.
     */
    fun challengeFor(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    /**
     * Where to send the reader's browser.
     *
     * No `scope` parameter: iNaturalist's Doorkeeper is configured with
     * `default_scopes :write, :login`, so omitting it asks for exactly what this needs
     * and nothing more. Naming `write` explicitly would be the same request with one
     * more thing that can drift out of step with their configuration.
     */
    fun authorizeUrl(clientId: String, redirectUri: String, challenge: String): String =
        buildString {
            append(SITE).append("/oauth/authorize")
            append("?client_id=").append(encode(clientId))
            append("&redirect_uri=").append(encode(redirectUri))
            append("&response_type=code")
            append("&code_challenge_method=S256")
            append("&code_challenge=").append(encode(challenge))
        }

    /** The form body that turns an authorization code into an access token. */
    fun tokenRequestBody(
        clientId: String,
        redirectUri: String,
        code: String,
        verifier: String,
    ): String = form(
        "client_id" to clientId,
        "code" to code,
        "redirect_uri" to redirectUri,
        "grant_type" to "authorization_code",
        "code_verifier" to verifier,
    )

    /**
     * Pulls the code out of the redirect the browser came back with.
     *
     * Returns null for anything that is not a code — including `?error=access_denied`,
     * which is what a reader tapping "no" on iNaturalist's consent screen produces, and
     * which is a decision rather than a failure.
     */
    fun codeFrom(uri: String): String? =
        Regex("[?&]code=([^&#]+)").find(uri)?.groupValues?.get(1)?.let(::decode)

    fun errorFrom(uri: String): String? =
        Regex("[?&]error=([^&#]+)").find(uri)?.groupValues?.get(1)?.let(::decode)

    const val SITE = "https://www.inaturalist.org"

    private fun base64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun form(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun decode(s: String): String = java.net.URLDecoder.decode(s, "UTF-8")
}
