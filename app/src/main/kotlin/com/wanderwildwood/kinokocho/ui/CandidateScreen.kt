package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.key.Frequency
import com.wanderwildwood.kinokocho.key.Hazard
import com.wanderwildwood.kinokocho.key.Taxon
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
 */
@Composable
fun CandidateScreen(
    vm: JournalViewModel,
    taxon: Taxon,
    answers: com.wanderwildwood.kinokocho.key.KeyEngine.Answers,
    onClose: () -> Unit,
) {
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
                    Text("Comes on: $it", style = MaterialTheme.typography.bodySmall)
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
         * What it is said to be, laid against what was answered.
         *
         * Marked where the answer agrees and where it does not, because a candidate that
         * survived the key on nine characters and disagrees on the tenth is exactly the
         * thing worth looking at again — and the key will not say so on its own, since a
         * single mismatch only moves a taxon down the list rather than off it.
         */
        item { Section("What it is said to be") }
        val described = vm.schema.characters.filter { taxon.characters.containsKey(it.id) }
        items(described.size) { i ->
            val character = described[i]
            val states = taxon.characters[character.id].orEmpty()
            val labels = states.filter { it.frequency != Frequency.RARELY }.mapNotNull { st ->
                vm.schema.valuesOf(character).firstOrNull { it.id == st.value }?.label
            }
            if (labels.isNotEmpty()) {
                val answered = answers.values[character.id].orEmpty()
                val agrees = answered.isNotEmpty() &&
                    answered.any { a -> states.any { it.value == a } }
                val disagrees = answered.isNotEmpty() && !agrees

                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            character.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (disagrees) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            labels.joinToString(", ") +
                                if (disagrees) "  — not what you recorded" else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (agrees) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        if (taxon.lookalikes.isNotEmpty()) {
            item {
                Section("Confused with")
                taxon.lookalikes.forEach { look ->
                    val other = vm.pack.taxon(look.taxon)
                    Text(
                        other?.scientificName ?: look.taxon,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(look.note, style = MaterialTheme.typography.bodySmall)
                    if (look.discriminators.isNotEmpty()) {
                        Text(
                            "Settled by: " + look.discriminators
                                .mapNotNull { vm.schema.character(it)?.label }
                                .joinToString(" "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        item {
            Section("When and how big")
            if (taxon.seasonMonths.isNotEmpty()) {
                val names = DateFormatSymbols(Locale.getDefault()).shortMonths
                Text(
                    taxon.seasonMonths.sorted().joinToString(", ") { names[it - 1] },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            taxon.measurements["cap_width_mm"]?.let {
                Text(
                    "Cap ${it.first}–${it.last} mm across",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            taxon.measurements["stipe_height_mm"]?.let {
                Text(
                    "Stem ${it.first}–${it.last} mm tall",
                    style = MaterialTheme.typography.bodySmall,
                )
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
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            OutlinedButtonMMD(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            ) { Text("Back") }
        }
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDividerMMD(Modifier.padding(top = 12.dp))
    Text(
        title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}
