package com.wanderwildwood.kinokocho

import com.wanderwildwood.kinokocho.ui.DarkSelection
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.wanderwildwood.kinokocho.key.KeyData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.wanderwildwood.kinokocho.key.KeyEngine
import androidx.compose.ui.platform.LocalContext
import com.wanderwildwood.kinokocho.ui.AboutDialog
import com.wanderwildwood.kinokocho.ui.EInkDialog
import com.wanderwildwood.kinokocho.ui.JournalExport
import com.wanderwildwood.kinokocho.ui.JournalImport
import com.wanderwildwood.kinokocho.ui.CandidateScreen
import com.wanderwildwood.kinokocho.ui.EntryScreen
import com.wanderwildwood.kinokocho.ui.JournalScreen
import com.wanderwildwood.kinokocho.ui.BackupDialog
import com.wanderwildwood.kinokocho.ui.PhotoSheet
import com.wanderwildwood.kinokocho.ui.FootNotice
import com.wanderwildwood.kinokocho.ui.SaveDialog
import com.wanderwildwood.kinokocho.ui.SeasonRow
import com.wanderwildwood.kinokocho.ui.SeasonScreen
import com.wanderwildwood.kinokocho.ui.houseTypography
import com.wanderwildwood.kinokocho.ui.monochrome
import java.util.Calendar
import com.wanderwildwood.kinokocho.ui.NewEntryScreen
import com.wanderwildwood.kinokocho.ui.ArmedScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD(colorScheme = monochrome, typography = houseTypography) { DarkSelection {
                ArmedScope {
                    // A blinking caret repaints the e-ink panel twice a second for nothing.
                    CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                            // The key's data is read off the main thread first, so the
                            // blank page is drawn at once and the journal follows it.
                            val ready by produceState(false) {
                                withContext(Dispatchers.Default) { KeyData.get(applicationContext.assets) }
                                value = true
                            }
                            if (ready) Journal()
                        }
                    }
                }
            } }
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
    var backup by remember { mutableStateOf(false) }

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
    BackHandler(enabled = draft != null || about || backup || photographing || season || candidate != null) {
        when {
            backup -> backup = false
            season -> season = false
            photographing -> photographing = false
            about -> about = false
            candidate != null -> candidate = null
            asking -> asking = false
            // Reading a saved find: back goes to the journal it came from.
            reading -> { reading = false; vm.close() }
            // Mid-key: back asks whether to keep it rather than losing it silently. A find
            // already in the journal is saved as it changes; back returns to it.
            open != null -> if (open.kept) reading = true else asking = true
            else -> Unit
        }
    }

    // `entries` is already only what was kept — the DAO filters on it — so the find
    // being keyed out right now never appears here however the app is left.
    val trashed by vm.trashed.collectAsState()
    val kept = if (trashed == null) entries else entries.filter { it.observation.id != trashed }
    // A find thrown away is never in the journal, but its Undo is shown there all the same.
    val thrownAway by vm.thrownAway.collectAsState()

    // Leaving the app is moving on too: a find deleted from the journal goes when the app
    // is put away, not only when another is opened, or Back or a swipe from Recents
    // would bring it back, photographs and all.
    val lifecycleOwner = LocalView.current.findViewTreeLifecycleOwner()
    DisposableEffect(lifecycleOwner, vm) {
        val watcher = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { vm.commitTrash(); vm.commitDiscard() }
        }
        lifecycleOwner?.lifecycle?.addObserver(watcher)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(watcher) }
    }

    when {
        // Reachable from the month page as well as from a shortlist, which is why the
        // answers are optional: read about a mushroom before you have found one, and
        // there is nothing to lay the description against yet.
        shown != null -> CandidateScreen(
            vm = vm,
            taxon = shown,
            answers = open?.answers ?: KeyEngine.Answers(),
            onClose = { candidate = null },
        )

        // Before the journal branch, not after it. `open == null` is true on the
        // journal, so a season branch below it could never be reached: tapping the row
        // set the flag, the journal matched first, and the page was dead code with a
        // link to it.
        season -> SeasonScreen(
            taxa = vm.pack.taxa,
            month = thisMonth(),
            onClose = { season = false },
            onOpen = { candidate = it },
        )

        open == null -> JournalScreen(
            entries = kept,
            onOpen = { id -> reading = true; vm.open(id) },
            onNew = { reading = false; vm.resumeOrStart() },
            onAbout = { about = true },
            onBackup = { backup = true },
            onDelete = vm::trash,
            deleted = trashed != null || thrownAway != null,
            deletedText = if (thrownAway != null) R.string.journal_thrown_away else R.string.journal_deleted,
            onUndo = { if (thrownAway != null) vm.undoDiscard() else vm.undoTrash() },
            seasonRow = {
                val m = thisMonth()
                SeasonRow(
                    month = m,
                    count = vm.pack.taxa.count { it.seasonMonths.isEmpty() || m in it.seasonMonths },
                    onOpen = { season = true },
                )
            },
        )

        // Everything from here down has a draft. The `open == null` branch above took
        // the journal, so the compiler already knows it — saying `open != null` again
        // reads like a guard and compiles to nothing.
        reading -> EntryScreen(
            vm = vm,
            draft = open,
            onContinue = { reading = false },
            onAddPhoto = { photographing = true },
            onClose = { reading = false; vm.close() },
            onCandidate = { candidate = it },
        )

        else -> NewEntryScreen(
            vm = vm,
            draft = open,
            onAddPhoto = { photographing = true },
            onDone = { if (open.kept) reading = true else asking = true },
            onJournal = { vm.close() },
            journalCount = kept.size,
            onCandidate = { candidate = it },
        )
    }

    if (asking && open != null) {
        SaveDialog(
            answered = open.answers.answeredCount,
            photos = open.photos.size,
            onSave = { asking = false; vm.keep(); reading = true },
            onDiscard = {
                // The find and its photographs go, with the journal's Undo for a mis-tap: a
                // picture kept without its find could not be reached from anywhere.
                asking = false
                // Never a find already in the journal: that is deleted from the journal, armed
                // and with Undo.
                if (!open.kept) vm.discard() else vm.close()
            },
            onBack = { asking = false },
        )
    }

    // What the last import did, said once and dismissed. A count is the whole report a
    // person needs from a restore: how many came in, and how many were already here.
    var imported by remember { mutableStateOf<JournalImport.Result?>(null) }
    val pickBackup = rememberLauncherForActivityResult(
        FilesOpenDocument(writable = false)
    ) { uri ->
        if (uri != null) vm.importJournal(uri) { imported = it }
    }

    // Said once, on the first start, before anything else: what the guide covers and what
    // it does not. Dismissing it is remembered; About carries the same words for good.
    val context0 = LocalContext.current
    val coverageSeen = remember {
        context0.getSharedPreferences("first_start", android.content.Context.MODE_PRIVATE)
    }
    var coverage by remember { mutableStateOf(!coverageSeen.getBoolean("coverage_seen", false)) }
    if (coverage) {
        EInkDialog(onDismiss = {}) {
            TextMMD(stringResource(R.string.coverage_statement))
            TextMMD(
                stringResource(R.string.safety_disclaimer),
                modifier = Modifier.padding(top = 10.dp),
            )
            ButtonMMD(
                onClick = {
                    coverageSeen.edit().putBoolean("coverage_seen", true).apply()
                    coverage = false
                },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) { TextMMD(stringResource(R.string.coverage_ok)) }
        }
    }

    // Where the copy goes is the person's choice, made in a save screen (Cabinet's, or the
    // system's), so it can be a folder on the phone or a card as well as an app. Nothing
    // here is shared.
    var saved by remember { mutableStateOf<Boolean?>(null) }
    val saveCopy = rememberLauncherForActivityResult(
        FilesCreateDocument("application/zip")
    ) { uri ->
        if (uri != null) vm.exportJournalTo(uri) { saved = it }
    }
    saved?.let { ok ->
        EInkDialog(onDismiss = { saved = null }) {
            TextMMD(
                stringResource(if (ok) R.string.export_saved else R.string.export_failed),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButtonMMD(
                onClick = { saved = null },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { TextMMD(stringResource(R.string.import_close)) }
        }
    }

    if (about) AboutDialog(onDismiss = { about = false })

    if (backup) {
        BackupDialog(
            onDismiss = { backup = false },
            onExport = {
                backup = false
                saveCopy.launch(JournalExport.fileName())
            },
            onImport = {
                backup = false
                // Anything, because a backup handed back by a cloud drive often arrives
                // as octet-stream rather than as a zip, and a filter that hides the file
                // somebody is looking for is worse than one that shows too much.
                pickBackup.launch(arrayOf("*/*"))
            },
        )
    }

    imported?.let { result ->
        EInkDialog(onDismiss = { imported = null }) {
            TextMMD(
                result.failed ?: when {
                    // Said alone when it is all that happened, so it is not lost behind
                    // "Nothing in that file to read".
                    result.added == 0 && result.heldBack > 0 ->
                        pluralStringResource(R.plurals.import_held_back, result.heldBack, result.heldBack).trim()
                    result.added == 0 && result.alreadyHere > 0 ->
                        stringResource(R.string.import_all_already_here)
                    result.added == 0 -> stringResource(R.string.import_nothing_to_read)
                    else -> stringResource(
                        R.string.import_added_summary,
                        pluralStringResource(R.plurals.import_finds_added, result.added, result.added),
                        if (result.photos > 0) pluralStringResource(R.plurals.import_with_photographs, result.photos, result.photos) else "",
                        if (result.alreadyHere > 0)
                            stringResource(R.string.import_already_here, result.alreadyHere) else "",
                    ) + if (result.heldBack > 0)
                        pluralStringResource(R.plurals.import_held_back, result.heldBack, result.heldBack) else ""
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButtonMMD(
                onClick = { imported = null },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { TextMMD(stringResource(R.string.import_close)) }
        }
    }

    val forPhotos = draft
    if (photographing && forPhotos != null) {
        EInkDialog(onDismiss = { photographing = false }) {
            PhotoSheet(
                filled = forPhotos.photos.map { it.slot }.toSet(),
                onCaptured = vm::takeInPhoto,
                onPicked = { slot, uri ->
                    vm.takeInPicked(slot, uri) {
                        FootNotice.say(context0, context0.getString(R.string.photos_not_readable))
                    }
                },
                onClose = { photographing = false },
            )
        }
    }
}

private fun thisMonth(): Int =
    Calendar.getInstance().get(Calendar.MONTH) + 1
