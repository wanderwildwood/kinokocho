package com.wanderwildwood.kinokocho.net

import com.wanderwildwood.kinokocho.BuildConfig

/**
 * Whether this build can talk to iNaturalist at all.
 *
 * The client id is not in this repository and there is no default. It arrives as a
 * gradle property at build time — `-PinatClientId=...`, or a line in a gitignored
 * `local.properties` — and a build without one produces an app whose iNaturalist button
 * is not there.
 *
 * Two reasons, and the second is the real one:
 *
 * - A PKCE client id is not a secret. It is in every copy of the APK by necessity, and
 *   treating it as a credential would be theatre.
 * - It is, however, **an identity**. It is registered to one person's iNaturalist
 *   account, and everything posted with it is attributed to that application on their
 *   records. A fork built from this source should be posting as itself, not as this one
 *   — and the way to make sure of that is for the source to have no id in it to inherit.
 *
 * ⚠ Registering the application needs the **APP_OWNER** role on iNaturalist, which is
 * granted by their staff after a written request, and which they expect to see some
 * identifications behind. It is a wait, not a form. Until then this is inert, which is
 * what [configured] is for: the button says why rather than failing when pressed.
 */
object INatConfig {

    val CLIENT_ID: String = BuildConfig.INAT_CLIENT_ID

    /**
     * The custom scheme the browser hands the code back on.
     *
     * A scheme rather than an https link, because catching an https redirect would mean
     * either a server this app does not have or an App Link verified against a domain
     * it does not own. iNaturalist accepts it — their Doorkeeper sets
     * `force_ssl_in_redirect_uri false` — and it must be entered on the application's
     * registration page exactly as it reads here.
     */
    const val REDIRECT_URI = "kinokocho://oauth"

    val configured: Boolean get() = CLIENT_ID.isNotBlank()
}
