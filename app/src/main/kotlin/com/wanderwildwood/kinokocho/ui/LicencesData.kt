package com.wanderwildwood.kinokocho.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.kinokocho.R

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/kinokocho"

/**
 * Everything the APK contains that is not this project's own code is listed by the build
 * (AboutLibraries, one row per artifact, release libraries only); the data and drawings it ships
 * are added in config/libraries.
 *
 * Each distinct licence text once: family id and the raw resource. */
val LICENCE_RAW: List<Pair<String, Int>> = listOf(
    "gpl" to R.raw.gpl_3_0,
    "apache" to R.raw.apache_2_0,
    "ofl" to R.raw.ofl_1_1,
    "cc-by" to R.raw.cc_by_4_0,
)

@Composable
fun rememberLicencesData(): LicencesData = LicencesData(
    appName = "Mushroom Journal",
    sourceUrl = SOURCE_URL,
    licenceLine = stringResource(R.string.licences_licence),
    // One row per artifact in the APK, listed by the build (config/libraries adds what Gradle cannot see).
    components = rememberGeneratedComponents(R.raw.aboutlibraries),
    texts = LICENCE_RAW.map { (id, raw) ->
        LicenceText(id, stringResource(when (id) {
            "gpl" -> R.string.licences_full_gpl
            "apache" -> R.string.licences_full_apache
            "ofl" -> R.string.licences_full_ofl
            "cc-by" -> R.string.licences_full_ccby
            else -> R.string.licences_full_head
        }), raw)
    },
)

@Composable
fun rememberLicencesLabels(): LicencesLabels {
    val none = stringResource(R.string.about_no_browser_address, "@@")
    val noText = stringResource(R.string.licences_no_text, "@@")
    return LicencesLabels(
        title = stringResource(R.string.licences_title),
        back = stringResource(R.string.licences_back),
        sourceHeading = stringResource(R.string.licences_source_head),
        noText = { licence -> noText.replace("@@", licence) },
        noBrowser = { address -> none.replace("@@", address) },
    )
}
