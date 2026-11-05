package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.kinokocho.BuildConfig
import com.wanderwildwood.kinokocho.R
import com.wanderwildwood.kinokocho.net.INatConfig

/**
 * This app's About: only the words and the address. The dialog, its drawers, its one Close button,
 * the Licenses drawer, the links and the llama are the shared HouseAbout.kt, the same in every app.
 *
 * The privacy lines stay because a record of where someone forages is worth more to a stranger
 * than a record of their games. The iNaturalist lines are there only in a build that has a
 * publishing button: telling the reader of a build without one about it would be untrue. Backup
 * (copy of everything, reading one back) is no longer here: it lives in the journal's own Backup
 * dialog.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val data = AboutData(
        title = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
        sections = buildList {
            add(AboutSection(null, stringResource(R.string.about_what_it_is)))
            add(AboutSection(null, stringResource(R.string.coverage_statement)))
            add(AboutSection(null, stringResource(R.string.coverage_collaborate)))
            add(AboutSection(stringResource(R.string.about_safety_heading), stringResource(R.string.safety_disclaimer)))
            add(AboutSection(stringResource(R.string.about_privacy_heading), stringResource(R.string.about_privacy)))
            if (INatConfig.configured) {
                add(AboutSection(null, stringResource(R.string.about_inat)))
                add(AboutSection(null, stringResource(R.string.about_inat_obscured)))
            }
            add(AboutSection(null, stringResource(R.string.about_privacy_close)))
            add(AboutSection(stringResource(R.string.privacy_who_title), stringResource(R.string.privacy_who)))
            add(AboutSection(stringResource(R.string.privacy_contact_title), stringResource(R.string.privacy_contact)))
            add(AboutSection(stringResource(R.string.privacy_rights_title), stringResource(R.string.privacy_rights)))
            add(AboutSection(stringResource(R.string.about_terms_heading), stringResource(R.string.about_terms_licence)))
            add(AboutSection(null, stringResource(R.string.about_terms)))
            add(AboutSection(null, stringResource(R.string.about_schema_credit)))
            add(AboutSection(null, stringResource(R.string.about_delta_credit)))
        },
        sourceUrl = SOURCE_URL,
    )
    val labels = AboutLabels(
        close = stringResource(R.string.about_close),
        licences = stringResource(R.string.licences_title),
        feedTheLlamas = stringResource(R.string.about_feed_the_llamas),
        noBrowser = { address -> context.getString(R.string.about_no_browser_address, address) },
        writeTo = { address -> context.getString(R.string.about_write_to, address) },
        noMailApp = { address -> context.getString(R.string.about_no_mail_app, address) },
        openDrawer = { heading -> context.getString(R.string.about_open_drawer, heading) },
        closeDrawer = { heading -> context.getString(R.string.about_close_drawer, heading) },
        intro = stringResource(R.string.about_intro_head),
        contact = stringResource(R.string.about_contact_head),
        links = stringResource(R.string.about_links_head),
    )
    HouseAboutDialog(data, labels, R.drawable.llama, onDismiss)
}
