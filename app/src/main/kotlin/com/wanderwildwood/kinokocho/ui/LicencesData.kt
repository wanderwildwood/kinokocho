package com.wanderwildwood.kinokocho.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.kinokocho.R

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/kinokocho"

/**
 * Everything the APK contains that is not this project's own code, from its dependency tree
 * (androidx and Compose libraries all carry their licence in META-INF of the APK) and from the
 * data and drawings it ships. Libraries come in families: one row per family.
 */
val LICENCE_COMPONENTS: List<LicenceComponent> = listOf(
    LicenceComponent("Kotlin standard library 2.2", "JetBrains", "Apache-2.0", "https://kotlinlang.org"),
    LicenceComponent("kotlinx.coroutines 1.10", "JetBrains", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    LicenceComponent("AndroidX Core, Activity, Lifecycle, SavedState, Startup, Tracing, Emoji2, ProfileInstaller", "Google", "Apache-2.0", "https://developer.android.com/jetpack/androidx"),
    LicenceComponent("AndroidX Room and SQLite", "Google", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
    LicenceComponent("Jetpack Compose (runtime, UI, foundation, animation, Material 3)", "Google", "Apache-2.0", "https://developer.android.com/jetpack/compose"),
    LicenceComponent("MMD 1.0.2, Mudita's E Ink component library", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
    LicenceComponent("Lato typeface, inside MMD", "Łukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
    LicenceComponent("Region pack: southern Appalachia (characters, hazards, seasons)", "wander wildwood", "CC0-1.0", "https://creativecommons.org/publicdomain/zero/1.0/"),
    LicenceComponent("Whole-mushroom plates, traced from public-domain plates (Peck, Sowerby, Bresadola, Bulliard, Cooke and others)", "wander wildwood", "Public domain", "https://github.com/wanderwildwood/kinokocho/blob/master/tools/plate-sources.json"),
    LicenceComponent("Season counts from GBIF occurrence records (the publishers of each dataset keep the credit for their records)", "GBIF.org", "CC0-1.0 and CC-BY-4.0", "https://www.gbif.org"),
)

/** Each distinct licence text once: family id and the raw resource. */
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
    components = LICENCE_COMPONENTS,
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
