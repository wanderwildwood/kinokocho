package com.wanderwildwood.kinokocho.net

import android.content.Context
import android.content.SharedPreferences

/**
 * What the app remembers about being signed in, and for how long.
 *
 * Two tokens, and confusing them is the way this feature breaks a day after it is
 * built:
 *
 * - The **access token** from `/oauth/token` does not expire. iNaturalist returns
 *   `"expires_in": null`. It is the thing worth keeping, and the only thing here worth
 *   treating as a secret.
 * - The **API token** from `/users/api_token` is a JWT and lasts **24 hours** —
 *   `JsonWebToken.encode( payload, expiration = 24.hours.from_now )` in their source.
 *   It is what the v1 API actually accepts. Storing one of these and treating it as a
 *   login is how an app works on the day it was written and stops working the next
 *   morning with an unhelpful 401.
 *
 * So: keep the access token for ever, mint a JWT from it when the one in hand is stale,
 * and never ask the reader to sign in twice for a reason that is really a clock.
 *
 * These live in the app's own private preferences. Not [androidx.security] encrypted
 * preferences: that is a dependency and a keystore round trip to protect a token that
 * is already unreadable to every other app on the phone, and it would be protecting it
 * from an attacker who, having root, would simply read it anyway. What it does buy is
 * that a full-device backup would carry the token off the phone — which is why
 * `android:allowBackup="false"` is already set in the manifest.
 */
class INatAccount(context: Context) {



    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("inaturalist", Context.MODE_PRIVATE)

    val accessToken: String? get() = prefs.getString(ACCESS_TOKEN, null)

    /** The iNaturalist login this phone is signed in as, for showing on the button. */
    val login: String? get() = prefs.getString(LOGIN, null)

    val signedIn: Boolean get() = accessToken != null

    fun signIn(accessToken: String) {
        prefs.edit()
            .putString(ACCESS_TOKEN, accessToken)
            .remove(API_TOKEN)
            .remove(API_TOKEN_AT)
            .apply()
    }

    fun rememberLogin(login: String) {
        prefs.edit().putString(LOGIN, login).apply()
    }

    /**
     * Forgets everything, so the next push has to sign in again.
     *
     * This does not reach iNaturalist to revoke anything — it cannot, and saying it
     * could would be a promise the app does not keep. What it does is drop the token
     * from this phone. Revoking the application's access is done on iNaturalist, and
     * the About text says so.
     */
    fun signOut() {
        prefs.edit().clear().apply()
    }

    /** The cached JWT, or null when there is none or it is old enough not to trust. */
    fun apiToken(now: Long = System.currentTimeMillis()): String? {
        val token = prefs.getString(API_TOKEN, null) ?: return null
        val fetched = prefs.getLong(API_TOKEN_AT, 0)
        return if (now - fetched < API_TOKEN_LIFETIME) token else null
    }

    fun rememberApiToken(token: String, now: Long = System.currentTimeMillis()) {
        prefs.edit().putString(API_TOKEN, token).putLong(API_TOKEN_AT, now).apply()
    }

    /**
     * What to tell iNaturalist about where the mushroom was.
     *
     * **Obscured by default, and it stays that way unless somebody changes it.** A
     * foraging patch is a thing people keep, and the default has to be the careful one.
     *
     * ⚠ Obscured is not the same as not sent, and the About says so in those words.
     * iNaturalist receives the true coordinates and stores them; what obscuring changes
     * is what the *public* sees, which is a random point inside a 0.2 degree cell —
     * about twenty kilometres — and a place name generated from that rather than the one
     * you typed. Their `obscure_place_guess` moves a hand-written place note into a
     * private field, so "the big oak below the spring" is on their server and not on the
     * page.
     *
     * The third state is null rather than the string "open": their model's GEOPRIVACIES
     * list is [obscured, private] and nothing else, and a value it does not recognise is
     * silently dropped to null anyway.
     *
     * There is deliberately no way to send nothing at all. An observation with no
     * coordinates fails `research_grade_candidate?`, which tests `georeferenced?` before
     * anything else — so it is Casual for ever, left out of exports, and passed over by
     * the people who might have named it. Obscured is the careful answer; blank is the
     * one that looks careful and quietly wastes the record.
     */
    var geoprivacy: String?
        get() = prefs.getString(GEOPRIVACY, OBSCURED).takeIf { it != OPEN }
        set(value) {
            prefs.edit().putString(GEOPRIVACY, value ?: OPEN).apply()
        }

    /**
     * The half-finished sign-in, kept on disk rather than in memory.
     *
     * A sign-in leaves the app entirely — the browser opens, and on a phone with little
     * memory this process can be killed while somebody is typing a password into it.
     * Holding the verifier in a companion object would work every time it was tested on
     * a developer's desk and fail on the device it is for, with a sign-in that silently
     * does nothing.
     *
     * The verifier is not a password and is worthless without the code that comes back,
     * and both are cleared the moment the exchange finishes either way.
     */
    var pendingVerifier: String?
        get() = prefs.getString(PENDING_VERIFIER, null)
        set(value) {
            prefs.edit().apply { if (value == null) remove(PENDING_VERIFIER) else putString(PENDING_VERIFIER, value) }.apply()
        }

    /**
     * The code the browser came back with, waiting for the app to be looking again.
     *
     * Written by [INatRedirectActivity] and read once by the screen. Through disk for
     * the same reason as above: the activity that catches the redirect may be the first
     * thing in a freshly started process.
     */
    var pendingCode: String?
        get() = prefs.getString(PENDING_CODE, null)
        set(value) {
            prefs.edit().apply { if (value == null) remove(PENDING_CODE) else putString(PENDING_CODE, value) }.apply()
        }

    fun clearPending() {
        prefs.edit().remove(PENDING_VERIFIER).remove(PENDING_CODE).apply()
    }

    companion object {
        const val OBSCURED = "obscured"
        const val PRIVATE = "private"

        /** Stored to mean "tell them nothing extra"; never sent. See [geoprivacy]. */
        const val OPEN = "open"

        private const val ACCESS_TOKEN = "access_token"
        private const val PENDING_VERIFIER = "pending_verifier"
        private const val PENDING_CODE = "pending_code"
        private const val GEOPRIVACY = "geoprivacy"
        private const val API_TOKEN = "api_token"
        private const val API_TOKEN_AT = "api_token_at"
        private const val LOGIN = "login"

        /**
         * Twenty-three hours, against a token that lives twenty-four.
         *
         * The hour of slack is for the push that starts at 23h59m: a token checked as
         * fresh and then used across several photograph uploads must still be fresh at
         * the end of them, and a large upload over a slow connection is not instant.
         */
        private const val API_TOKEN_LIFETIME = 23L * 60 * 60 * 1000
    }
}
