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
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.wanderwildwood.kinokocho.R
import com.wanderwildwood.kinokocho.data.FullObservation
import com.wanderwildwood.kinokocho.data.MeasurementRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import com.mudita.mmd.components.lazy.LazyColumnMMD

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

            /*
             * A way to find one again.
             *
             * A journal is kept for years, and the whole point of keeping it is going
             * back to something. Newest-first with no way to look is fine at a dozen
             * finds and useless at three hundred — "that white one from the spring, below
             * the spring" is a thing a person remembers in exactly these terms: a name, a
             * place, a month.
             *
             * It appears only once there is enough to lose something in. A search box
             * over four finds is furniture.
             */
            var query by remember { mutableStateOf("") }
            if (entries.size > ENOUGH_TO_LOSE_ONE_IN) {
                TextFieldMMD(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("A name, a place, a month") },
                    singleLine = true,
                )
            }
            val shown = remember(entries, query) { entries.matching(query) }

            when {
                entries.isEmpty() -> Empty(Modifier.weight(1f))
                shown.isEmpty() -> Column(
                    Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Nothing here matches that.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                else -> LazyColumnMMD(Modifier.weight(1f).padding(top = 8.dp)) {
                    items(shown, key = { it.observation.id }) { entry ->
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
            // The name leads once there is one. A journal you can read back is a list
            // of what things were, and the date is what it falls back to.
            val named = entry.observation.identifiedAs.takeIf { it.isNotBlank() }
            Text(
                when {
                    armed -> "Delete this find — tap again"
                    named != null -> named
                    else -> dateOf(entry.observation.recordedAt)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            // Not counting the ones the reader looked at and could not say. Those are
            // rows too now, and counting them would tell somebody they had recorded
            // twelve characters when four of them are "I could not tell".
            val counted = entry.characters
                .filterNot { it.valueId == MeasurementRow.NOT_TESTED }
                .map { it.characterId }.distinct().size
            val place = entry.observation.placeNote.takeIf { it.isNotBlank() }
            Text(
                listOfNotNull(
                    named?.let { dateOf(entry.observation.recordedAt) },
                    place,
                    "$counted character${if (counted == 1) "" else "s"}",
                    entry.photos.size.takeIf { it > 0 }
                        ?.let { "$it photo${if (it == 1) "" else "s"}" },
                    // Spore print pending is the whole reason an entry stays open, so
                    // it is said on the row rather than found by opening it.
                    // Gone once the print is recorded — and also once the reader has
                    // said they tried and it never dropped, which is an answer and not
                    // a thing still to do.
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

/**
 * The finds a search matches, or all of them when nothing is being searched for.
 *
 * Matches on the four things a person actually remembers about a find: what it turned out
 * to be, where it was, what they wrote about it, and roughly when. The date is matched as
 * the words it is displayed as, so "September" and "2026" both work and neither needs a
 * date picker to express.
 */
fun List<FullObservation>.matching(query: String): List<FullObservation> {
    val q = query.trim()
    if (q.isBlank()) return this
    return filter { entry ->
        val o = entry.observation
        listOf(o.identifiedAs, o.placeNote, o.note, dateOf(o.recordedAt))
            .any { it.contains(q, ignoreCase = true) }
    }
}

/**
 * How many finds it takes before a journal is worth searching.
 *
 * Below this the list is the search. A box over four finds is furniture.
 */
private const val ENOUGH_TO_LOSE_ONE_IN = 6

private fun dateOf(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))
