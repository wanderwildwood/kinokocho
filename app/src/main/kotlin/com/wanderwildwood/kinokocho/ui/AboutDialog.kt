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
                Line("茸帳 Mushroom Journal ${BuildConfig.VERSION_NAME}", bold = true)
            }
            item {
                Spacer(14)
            }
            item {
                Line(
                    "A field journal. It records what you saw and narrows what it could " +
                        "be — often far enough that someone more experienced can say. It " +
                        "does not decide, it has no opinion about eating anything, and on " +
                        "a screen with no colour it could not be the one to decide anyway."
                )
            }
            item {
                Spacer(14)
            }
            item {
                Line(
                    "Nothing leaves this phone unless you send it. The only permission " +
                        "asked for is a rough position, only when you press the button for " +
                        "it, and only ever rough — never a precise fix."
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
                        "Publishing a find to iNaturalist is the one thing that reaches the " +
                            "network. One find, when you press a button that says it is " +
                            "public. Sign-in happens in your browser, so this app never sees " +
                            "your password."
                    )
                    Spacer(10)
                    Line(
                        "Obscured is not the same as not sent: iNaturalist is told where the " +
                            "mushroom was and shows the public a point some twenty kilometres " +
                            "off. The place you typed is kept there too, and not shown."
                    )
                }
            }
            item {
                Spacer(14)
            }
            item {
                Line("GNU General Public License v3")
            }
            item {
                Line("Character schema and drawings — original, this project.")
            }
            item {
                Line("Question order after Watson & Dallwitz, DELTA, 1974.")
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
                    "Keep a copy of everything",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onExport),
                )
            }
            item {
                Line(
                    "A zip of every find and every photograph, handed to whatever you keep " +
                        "things in. Plain JSON inside, readable without this app."
                )
            }
            item {
                Spacer(10)
            }
            item {
                TextMMD(
                    "Read a copy back in",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onImport),
                )
            }
            item {
                Line(
                    "Adds whatever is not already here. Nothing is deleted and nothing is " +
                        "overwritten, so an old copy read onto a full journal keeps both."
                )
            }
            item {
                Spacer(14)
            }
            item {
                Line("github.com/wanderwildwood/kinokocho")
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
                    "Close",
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
                    Toast.makeText(context, "There is no browser on this phone to open that with.", Toast.LENGTH_SHORT).show()
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
        Line("Feed the llamas")
    }
}
