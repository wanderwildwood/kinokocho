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
    var photographing by remember { mutableStateOf(false) }

    // Back closes what is open rather than leaving the app, so a mis-tap on a find
    // costs nothing. Nothing here is destructive, so nothing here asks.
    BackHandler(enabled = draft != null || about || photographing) {
        when {
            photographing -> photographing = false
            about -> about = false
            // Answering, on a find opened for reading: back returns to the reading
            // view rather than throwing the whole entry away.
            draft != null && !reading && draft?.observationId != null -> reading = true
            else -> vm.close()
        }
    }

    val open = draft
    when {
        open == null -> JournalScreen(
            entries = entries,
            onOpen = { id -> reading = true; vm.open(id) },
            onNew = { reading = false; vm.startNewEntry() },
            onAbout = { about = true },
            onDelete = vm::delete,
        )

        reading -> EntryScreen(
            vm = vm,
            draft = open,
            onContinue = { reading = false },
            onAddPhoto = { photographing = true },
            onClose = { reading = false; vm.close() },
        )

        else -> NewEntryScreen(
            vm = vm,
            draft = open,
            onAddPhoto = { photographing = true },
            onDone = { reading = true },
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
