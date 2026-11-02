package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.kinokocho.R

/**
 * A way to get the journal off the phone, and back.
 *
 * The database says of itself that it holds notes that cannot be taken again: the mushroom is
 * gone and the season is over. A phone is lost, dropped in a stream or replaced, and a book that
 * lives in exactly one place is a book with a date on it. It sits on the journal, one row under
 * the title, rather than in About: About is only the standard About.
 */
@Composable
fun BackupDialog(onDismiss: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(
            stringResource(R.string.backup_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        BackupAction(R.string.backup_export, R.string.backup_export_body, onExport)
        BackupAction(R.string.backup_import, R.string.backup_import_body, onImport)
        Spacer(Modifier.padding(top = 14.dp))
        HouseButton(label = stringResource(R.string.about_close), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BackupAction(title: Int, body: Int, onClick: () -> Unit) {
    TextMMD(
        stringResource(title),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(top = 14.dp),
    )
    TextMMD(stringResource(body), style = MaterialTheme.typography.bodySmall)
}

/** The row on the journal that opens the backup dialog. */
@Composable
fun BackupRow(onOpen: () -> Unit) {
    TextMMD(
        stringResource(R.string.backup_row),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
