package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.wanderwildwood.kinokocho.R

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
    EInkDialog(onDismiss = onBack) {
        TextMMD(
            stringResource(R.string.save_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        TextMMD(
            if (photos > 0) {
                stringResource(
                    R.string.save_counts_with_photographs,
                    pluralStringResource(R.plurals.save_characters, answered, answered),
                    pluralStringResource(R.plurals.save_photographs, photos, photos),
                )
            } else {
                stringResource(
                    R.string.save_counts,
                    pluralStringResource(R.plurals.save_characters, answered, answered),
                )
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        TextMMD(
            stringResource(R.string.save_body),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )

        ButtonMMD(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.save_confirm))
        }
        OutlinedButtonMMD(
            onClick = onDiscard,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) { TextMMD(stringResource(R.string.save_discard)) }
        OutlinedButtonMMD(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) { TextMMD(stringResource(R.string.save_keep_answering)) }
    }
}
