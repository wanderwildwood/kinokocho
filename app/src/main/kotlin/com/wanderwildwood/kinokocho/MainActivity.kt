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
import com.wanderwildwood.kinokocho.ui.CandidateScreen
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
     * The journal is the front door and "Record a find" opens the key.
     *
     * Tried the other way round for a while — straight into the first question, journal
     * a row away — and that was the call. The journal is what the app is *for*; the
     * key is the thing you do to fill it.
     *
     * Answers are written down after every tap regardless — the phone is outdoors and
     * the battery can die mid-question — but nothing is *kept* until you say so at the
     * end. Crash-safety and "save in the journal?" are not in conflict; the entry simply
     * exists before it is claimed.
     */
    var asking by remember { mutableStateOf(false) }
    var candidate by remember { mutableStateOf<String?>(null) }
    var photographing by remember { mutableStateOf(false) }
    var season by remember { mutableStateOf(false) }

    val open = draft
    val shown = candidate?.let { vm.pack.taxon(it) }

    // Back closes what is open rather than leaving the app, so a mis-tap on a find
    // costs nothing. Nothing here is destructive, so nothing here asks.
    BackHandler(enabled = draft != null || about || photographing || season || candidate != null) {
        when {
            season -> season = false
            photographing -> photographing = false
            about -> about = false
            candidate != null -> candidate = null
            asking -> asking = false
            // Reading a saved find: back goes to the journal it came from.
            reading -> { reading = false; vm.close() }
            // Mid-key: back asks whether to keep it rather than losing it silently.
            open != null -> asking = true
            else -> Unit
        }
    }

    // `entries` is already only what was kept — the DAO filters on it — so the find
    // being keyed out right now never appears here however the app is left.
    val kept = entries

    when {
        shown != null && open != null -> CandidateScreen(
            vm = vm,
            taxon = shown,
            answers = open.answers,
            onClose = { candidate = null },
        )

        open == null -> JournalScreen(
            entries = kept,
            onOpen = { id -> reading = true; vm.open(id) },
            onNew = { reading = false; vm.resumeOrStart() },
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
            onCandidate = { candidate = it },
        )

        open != null -> NewEntryScreen(
            vm = vm,
            draft = open,
            onAddPhoto = { photographing = true },
            onDone = { asking = true },
            onJournal = { vm.close() },
            journalCount = kept.size,
            onCandidate = { candidate = it },
        )

        else -> Unit
    }

    if (asking && open != null) {
        SaveDialog(
            answered = open.answers.answeredCount,
            photos = open.photos.size,
            onSave = { asking = false; vm.keep(); reading = true },
            onDiscard = {
                // The photographs stay on the phone: the owner's call. Only the entry goes,
                // so a mis-tap costs the record and not the pictures.
                asking = false
                open.observationId?.let(vm::delete)
                vm.close()
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
