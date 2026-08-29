package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.key.Frequency
import com.wanderwildwood.kinokocho.key.Hazard
import com.wanderwildwood.kinokocho.key.Taxon
import com.wanderwildwood.kinokocho.schema.Character
import java.text.DateFormatSymbols
import java.util.Locale

/**
 * One candidate, read properly.
 *
 * The point of narrowing eighty-six taxa to four is that four is a number of things a
 * person can actually go and check. This is where they check: what it is said to look
 * like, what it does that the key never asked about, what it is confused with, and what
 * happens if the confusion goes the wrong way.
 *
 * It is still not an identification. The reader is comparing a description against the
 * thing in their hand and deciding for themselves, which is what "narrows, does not
 * decide" means in practice — this screen is where the deciding gets handed over.
 *
 * **How it is laid out, and why it was laid out again.** The first version listed every
 * described character as two lines of running text, each opening with the whole question
 * the key would have asked — "What is the cap surface like?", then "Scaly". Twenty-five
 * of those is not a description, it is a page of prose with the answers lost in it.
 *
 * So there are three levels now: the section, the part of the mushroom, and the row. A
 * row is a short noun in a fixed column and the value beside it, which means the values
 * line up and can be read straight down without reading anything else. The nouns can be
 * short — "Shape", "Edge", "Colour" — because the heading above them says which part of
 * the mushroom they belong to.
 *
 * What the reader answered is marked in place rather than gathered into a second list.
 * A separate block of agreements is a second thing to read; a tick against the row is
 * not.
 */
@Composable
fun CandidateScreen(
    vm: JournalViewModel,
    taxon: Taxon,
    answers: com.wanderwildwood.kinokocho.key.KeyEngine.Answers,
    onClose: () -> Unit,
) {
    val described = vm.schema.characters.filter { taxon.characters.containsKey(it.id) }
    val answered = described.filter { answers.values[it.id].orEmpty().isNotEmpty() }
    val agree = answered.count { agrees(vm, taxon, it, answers) }
    val differ = answered.size - agree

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
    ) {
        item {
            Text(
                taxon.scientificName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
            taxon.commonName?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Danger before description. A person reading this is holding the mushroom.
        if (taxon.hazard.severity != Hazard.Severity.NONE_KNOWN) {
            item {
                Section(
                    when (taxon.hazard.severity) {
                        Hazard.Severity.LETHAL -> "This one can kill"
                        Hazard.Severity.SEVERE -> "This one can cause serious harm"
                        Hazard.Severity.INTOXICATION -> "This one is intoxicating"
                        Hazard.Severity.GI -> "This one makes people ill"
                        else -> "Not known to be safe"
                    }
                )
                taxon.hazard.onset?.takeIf { it.isNotBlank() && it != "—" }?.let {
                    Row(it, "Comes on")
                }
                taxon.hazard.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        taxon.note?.takeIf { it.isNotBlank() }?.let { note ->
            item {
                Section("What to look for")
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
        }

        /*
         * Where and when, gathered up rather than left among forty other characters.
         *
         * "Is this the mushroom I am looking at?" is answered as much by the place as by
         * the mushroom: the wrong wood, the wrong month or the wrong altitude rules
         * things out that no amount of squinting at gills will.
         */
        item {
            Section(Character.Group.WHERE.heading)
            vm.schema.characters
                .filter { it.group == Character.Group.WHERE }
                .forEach { character ->
                    val text = statesOf(vm, taxon, character) ?: return@forEach
                    Row(text, character.noun, marked(vm, taxon, character, answers))
                }
            if (taxon.seasonMonths.isNotEmpty()) {
                val names = DateFormatSymbols(Locale.getDefault()).shortMonths
                Row(taxon.seasonMonths.sorted().joinToString(", ") { names[it - 1] }, "Season")
            }
            val cap = taxon.measurements["cap_width_mm"]
            val stem = taxon.measurements["stipe_height_mm"]
            if (cap != null || stem != null) {
                Row(
                    listOfNotNull(
                        cap?.let { "cap ${it.first}–${it.last} mm" },
                        stem?.let { "stem ${it.first}–${it.last} mm" },
                    ).joinToString(", "),
                    "Size",
                )
            }
            // What was measured, laid against those ranges. Said as "bigger than usual"
            // rather than "wrong", because a published range is about the tenth to the
            // ninetieth percentile of what grows and a button is under all of them.
            answers.measurements.forEach { (key, mm) ->
                val range = taxon.measurements[key] ?: return@forEach
                val name = vm.schema.character("size")
                    ?.let { vm.schema.valuesOf(it) }
                    ?.firstOrNull { it.id == key }?.label?.substringBefore(" (")?.lowercase()
                    ?: key
                Row(
                    "$mm mm — " + when {
                        mm in range -> "within the usual range for $name"
                        mm < range.first ->
                            "smaller than usual for $name, which young ones often are"
                        else -> "bigger than usual for $name"
                    },
                    "Measured",
                )
            }
        }

        // High up, because this is the question a person is really asking: not "does
        // this fit" but "what else fits, and how would I tell?"
        if (taxon.lookalikes.isNotEmpty()) {
            item {
                Section("Confused with")
                taxon.lookalikes.forEach { look ->
                    val other = vm.pack.taxon(look.taxon)
                    Text(
                        other?.commonName?.let { "${other.scientificName} — $it" }
                            ?: other?.scientificName ?: look.taxon,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(look.note, style = MaterialTheme.typography.bodySmall)
                    if (look.discriminators.isNotEmpty()) {
                        Text(
                            "Tells them apart: " + look.discriminators
                                .mapNotNull { vm.schema.character(it)?.noun?.lowercase() }
                                .joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            Section("Does it match?")
            Text(
                when {
                    answered.isEmpty() ->
                        "You have not recorded anything this is described by yet."
                    differ == 0 -> "Everything you recorded agrees."
                    // A single disagreement does not remove a candidate, it only moves
                    // it down, and the reader is the one who decides which it was.
                    else -> "$agree agree, $differ do not."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Everything the row says, by part of the mushroom. Where and when is already
        // above, so it is not repeated here.
        Character.Group.entries.filter { it != Character.Group.WHERE }.forEach { group ->
            val rows = described.filter { it.group == group }
                .mapNotNull { c -> statesOf(vm, taxon, c)?.let { c to it } }
            if (rows.isEmpty()) return@forEach
            item(key = "g${group.name}") {
                Text(
                    group.heading,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
                rows.forEach { (character, text) ->
                    Row(text, character.noun, marked(vm, taxon, character, answers))
                }
            }
        }

        item {
            // Said plainly rather than buried: none of this has been checked by anyone
            // who would know, and a reader deciding from it deserves to be told.
            Section("Where this comes from")
            taxon.sources.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            if (!taxon.reviewed) {
                Text(
                    "Not checked by a mycologist. Written from published descriptions, " +
                        "and no substitute for asking someone.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            OutlinedButtonMMD(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            ) { Text("Back") }
        }
    }
}

/** What the row says about a character, or null if it says nothing worth printing. */
private fun statesOf(vm: JournalViewModel, taxon: Taxon, character: Character): String? {
    val labels = taxon.characters[character.id].orEmpty()
        .filter { it.frequency != Frequency.RARELY }
        .mapNotNull { st ->
            vm.schema.valuesOf(character).firstOrNull { it.id == st.value }?.label
        }
    return labels.takeIf { it.isNotEmpty() }?.joinToString(", ")
}

private fun agrees(
    vm: JournalViewModel,
    taxon: Taxon,
    character: Character,
    answers: com.wanderwildwood.kinokocho.key.KeyEngine.Answers,
): Boolean {
    val chosen = answers.values[character.id].orEmpty()
        .filterNot { vm.schema.isUncertainValue(character.id, it) }
    val states = taxon.characters[character.id].orEmpty()
    return chosen.isNotEmpty() && chosen.any { a -> states.any { it.value == a } }
}

/**
 * What the reader said about this row, if anything — shown against the row rather than
 * collected into a list of its own, so that reading the description and checking your
 * own answers is one pass instead of two.
 */
@Composable
private fun marked(
    vm: JournalViewModel,
    taxon: Taxon,
    character: Character,
    answers: com.wanderwildwood.kinokocho.key.KeyEngine.Answers,
): String? {
    val chosen = answers.values[character.id].orEmpty()
        .filterNot { vm.schema.isUncertainValue(character.id, it) }
    if (chosen.isEmpty()) return null
    if (agrees(vm, taxon, character, answers)) return "you recorded this"
    val mine = chosen.mapNotNull { c ->
        vm.schema.valuesOf(character).firstOrNull { it.id == c }?.label
    }
    return "you recorded " + mine.joinToString(", ").lowercase()
}

/**
 * One row: a short noun in a fixed column, the value beside it.
 *
 * The fixed column is the whole point. With the label inline the values start at a
 * different place on every line and the eye has to read the labels to find them; in a
 * column they stack, and a person can run down the values alone.
 */
@Composable
private fun Row(value: String, label: String, note: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(96.dp).padding(end = 6.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDividerMMD(Modifier.padding(top = 14.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}
