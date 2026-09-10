package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD

/**
 * Asked at the end of keying something out: is this one to keep?
 *
 * Most of what a person keys out is a mushroom they were curious about for a minute.
 * Keeping every one of those turns a journal into a list of things you have forgotten,
 * and the finds worth going back to get buried in it. So the journal holds what was
 * claimed rather than everything that was looked at.
 *
 * The answers were being written down all along — the phone is outdoors and can die
 * mid-question — so this asks about keeping, not about saving. Nothing is lost by
 * saying no except a record nobody wanted.
 */
@Composable
fun SaveDialog(
    answered: Int,
    photos: Int,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
) {
    Dialog(onDismissRequest = onBack) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
                .padding(20.dp),
        ) {
            TextMMD(
                "Save in the journal?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextMMD(
                buildString {
                    append("$answered character")
                    if (answered != 1) append("s")
                    if (photos > 0) {
                        append(" and $photos photograph")
                        if (photos != 1) append("s")
                    }
                    append(".")
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
            TextMMD(
                "Worth keeping if you want to add a spore print in the morning, or ask " +
                    "someone about it later.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )

            ButtonMMD(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                TextMMD("Save it")
            }
            OutlinedButtonMMD(
                onClick = onDiscard,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) { TextMMD("No, throw it away") }
            OutlinedButtonMMD(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) { TextMMD("Keep answering") }
        }
    }
}
