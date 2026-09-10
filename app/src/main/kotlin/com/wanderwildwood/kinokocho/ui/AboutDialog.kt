package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import com.wanderwildwood.kinokocho.BuildConfig
import com.wanderwildwood.kinokocho.net.INatConfig

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
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
                // Scrolls, because it outgrew the screen. With the two backup rows on it
                // the last one sat in the system's gesture strip along the bottom edge on
                // a 480x800 panel — which is the Kompakt — and could not be tapped at
                // all: the swipe was taken as a navigation gesture and the row never saw
                // it. A dialog that cannot be scrolled is a dialog that must never grow.
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Line("茸帳 Mushroom Journal ${BuildConfig.VERSION_NAME}", bold = true)

            Spacer(14)
            Line(
                "A field journal. It records what you saw and narrows what it could " +
                    "be — often far enough that someone more experienced can say. It " +
                    "does not decide, it has no opinion about eating anything, and on " +
                    "a screen with no colour it could not be the one to decide anyway."
            )

            Spacer(14)
            Line(
                "Nothing leaves this phone unless you send it. The only permission " +
                    "asked for is a rough position, only when you press the button for " +
                    "it, and only ever rough — never a precise fix."
            )

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

            Spacer(14)
            Line("GNU General Public License v3")
            Line("Character schema and drawings — original, this project.")
            Line("Question order after Watson & Dallwitz, DELTA, 1974.")

            Spacer(14)
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
            Line(
                "A zip of every find and every photograph, handed to whatever you keep " +
                    "things in. Plain JSON inside, readable without this app."
            )

            Spacer(10)
            TextMMD(
                "Read a copy back in",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onImport),
            )
            Line(
                "Adds whatever is not already here. Nothing is deleted and nothing is " +
                    "overwritten, so an old copy read onto a full journal keeps both."
            )

            Spacer(14)
            Line("github.com/wanderwildwood/kinokocho")

            Spacer(20)
            TextMMD(
                "Close",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss),
            )
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
