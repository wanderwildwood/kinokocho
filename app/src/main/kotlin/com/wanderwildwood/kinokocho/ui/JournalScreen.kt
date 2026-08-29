package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.wanderwildwood.kinokocho.R
import com.wanderwildwood.kinokocho.data.FullObservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Everything recorded, newest first.
 *
 * The list is the app. A journal that opens on a form is a form; one that opens on
 * what you have already found is a journal, and the reason to carry it is that the
 * last three walks are in your hand.
 */
@Composable
fun JournalScreen(
    entries: List<FullObservation>,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onAbout: () -> Unit,
    onDelete: (Long) -> Unit,
    seasonRow: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Mushroom Journal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                // About lives behind an i, not in a settings list: it is the first thing
                // a stranger looks for and it is not a setting.
                Icon(
                    painter = painterResource(R.drawable.ic_about),
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp).clickable(onClick = onAbout),
                )
            }

            // What is about this month, before anything has been found. It is the one
            // thing a journal can say when it is still empty.
            seasonRow?.invoke()
            HorizontalDividerMMD()

            if (entries.isEmpty()) {
                Empty(Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
                    items(entries, key = { it.observation.id }) { entry ->
                        EntryRow(entry, onOpen = { onOpen(entry.observation.id) },
                            onDelete = { onDelete(entry.observation.id) })
                        HorizontalDividerMMD()
                    }
                }
            }

            ButtonMMD(
                onClick = onNew,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("Record a find") }
        }
    }
}

@Composable
private fun Empty(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.art_launcher_chanterelle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(72.dp),
        )
        Text(
            "Nothing recorded yet.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "A find is worth recording even when you never learn what it was. " +
                "Answer whatever you can see and leave the rest.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * One find.
 *
 * Deliberately shows what was recorded rather than what it might be. A row that said
 * a name would be the app guessing, and it does not guess — so the row carries the
 * date, the place if there is one, and how much of the sheet was filled in.
 */
@Composable
private fun EntryRow(entry: FullObservation, onOpen: () -> Unit, onDelete: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(4000)
            armed = false
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { if (armed) armed = false else onOpen() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (armed) "Delete this find — tap again" else dateOf(entry.observation.recordedAt),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            val counted = entry.characters.map { it.characterId }.distinct().size
            val place = entry.observation.placeNote.takeIf { it.isNotBlank() }
            Text(
                listOfNotNull(
                    place,
                    "$counted character${if (counted == 1) "" else "s"}",
                    entry.photos.size.takeIf { it > 0 }
                        ?.let { "$it photo${if (it == 1) "" else "s"}" },
                    // Spore print pending is the whole reason an entry stays open, so
                    // it is said on the row rather than found by opening it.
                    "spore print pending".takeIf {
                        entry.characters.none { c -> c.characterId == "spore_print" }
                    },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_remove),
            contentDescription = if (armed) "Delete this find — tap again" else "Delete this find",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(24.dp)
                .clickable { if (armed) onDelete() else armed = true },
        )
    }
}

private fun dateOf(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))
