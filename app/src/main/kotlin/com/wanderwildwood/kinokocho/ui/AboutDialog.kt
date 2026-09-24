package com.wanderwildwood.kinokocho.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import com.wanderwildwood.kinokocho.BuildConfig
import com.wanderwildwood.kinokocho.net.INatConfig
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.kinokocho.R

/**
 * What this is, and what it does with what you tell it.
 *
 * The privacy line stays here where it would be dropped from a Go board's About,
 * because a record of where someone forages is worth more to a stranger than a record
 * of their games, and nobody can safely assume which way a journal app went.
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
) {
    EInkDialog(onDismiss = onDismiss) {
        // This outgrew the panel once already: with the two backup rows on it the last
        // one landed in the system's gesture strip on a 480x800 screen and stopped being
        // tappable at all. It pages now, four rows to a swipe, rather than coasting.
        LazyColumnMMD(modifier = Modifier.heightIn(max = 460.dp)) {
            item {
                Line(stringResource(R.string.about_title, BuildConfig.VERSION_NAME), bold = true)
            }
            item {
                Spacer(14)
            }
            item {
                Line(
                    stringResource(R.string.about_what_it_is)
                )
            }
            item {
                Spacer(14)
            }
            item {
                Line(
                    stringResource(R.string.about_privacy)
                )
            }
            item {
                /*
                 * The sentence a stranger reads before deciding whether to trust this.
                 *
                 * It used to say "No network", which stopped being true the day publishing
                 * was built, and a stale reassurance is worse than none — it is the one line
                 * somebody would have relied on. The word "obscured" is the reason the
                 * second sentence exists: it sounds like the coordinates are not sent, and
                 * they are. Somebody publishing a foraging patch should read the true
                 * version here rather than find it out later.
                 *
                 * Conditional, because a build with no application id genuinely has no
                 * publishing button and telling its reader about one would be its own kind
                 * of untrue.
                 */
                if (INatConfig.configured) {
                    Spacer(14)
                    Line(
                        stringResource(R.string.about_inat)
                    )
                    Spacer(10)
                    Line(
                        stringResource(R.string.about_inat_obscured)
                    )
                }
            }
            item {
                Spacer(14)
            }
            item {
                Line(stringResource(R.string.about_licence))
            }
            item {
                Line(stringResource(R.string.about_schema_credit))
            }
            item {
                Line(stringResource(R.string.about_delta_credit))
            }
            item {
                Spacer(14)
            }
            item {
                /*
                 * A way to get the journal off the phone.
                 *
                 * The database says of itself that it holds notes that cannot be taken again
                 * — the mushroom is gone and the season is over — and for a long time there
                 * was no way to copy any of them anywhere. A phone is lost, dropped in a
                 * stream, or simply replaced, and a book that lives in exactly one place is
                 * a book with a date on it.
                 *
                 * Here rather than on the journal itself, because it is a thing done twice a
                 * year and the journal screen is for the finds.
                 */
                TextMMD(
                    stringResource(R.string.about_export),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onExport),
                )
            }
            item {
                Line(
                    stringResource(R.string.about_export_body)
                )
            }
            item {
                Spacer(10)
            }
            item {
                TextMMD(
                    stringResource(R.string.about_import),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onImport),
                )
            }
            item {
                Line(
                    stringResource(R.string.about_import_body)
                )
            }
            item {
                Spacer(14)
            }
            item {
                Line("wanderthe.dev")
            }
            item {
                Spacer(14)
            }
            item {
                Llama()
            }
            item {
                Spacer(20)
            }
            item {
                TextMMD(
                    stringResource(R.string.about_close),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss),
                )
            }
        }
    }
}

@Composable
private fun Line(text: String, bold: Boolean = false) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

@Composable
private fun Spacer(dp: Int) {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = dp.dp))
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to.
 *
 * Three words rather than an address: a verb and an object, so what happens when you press
 * them is not a surprise even though the page is not named. The drawing is his own, and it is
 * ink rather than an emoji, which is a colour glyph and reaches the panel as a pale smudge.
 *
 * The Kompakt may have nothing registered for a web address, so the intent is allowed to fail
 * quietly rather than take the dialog down with it.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Straight to the checkout. The Donate button on the site only leads
                // here anyway, so the page in between is a press the reader does not need.
                // The short square.link form, not the long checkout.square.site address it
                // redirects to -- the short one is what the site itself links to, so a
                // regenerated checkout follows it and a published app does not break.
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://square.link/u/AGu8oT10")),
                    )
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.about_no_browser), Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 4.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.llama),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
        Line(stringResource(R.string.about_feed_the_llamas))
    }
}
