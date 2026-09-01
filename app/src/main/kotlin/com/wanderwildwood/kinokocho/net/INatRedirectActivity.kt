package com.wanderwildwood.kinokocho.net

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.wanderwildwood.kinokocho.MainActivity

/**
 * Where the browser lands on the way back from iNaturalist.
 *
 * It writes the code down, brings the app forward and gets out of the way. Nothing is
 * exchanged here: this activity can be the first thing in a process that Android killed
 * while the reader was typing their password, so it holds no state of its own and
 * assumes none is waiting for it.
 *
 * `SINGLE_TOP` and `CLEAR_TOP` rather than a plain start, so coming back from the
 * browser returns to the journal that is already open rather than stacking a second copy
 * of it on top of the first.
 */
class INatRedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val account = INatAccount(this)
        val uri = intent?.data?.toString()

        if (uri != null) {
            val code = INatAuth.codeFrom(uri)
            if (code != null) {
                account.pendingCode = code
            } else {
                // `?error=access_denied` is a person tapping "no", which is an answer
                // rather than a fault. Either way the half-finished sign-in goes, so the
                // next attempt starts with a fresh verifier.
                account.clearPending()
            }
        }

        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        )
        finish()
    }
}
