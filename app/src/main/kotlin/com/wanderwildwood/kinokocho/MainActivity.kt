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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.kinokocho.ui.AboutDialog
import com.wanderwildwood.kinokocho.ui.JournalScreen
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

    // Back closes what is open rather than leaving the app, so a mis-tap on a find
    // costs nothing. Nothing here is destructive, so nothing here asks.
    BackHandler(enabled = draft != null || about) {
        if (about) about = false else vm.close()
    }

    val open = draft
    if (open == null) {
        JournalScreen(
            entries = entries,
            onOpen = vm::open,
            onNew = vm::startNewEntry,
            onAbout = { about = true },
            onDelete = vm::delete,
        )
    } else {
        NewEntryScreen(
            vm = vm,
            draft = open,
            onAddPhoto = { /* camera lands in the commit that first needs it */ },
            onDone = vm::close,
        )
    }

    if (about) AboutDialog(onDismiss = { about = false })
}
