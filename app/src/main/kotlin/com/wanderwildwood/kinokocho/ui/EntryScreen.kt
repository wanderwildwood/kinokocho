package com.wanderwildwood.kinokocho.ui

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onCandidate: (String) -> Unit = {},
) {
    /*
     * Keyed on the answers, because nothing else changes them.
     *
     * All three walk the whole pack, and this screen holds three text fields: the place,
     * the note, and what it turned out to be. Every keystroke in any of them changes the
     * draft, recomposes, and re-ranked a hundred taxa, re-scored every character for the
     * advice list, and rebuilt the safety notes — measured at about two milliseconds a
     * keystroke on a laptop, which is not two milliseconds on the phone this is for.
     * Typing a name is not a reason to work out what the mushroom might be again.
     */
    val ranking = remember(draft.answers) { vm.engine.rank(draft.answers) }
    val missing = remember(draft.answers) { vm.engine.mostValuableMissing(draft.answers) }
    val safety = remember(draft.answers) { vm.engine.safetyNotes(draft.answers) }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCandidate(c.taxon.id) }
                            .padding(top = 6.dp, bottom = 6.dp),
                    )
                }
            }
        }

        /*
         * Dangerous, and not ruled out.
         *
         * Names and what would settle them, and nothing else. This section used to carry
         * every lookalike note for every live hazard, and when the pack grew from
         * twenty-nine confusions to sixty-one it became two and a half screens of prose
         * — under the one heading in the app that a person might be reading while
         * holding the thing. Long is the same as unread here.
         *
         * The prose is on each mushroom's own page, which is now one tap from this list.
         * What belongs here is which of them are still live and what question would
         * remove them, because those are the two things a person can act on standing up.
         */
        val lethal = safety.filter { it.taxon.hazard.severity == Hazard.Severity.LETHAL }
        if (lethal.isNotEmpty()) {
            item {
                Section("Dangerous, and not ruled out")
                Text(
                    "Tap one to read it properly.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            items3(minOf(lethal.size, MOST_HAZARDS)) { i ->
                val note = lethal[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCandidate(note.taxon.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The drawing beside the name, on the one screen where a person is
                    // most likely to be holding the thing. This list is the deadly
                    // candidates still in play, which is the set the plates were drawn
                    // for first — a shape is quicker to check against a mushroom in your
                    // hand than a sentence is.
                    TaxonPlate.of(note.taxon.id)?.let { plate ->
                        Image(
                            painter = painterResource(plate),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.size(52.dp).padding(end = 8.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                    Text(
                        note.taxon.commonName
                            ?.let { "${note.taxon.scientificName} — $it" }
                            ?: note.taxon.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (note.discriminators.isNotEmpty()) {
                        Text(
                            "Would settle it: " + note.discriminators
                                .mapNotNull { vm.schema.character(it)?.noun?.lowercase() }
                                .distinct()
                                .take(4)
                                .joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    }
                }
            }
            if (lethal.size > MOST_HAZARDS) {
                item {
                    Text(
                        "…and ${lethal.size - MOST_HAZARDS} more, which answering " +
                            "anything above will start to rule out.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
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

        /*
         * Photographs, shown rather than counted. A thumbnail is also the only way to
         * notice that the picture of the base came out unusable before the specimen is
         * back in the wood — which is only useful if it can then be taken again, and
         * until now it could not: there was no way to remove a photograph at all, so the
         * first bad one in a slot stayed there for the life of the entry.
         *
         * Tapping arms it and a second tap removes it, per the house rule. A photograph
         * is a thing that cannot be got back once the mushroom is in the wood again, and
         * a thumbnail is small and a thumb is not.
         */
        if (draft.photos.isNotEmpty()) {
            item {
                Section("Photographs")
                var arming by remember(draft.uuid) { mutableStateOf<String?>(null) }
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
                        val armed = arming == photo.fileName
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                if (armed) {
                                    vm.deletePhoto(photo)
                                    arming = null
                                } else {
                                    arming = photo.fileName
                                }
                            },
                        ) {
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
                                if (armed) "Remove it?"
                                else PhotoSlot.entries.firstOrNull { it.id == photo.slot }
                                    ?.label ?: photo.slot,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (armed) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (armed) {
                                Text(
                                    "tap again",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        /*
         * How big it was.
         *
         * Not one of the questions, and deliberately. A measurement never leaves a
         * single taxon standing - published ranges overlap far too much for that - so
         * it loses to every state character the key could ask instead, and would sit at
         * the bottom of the queue for ever. But it is worth writing down and worth
         * scoring, so it is asked for here, with the place and the photographs, which
         * are the other things you record about the mushroom rather than deduce from it.
         *
         * Blank stays blank. Somebody who measured the cap and not the stem has said
         * something true, and filling the gap in for them would be inventing a number.
         */
        /*
         * And how old and how battered it is.
         *
         * These sit here for the same reason size does — they are facts about the
         * specimen rather than the species, and no taxon will ever declare "age: old".
         * Both had been in the schema from the start with no way to record them and no
         * taxon that could match them, which made them dead weight; [KeyEngine] now
         * reads age to decide how much a missing ring is allowed to rule out, which is
         * what the schema said it was for all along.
         */
        vm.schema.characters
            .filter { it.id == "age" || it.id == "condition" }
            .forEach { character ->
                item(key = character.id) {
                    Section(character.label)
                    val chosen = draft.answers.values[character.id].orEmpty()
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        vm.schema.valuesOf(character).forEach { value ->
                            val picked = value.id in chosen
                            val pick = {
                                vm.answer(
                                    character.id,
                                    if (picked) emptySet() else setOf(value.id),
                                )
                            }
                            if (picked) {
                                ButtonMMD(onClick = pick, modifier = Modifier.weight(1f)) {
                                    Text(value.label)
                                }
                            } else {
                                OutlinedButtonMMD(
                                    onClick = pick,
                                    modifier = Modifier.weight(1f),
                                ) { Text(value.label) }
                            }
                        }
                    }
                    if (character.id == "age" && "old" in chosen) {
                        Text(
                            "A ring or a veil that is not there proves nothing on an " +
                                "old one, so nothing will be ruled out for missing them.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

        vm.schema.characters
            .filter { it.kind == Character.Kind.MEASUREMENT }
            .forEach { character ->
                item(key = character.id) {
                    Section(character.label)
                    character.hint?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    vm.schema.valuesOf(character).forEach { value ->
                        var typed by remember(draft.uuid, value.id) {
                            mutableStateOf(draft.answers.measurements[value.id]?.toString() ?: "")
                        }
                        TextFieldMMD(
                            value = typed,
                            onValueChange = { entered ->
                                typed = entered.filter { it.isDigit() }.take(4)
                                vm.measure(mapOf(value.id to typed.toIntOrNull()))
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            label = { Text(value.label) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                            singleLine = true,
                        )
                    }
                }
            }

        /*
         * What it turned out to be.
         *
         * The answer arrives after the walk — from a forum, a friend, a book at the
         * kitchen table — and an entry could hold everything about a mushroom except
         * what it was. It sits above the shortlist rather than below it, because once
         * somebody has said, the shortlist is history.
         *
         * Free text, and the shortlist is offered as a shortcut and not as a menu: the
         * honest answer is often "a Russula, probably variata", or a name this pack has
         * never heard of. Pinning it to the pack would mean the app could only be told
         * things it already knew.
         */
        item {
            Section("What it turned out to be")
            var name by remember(draft.uuid) { mutableStateOf(draft.identifiedAs) }
            TextFieldMMD(
                value = name,
                onValueChange = { name = it; vm.setIdentifiedAs(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("A name, once somebody says") },
                singleLine = true,
            )
            if (name.isBlank() && draft.answers.answeredCount > 0) {
                Text(
                    "Or take one from the list below.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                ranking.candidates.take(3).forEach { c ->
                    Text(
                        c.taxon.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                name = c.taxon.scientificName
                                vm.setIdentifiedAs(name)
                            }
                            .padding(top = 6.dp),
                    )
                }
            }
        }

        // Where it was. Typing it is the default; the button asks for coarse location
        // only, because a foraging patch is not a thing to keep at metre precision.
        item {
            Section("Where, and anything else")
            PlaceField(
                place = draft.placeNote,
                onPlaceChange = vm::setPlaceNote,
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
        items2(draft, vm, onContinue)

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
            // The whole point of the record: handing it to someone who can look at it.
            val context = LocalContext.current
            OutlinedButtonMMD(
                onClick = {
                    val intent = ShareFind.intent(context, vm.schema, vm.engine, draft)
                    context.startActivity(
                        Intent.createChooser(intent, "Ask someone about this find")
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) { Text("Ask someone about this") }

            OutlinedButtonMMD(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 20.dp),
            ) { Text("Close") }
        }
    }
}

/** The answers themselves, one line each, label and value. */
private fun androidx.compose.foundation.lazy.LazyListScope.items4(
    count: Int,
    content: @Composable (Int) -> Unit,
) = items(count) { content(it) }

/**
 * How many deadly candidates this screen names before it says "and more".
 *
 * Four is about what a person can hold in their head standing over a mushroom. The rest
 * are not hidden — the count is said, and every one of them is on the shortlist above.
 */
private const val MOST_HAZARDS = 4

private fun androidx.compose.foundation.lazy.LazyListScope.items3(
    count: Int,
    content: @Composable (Int) -> Unit,
) = items(count) { content(it) }

private fun androidx.compose.foundation.lazy.LazyListScope.items2(
    draft: JournalViewModel.Draft,
    vm: JournalViewModel,
    onContinue: () -> Unit,
) {
    val entries = draft.answers.values.entries.toList()
    val gaveUp = draft.answers.notTested.toList()
    if (entries.isEmpty() && gaveUp.isEmpty()) {
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
            // Tapping an answer reopens that question. Trying a different answer and
            // watching the list move is how a person learns which characters actually
            // decide anything — and the reader who wants to do that is the reader this
            // app is for.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.revisit(characterId); onContinue() }
                    .padding(vertical = 6.dp),
            ) {
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

    /*
     * And what was looked at and given up on.
     *
     * These used to be invisible: an entry where the reader had honestly said "I looked
     * and cannot say" to four questions read "Nothing yet". That is the answer the
     * schema goes out of its way to treat as real — somebody who never cut the mushroom
     * has not established that it does not bruise — and the reader could not see they
     * had given it, or change their mind about it.
     */
    items(gaveUp.size) { i ->
        val character = vm.schema.character(gaveUp[i])
        if (character != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.revisit(character.id); onContinue() }
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    character.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (character.notTested) "looked, cannot say" else "skipped",
                    style = MaterialTheme.typography.bodySmall,
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
