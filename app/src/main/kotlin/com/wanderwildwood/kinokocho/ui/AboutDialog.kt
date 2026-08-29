package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import com.wanderwildwood.kinokocho.BuildConfig

/**
 * What this is, and what it does with what you tell it.
 *
 * The privacy line stays here where it would be dropped from a Go board's About,
 * because a record of where someone forages is worth more to a stranger than a record
 * of their games, and nobody can safely assume which way a journal app went.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
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
            Line("No permissions. No network. What you record stays on this phone.")

            Spacer(14)
            Line("GNU General Public License v3")
            Line("Character schema and drawings — original, this project.")
            Line("Question order after Watson & Dallwitz, DELTA, 1974.")

            Spacer(14)
            Line("github.com/wanderwildwood/kinokocho")

            Spacer(20)
            Text(
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
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

@Composable
private fun Spacer(dp: Int) {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = dp.dp))
}
