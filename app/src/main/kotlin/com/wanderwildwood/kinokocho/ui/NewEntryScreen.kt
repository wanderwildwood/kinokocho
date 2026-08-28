package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.key.Hazard
import com.wanderwildwood.kinokocho.key.KeyEngine
import com.wanderwildwood.kinokocho.schema.Character

/**
 * One question at a time, with what it has narrowed to underneath.
 *
 * The question is chosen by the engine rather than by a fixed list, so it changes as
 * the candidates change. Every question is skippable: an entry with three characters
 * is a real entry, and a key that insists on answers gets false ones.
 */
@Composable
fun NewEntryScreen(
    vm: JournalViewModel,
    draft: JournalViewModel.Draft,
    onAddPhoto: () -> Unit,
    onDone: () -> Unit,
) {
    val questionId = vm.currentQuestion(draft)
    val ranking = vm.engine.rank(draft.answers)

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

        if (questionId == null) {
            NothingLeftToAsk(draft, ranking, onAddPhoto, onDone)
            return@Column
        }

        val character = vm.schema.character(questionId)!!
        val values = vm.schema.valuesOf(character)
        val already = draft.answers.values[questionId].orEmpty()
        var picked by remember(questionId) { mutableStateOf(already) }

        Text(
            character.label,
            style = MaterialThemeTypography().titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        character.hint?.let {
            Text(it, style = MaterialThemeTypography().bodySmall, modifier = Modifier.padding(top = 4.dp))
        }

        val multi = character.cardinality == Character.Cardinality.MULTI
        if (multi) {
            Text(
                "Choose as many as apply.",
                style = MaterialThemeTypography().bodySmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(values, key = { it.id }) { value ->
                val selected = value.id in picked
                val choose = {
                    picked = when {
                        !multi -> setOf(value.id)
                        selected -> picked - value.id
                        else -> picked + value.id
                    }
                    if (!multi) vm.answer(questionId, picked)
                }
                if (selected) {
                    ButtonMMD(onClick = choose, modifier = Modifier.fillMaxWidth()) {
                        Text(value.label)
                    }
                } else {
                    OutlinedButtonMMD(onClick = choose, modifier = Modifier.fillMaxWidth()) {
                        Text(value.label)
                    }
                }
            }

            item {
                // Not a state of the mushroom. A state of the person looking at it, and
                // it must never be scored as though they had answered.
                OutlinedButtonMMD(
                    onClick = { vm.skip(questionId) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                ) {
                    Text(if (character.notTested) "I looked and cannot say" else "Skip this")
                }
            }

            if (multi) {
                item {
                    ButtonMMD(
                        onClick = { vm.answer(questionId, picked) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Next") }
                }
            }
        }

        Footer(draft, ranking, vm, onAddPhoto, onDone)
    }
}

@Composable
private fun NothingLeftToAsk(
    draft: JournalViewModel.Draft,
    ranking: KeyEngine.Ranking,
    onAddPhoto: () -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Nothing more to ask", fontWeight = FontWeight.Bold)
        Text(
            "Every question that would tell these apart has been answered or skipped.",
            style = MaterialThemeTypography().bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        ranking.candidates.take(3).forEach {
            Text("· ${it.taxon.scientificName}", modifier = Modifier.padding(top = 6.dp))
        }
        ButtonMMD(onClick = onAddPhoto, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Add a photograph")
        }
        OutlinedButtonMMD(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("Done")
        }
    }
}

/**
 * What the answers have narrowed to, and what is still dangerous.
 *
 * Deliberately never a percentage and never a single name. The wording is "closest so
 * far", because that is what a ranking is - the app has not identified anything.
 */
@Composable
private fun Footer(
    draft: JournalViewModel.Draft,
    ranking: KeyEngine.Ranking,
    vm: JournalViewModel,
    onAddPhoto: () -> Unit,
    onDone: () -> Unit,
) {
    HorizontalDividerMMD(Modifier.padding(top = 4.dp))

    if (draft.answers.answeredCount == 0) {
        Text(
            "Answer anything to start narrowing.",
            style = MaterialThemeTypography().bodySmall,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    } else {
        val live = ranking.candidates.filter { it.mismatched == 0 }
        Text(
            "Closest so far, of ${live.size} still possible",
            style = MaterialThemeTypography().bodySmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        ranking.candidates.take(2).forEach {
            Text(
                it.taxon.commonName?.let { c -> "${it.taxon.scientificName} - $c" }
                    ?: it.taxon.scientificName,
                style = MaterialThemeTypography().bodySmall,
            )
        }

        val hazards = ranking.hazards.filter { it.taxon.hazard.severity == Hazard.Severity.LETHAL }
        if (hazards.isNotEmpty()) {
            Text(
                "Still not ruled out: " + hazards.joinToString(", ") { it.taxon.scientificName },
                style = MaterialThemeTypography().bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButtonMMD(onClick = onAddPhoto, modifier = Modifier.weight(1f)) {
            Text(if (draft.photos.isEmpty()) "Photo" else "Photo (${draft.photos.size})")
        }
        ButtonMMD(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Done") }
    }
}

@Composable
private fun MaterialThemeTypography() = androidx.compose.material3.MaterialTheme.typography
