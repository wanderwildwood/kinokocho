package com.wanderwildwood.kinokocho.net

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the app remembers about being signed in, and what it defaults to. */
@RunWith(RobolectricTestRunner::class)
class INatAccountTest {

    private lateinit var account: INatAccount

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("inaturalist", Context.MODE_PRIVATE).edit().clear().commit()
        account = INatAccount(context)
    }

    /**
     * The default, and the single most consequential line in this feature.
     *
     * A person who never opens the setting publishes obscured. Anything else would mean
     * the app deciding, on their behalf and without asking, to put a foraging patch on a
     * map.
     */
    @Test
    fun `a phone that has never been asked publishes obscured`() {
        assertEquals(INatAccount.OBSCURED, account.geoprivacy)
    }

    /**
     * More private before less private.
     *
     * Somebody tapping "Change" to see what it does must not reach exact coordinates in
     * one tap. Obscured to private is a tightening; only the second tap loosens, by which
     * point they have read two descriptions saying so.
     */
    @Test
    fun `the setting tightens before it loosens`() {
        assertEquals(INatAccount.PRIVATE, INatAccount.next(INatAccount.OBSCURED))
        assertNull("private should come before exact", INatAccount.next(INatAccount.PRIVATE))
        assertEquals(INatAccount.OBSCURED, INatAccount.next(null))
    }

    @Test
    fun `the choice is remembered, and exact is stored without being sent as a word`() {
        account.geoprivacy = null
        assertNull(account.geoprivacy)
        account.geoprivacy = INatAccount.PRIVATE
        assertEquals(INatAccount.PRIVATE, account.geoprivacy)
    }

    /**
     * Signing out drops the token. It cannot reach iNaturalist to revoke anything and the
     * app says so rather than implying otherwise.
     */
    @Test
    fun `signing out lets go of everything, including a half-finished sign-in`() {
        account.signIn("token")
        account.rememberApiToken("jwt")
        account.pendingVerifier = "verifier"
        account.pendingCode = "code"

        account.signOut()

        assertNull(account.accessToken)
        assertNull(account.apiToken())
        assertNull(account.pendingVerifier)
        assertNull(account.pendingCode)
    }

    /** A day-long token, and an hour of slack for a slow upload that started late. */
    @Test
    fun `a token older than the day it lasts is not offered`() {
        val now = 1_756_000_000_000L
        account.rememberApiToken("jwt", now)
        assertEquals("jwt", account.apiToken(now))
        assertEquals("jwt", account.apiToken(now + 22 * 60 * 60 * 1000))
        assertNull("a token near its expiry was still offered", account.apiToken(now + 23 * 60 * 60 * 1000 + 1))
    }

    @Test
    fun `a new sign-in throws away the old day-token`() {
        account.signIn("first")
        account.rememberApiToken("jwt")
        account.signIn("second")
        assertNull(account.apiToken())
        assertTrue(account.signedIn)
    }
}
