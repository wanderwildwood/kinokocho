package com.wanderwildwood.kinokocho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.kinokocho.ui.AboutDialog
import com.wanderwildwood.kinokocho.ui.EntryScreen
import com.wanderwildwood.kinokocho.ui.JournalScreen
import com.wanderwildwood.kinokocho.ui.PhotoSheet
import com.wanderwildwood.kinokocho.ui.SaveDialog
import com.wanderwildwood.kinokocho.ui.SeasonRow
import com.wanderwildwood.kinokocho.ui.SeasonScreen
import java.util.Calendar
import com.wanderwildwood.kinokocho.ui.NewEntryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Journal()
                }
            }
        }
    }
}

/**
 * The whole app is two places: the list of finds, and one find.
 *
 * No navigation library. There are two screens and a dialog, and the draft in the
 * view model is what decides which one you are looking at — a find being written and
 * a find being read back are the same screen, because amending an entry after the
 * walk is the ordinary case rather than the exception. The spore print is not known
 * until the morning.
 */
@Composable
private fun Journal(vm: JournalViewModel = viewModel()) {
    val entries by vm.entries.collectAsState()
    val draft by vm.draft.collectAsState()
    var about by remember { mutableStateOf(false) }

    // Opening a find reads it back; starting one asks questions. The same draft serves
    // both, because an entry is amended far more often than it is finished — the spore
    // print is not known until the morning.
    var reading by remember { mutableStateOf(false) }

    /*
     * The key is the front door, and the journal is a place you go.
     *
     * This app is an e-ink answer to Shroomify with a journal added, which is a
     * different thing from a journal that happens to contain a key: you open it because
     * you are holding a mushroom and want to know what it could be, not to read back
     * last week. So it opens on the first question, and the journal is a row away.
     *
     * Answers are still written down after every tap — the phone is outdoors and the
     * battery can die mid-question — but nothing is *kept* until you say so at the end.
     * Crash-safety and "save in the journal?" are not in conflict; the entry simply
     * exists before it is claimed.
     */
    var showJournal by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (draft == null && !showJournal) vm.startNewEntry() }
    var photographing by remember { mutableStateOf(false) }
    var season by remember { mutableStateOf(false) }

    // Back closes what is open rather than leaving the app, so a mis-tap on a find
    // costs nothing. Nothing here is destructive, so nothing here asks.
    BackHandler(enabled = true) {
        when {
            season -> season = false
            photographing -> photographing = false
            about -> about = false
            asking -> asking = false
            showJournal -> showJournal = false
            // Reading a saved find: back returns to the key rather than to nothing.
            reading -> { reading = false; vm.close(); vm.startNewEntry() }
            else -> asking = true
        }
    }

    val open = draft

    // Everything the journal should show: what has been kept. The find being keyed out
    // right now is written down after every tap so a dead battery cannot lose it, which
    // means it is already a row in the database — but it is not a journal entry until
    // the reader says so, and counting it would make the journal claim one more find
    // than the reader has agreed to.
    val kept = entries.filter { it.observation.id != open?.observationId }

    when {
        showJournal -> JournalScreen(
            entries = kept,
            onOpen = { id -> showJournal = false; reading = true; vm.open(id) },
            onNew = { showJournal = false; reading = false; vm.close(); vm.startNewEntry() },
            onAbout = { about = true },
            onDelete = vm::delete,
            seasonRow = {
                val m = thisMonth()
                SeasonRow(
                    month = m,
                    count = vm.pack.taxa.count { it.seasonMonths.isEmpty() || m in it.seasonMonths },
                    onOpen = { season = true },
                )
            },
        )

        season -> SeasonScreen(
            taxa = vm.pack.taxa,
            month = thisMonth(),
            onClose = { season = false },
        )

        reading && open != null -> EntryScreen(
            vm = vm,
            draft = open,
            onContinue = { reading = false },
            onAddPhoto = { photographing = true },
            onClose = { reading = false; vm.close() },
        )

        open != null -> NewEntryScreen(
            vm = vm,
            draft = open,
            onAddPhoto = { photographing = true },
            onDone = { asking = true },
            onJournal = { showJournal = true },
            journalCount = kept.size,
        )

        else -> Unit
    }

    if (asking && open != null) {
        SaveDialog(
            answered = open.answers.answeredCount,
            photos = open.photos.size,
            onSave = { asking = false; reading = true },
            onDiscard = {
                // The photographs stay on the phone: the owner's call. Only the entry goes,
                // so a mis-tap costs the record and not the pictures.
                asking = false
                open.observationId?.let(vm::delete)
                vm.close()
                vm.startNewEntry()
            },
            onBack = { asking = false },
        )
    }

    if (about) AboutDialog(onDismiss = { about = false })

    val forPhotos = draft
    if (photographing && forPhotos != null) {
        Dialog(onDismissRequest = { photographing = false }) {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)),
            ) {
                PhotoSheet(
                    filled = forPhotos.photos.map { it.slot }.toSet(),
                    onCaptured = vm::addPhoto,
                    onClose = { photographing = false },
                )
            }
        }
    }
}

private fun thisMonth(): Int =
    Calendar.getInstance().get(Calendar.MONTH) + 1
