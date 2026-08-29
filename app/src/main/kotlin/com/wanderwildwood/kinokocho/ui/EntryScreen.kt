package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.key.Hazard
import com.wanderwildwood.kinokocho.schema.Character
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One find, read back.
 *
 * Separate from the question screen because reading and writing are different jobs. A
 * walk produces an entry in a few minutes standing up; the useful part happens later,
 * at a table, when the spore print has dropped and there is time to see what was
 * missed. This is that screen.
 */
@Composable
fun EntryScreen(
    vm: JournalViewModel,
    draft: JournalViewModel.Draft,
    onContinue: () -> Unit,
    onAddPhoto: () -> Unit,
    onClose: () -> Unit,
) {
    val ranking = vm.engine.rank(draft.answers)
    val missing = vm.engine.mostValuableMissing(draft.answers)
    val safety = vm.engine.safetyNotes(draft.answers)
    val sporePrintPending = "spore_print" !in draft.answers.values.keys

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
    ) {
        item {
            Text(
                dateOf(draft.recordedAt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp),
            )
            if (draft.placeNote.isNotBlank()) {
                Text(draft.placeNote, style = MaterialTheme.typography.bodySmall)
            }
            val n = draft.answers.answeredCount
            val p = draft.photos.size
            Text(
                "$n character${if (n == 1) "" else "s"} · " +
                    "$p photograph${if (p == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        /*
         * The characters that cannot be got in a wood, answered here.
         *
         * These are the reason an entry stays open overnight, and they are the only
         * ones the question screen will never offer: nextQuestion asks field
         * characters, because standing in a wood being asked for a spore print is
         * useless. So they live here, at the table, where the answer exists.
         */
        val deferred = vm.schema.characters
            .filter { it.availability == Character.Availability.DEFERRED }
            .filter { vm.schema.isApplicable(it.id, draft.answers.values) }
            .filter { it.id !in draft.answers.values.keys }

        if (deferred.isNotEmpty()) {
            item {
                Section("Still to do, at home")
                if (sporePrintPending) {
                    Text(
                        "Leave the cap gills-down on paper overnight. The spore print " +
                            "settles more than any other character and cannot be got " +
                            "in the field.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            items4(deferred.size) { i ->
                val character = deferred[i]
                var open by remember(character.id) { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    OutlinedButtonMMD(
                        onClick = { open = !open },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(character.label) }

                    if (open) {
                        vm.schema.valuesOf(character).forEach { value ->
                            OutlinedButtonMMD(
                                onClick = {
                                    vm.answer(character.id, setOf(value.id))
                                    open = false
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            ) { Text(value.label) }
                        }
                        // Not answering is a real answer here: a print that never
                        // dropped is a fact about the specimen, not a gap in the record.
                        OutlinedButtonMMD(
                            onClick = { vm.skip(character.id); open = false },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) { Text("I tried and cannot say") }
                    }
                }
            }
        }

        // Never a name and never a percentage. This is a list of what has not been
        // ruled out, and the wording has to keep saying so.
        if (draft.answers.answeredCount > 0) {
            item {
                Section("Not yet ruled out")
                val live = ranking.candidates.filter { it.mismatched == 0 }
                Text(
                    "${live.size} of ${ranking.candidates.size} still fit what you wrote down.",
                    style = MaterialTheme.typography.bodySmall,
                )
                ranking.candidates.take(4).forEach { c ->
                    Text(
                        c.taxon.commonName?.let { "${c.taxon.scientificName} — $it" }
                            ?: c.taxon.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }

        val lethal = safety.filter { it.taxon.hazard.severity == Hazard.Severity.LETHAL }
        if (lethal.isNotEmpty()) {
            item {
                Section("Dangerous, and not ruled out")
                lethal.forEach { note ->
                    Text(
                        note.taxon.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    note.notes.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    if (note.discriminators.isNotEmpty()) {
                        Text(
                            "Would settle it: " + note.discriminators
                                .mapNotNull { vm.schema.character(it)?.label }
                                .joinToString(" "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        /*
         * What you did not write down.
         *
         * The point of the whole app, arguably. It is not a reproach about this find —
         * that mushroom is already back in the wood — it is what to look at first on
         * the next one. Three is enough; a list of everything unanswered is a list of
         * everything, and teaches nothing.
         */
        if (missing.isNotEmpty()) {
            item {
                Section("What would have narrowed it most")
                missing.take(3).forEach { (characterId, _) ->
                    val character = vm.schema.character(characterId) ?: return@forEach
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The first drawn state of the character, as a reminder of what
                        // the question even looks like.
                        vm.schema.valuesOf(character).firstNotNullOfOrNull {
                            CharacterArt.of(characterId, it.id)
                        }?.let { art ->
                            Icon(
                                painter = painterResource(art.drawable),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp).padding(end = 8.dp),
                            )
                        }
                        Column {
                            Text(character.label, style = MaterialTheme.typography.bodySmall)
                            if (character.availability.name == "DEFERRED") {
                                Text(
                                    "at home, not in the field",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Photographs, shown rather than counted. A thumbnail is also the only way to
        // notice that the picture of the base came out unusable before the specimen is
        // back in the wood.
        if (draft.photos.isNotEmpty()) {
            item {
                Section("Photographs")
                val context = LocalContext.current
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items3(draft.photos.size) { i ->
                        val photo = draft.photos[i]
                        val file = File(photoDir(context), photo.fileName)
                        val bitmap = remember(photo.fileName) {
                            runCatching {
                                BitmapFactory.decodeFile(
                                    file.absolutePath,
                                    // Sampled down: a phone camera JPEG decoded at full
                                    // size to fill a 96dp box is megabytes for nothing.
                                    BitmapFactory.Options().apply { inSampleSize = 8 },
                                )?.asImageBitmap()
                            }.getOrNull()
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = photo.slot,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(96.dp),
                                )
                            } else {
                                Text("(missing)", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                PhotoSlot.entries.firstOrNull { it.id == photo.slot }
                                    ?.label ?: photo.slot,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        // Where it was, typed rather than read off GPS. Foraging spots are the reason
        // this app never asks for location.
        item {
            Section("Where, and anything else")
            var place by remember(draft.uuid) { mutableStateOf(draft.placeNote) }
            TextFieldMMD(
                value = place,
                onValueChange = { place = it; vm.setPlaceNote(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("The place, in your own words") },
            )
            var note by remember(draft.uuid) { mutableStateOf(draft.note) }
            TextFieldMMD(
                value = note,
                onValueChange = { note = it; vm.setNote(it) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text("Anything the questions did not cover") },
            )
        }

        item {
            Section("What you wrote down")
        }
        items2(draft, vm)

        item {
            HorizontalDividerMMD(Modifier.padding(top = 12.dp))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButtonMMD(onClick = onAddPhoto, modifier = Modifier.weight(1f)) {
                    Text("Photo")
                }
                ButtonMMD(onClick = onContinue, modifier = Modifier.weight(1f)) {
                    Text("Answer more")
                }
            }
            OutlinedButtonMMD(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            ) { Text("Close") }
        }
    }
}

/** The answers themselves, one line each, label and value. */
private fun androidx.compose.foundation.lazy.LazyListScope.items4(
    count: Int,
    content: @Composable (Int) -> Unit,
) = items(count) { content(it) }

private fun androidx.compose.foundation.lazy.LazyListScope.items3(
    count: Int,
    content: @Composable (Int) -> Unit,
) = items(count) { content(it) }

private fun androidx.compose.foundation.lazy.LazyListScope.items2(
    draft: JournalViewModel.Draft,
    vm: JournalViewModel,
) {
    val entries = draft.answers.values.entries.toList()
    if (entries.isEmpty()) {
        item {
            Text(
                "Nothing yet.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }
    items(entries.size) { i ->
        val (characterId, chosen) = entries[i]
        val character = vm.schema.character(characterId)
        if (character != null) {
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    character.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    chosen.mapNotNull { c ->
                        vm.schema.valuesOf(character).firstOrNull { it.id == c }?.label
                    }.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDividerMMD(Modifier.padding(top = 14.dp))
    Text(
        title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

private fun dateOf(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))
