package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.mudita.mmd.components.lazy.LazyColumnMMD

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
    onJournal: () -> Unit = {},
    journalCount: Int = 0,
    onCandidate: (String) -> Unit = {},
) {
    // Keyed on what they actually depend on. Choosing a state on a multi-select question
    // is local screen state and must not re-run the key over a hundred taxa on each tap.
    val questionId = remember(draft.answers, draft.revisiting) { vm.currentQuestion(draft) }
    val ranking = remember(draft.answers) { vm.engine.rank(draft.answers) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

        // The way to the journal, from the screen the app opens on. A row rather than a
        // hamburger: it goes to one place and says which.
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                // The app's name, not a bare noun. This is the only heading on the
                // question screen, and "Journal" left the one screen somebody spends a
                // walk inside unnamed.
                if (journalCount == 0) "Mushroom Journal" else "Mushroom Journal · $journalCount",
                style = MaterialThemeTypography().bodySmall,
                modifier = Modifier.weight(1f).clickable(onClick = onJournal),
            )
        }
        HorizontalDividerMMD()

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

        val choose = { valueId: String ->
            val selected = valueId in picked
            picked = when {
                !multi -> setOf(valueId)
                selected -> picked - valueId
                else -> picked + valueId
            }
            if (!multi) vm.answer(questionId, picked)
        }

        // Pictures where there are pictures. Colour and smell get none — a monochrome
        // panel cannot draw a brown spore print or an almond smell, and a drawing that
        // stands in for one would be worse than the word.
        val drawn = CharacterArt.coversAll(questionId, values.map { it.id })

        // Measured rather than assumed: the list gets whatever is left between the
        // question and the footer, and the tiles need to know how much that is if they
        // are to divide it into whole rows.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val viewport = maxHeight
        LazyColumnMMD(
            Modifier.fillMaxSize().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (drawn) {
                choiceGrid(
                    characterId = questionId,
                    values = values,
                    picked = picked,
                    viewport = viewport - 8.dp,
                    onPick = choose,
                )
            } else {
                /*
                 * The same whole-rows-to-a-page rule as the picture tiles.
                 *
                 * These are the questions with no drawings — colour, smell, taste — and
                 * they are the longest lists in the app, so they were the worst for it:
                 * sixteen smells with the fifth sliced through by the fold. The natural
                 * height is deliberately generous, because guessing it low would squeeze
                 * a gloss out of its own box and move the clipping inside the button.
                 */
                val tall = values.any { it.gloss != null }
                val optionHeight = heightToFill(
                    viewport = viewport - 8.dp,
                    natural = if (tall) 55.dp else 42.dp,
                    count = values.size,
                    gap = GAP,
                )
                items(values, key = { it.id }) { value ->
                    val selected = value.id in picked
                    // The gloss where there is one. Colour is the character with no
                    // drawings and the most words that need explaining, and these two
                    // facts are the same fact.
                    val face: @Composable () -> Unit = {
                        if (value.gloss == null) {
                            Text(value.label)
                        } else {
                            Column {
                                Text(value.label)
                                Text(
                                    value.gloss!!,
                                    style = MaterialThemeTypography().bodySmall,
                                )
                            }
                        }
                    }
                    val shape = Modifier.fillMaxWidth()
                        .then(optionHeight?.let { Modifier.height(it) } ?: Modifier)
                    if (selected) {
                        ButtonMMD(onClick = { choose(value.id) }, modifier = shape) { face() }
                    } else {
                        OutlinedButtonMMD(onClick = { choose(value.id) }, modifier = shape) { face() }
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


        }
        }

        Footer(
            draft = draft,
            ranking = ranking,
            vm = vm,
            onAddPhoto = onAddPhoto,
            onDone = onDone,
            // Almost every character is multi-select now, so a question is committed by
            // pressing on rather than by tapping a choice. That button was at the end of
            // the list, which on a twelve-state question meant scrolling past everything
            // to answer it. It belongs where the eye already is.
            onCandidate = onCandidate,
            onNext = if (multi && picked.isNotEmpty()) {
                { vm.answer(questionId, picked) }
            } else {
                null
            },
        )
    }
}

@Composable
private fun NothingLeftToAsk(
    draft: JournalViewModel.Draft,
    ranking: KeyEngine.Ranking,
    onAddPhoto: () -> Unit,
    onDone: () -> Unit,
) {
    /*
     * The end of the key, and the list somebody actually walks away with.
     *
     * This showed the top three by score under the word "these", which is not the same
     * set as the ones that fit: the ranking sorts on score alone, and a mismatch is worth
     * -2.0 against a full match's +1.0, so a contradicted taxon can outrank an
     * uncontradicted one. The same fault as the entry screen's "Not yet ruled out" list,
     * in the place it matters most — this is the screen the questions were leading to.
     *
     * When nothing fits everything, the nearest few are still the useful answer, but the
     * line above them has to stop calling them "these" and say what they are.
     */
    val live = ranking.live
    val shortlist = ranking.shortlist(3)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Nothing more to ask", fontWeight = FontWeight.Bold)
        Text(
            if (live.isEmpty()) {
                "Nothing fits everything you wrote down — a real mushroom against a " +
                    "description of a typical one. These come nearest, and one of the " +
                    "answers may be worth looking at again."
            } else {
                "Every question that would tell these apart has been answered or skipped."
            },
            style = MaterialThemeTypography().bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        shortlist.forEach {
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
    onCandidate: (String) -> Unit = {},
    onNext: (() -> Unit)? = null,
) {
    HorizontalDividerMMD(Modifier.padding(top = 4.dp))

    if (draft.answers.answeredCount == 0) {
        Text(
            "Answer anything to start narrowing.",
            style = MaterialThemeTypography().bodySmall,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    } else {
        val live = ranking.live
        Text(
            // "of 0 still possible" printed directly above two named candidates, which
            // is what the screen said whenever the answers contradicted each other —
            // and answers contradict each other often, because a person is looking at a
            // real mushroom and the pack is a description of a typical one. Say that
            // plainly instead of printing a count that argues with the list under it.
            if (live.isEmpty()) {
                "Nothing fits everything you have said. Nearest anyway:"
            } else {
                "Closest so far, of ${live.size} still possible"
            },
            style = MaterialThemeTypography().bodySmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        // The ones that still fit, so that "closest" means closest among those the line
        // above has just counted. Showing the top two by score could put a contradicted
        // taxon under "of 2 still possible", because the ranking sorts on score alone and
        // a mismatch is -2.0 against a full match's +1.0.
        val shown = ranking.shortlist(2)
        shown.forEach { c ->
            Text(
                c.taxon.commonName?.let { n -> "${c.taxon.scientificName} — $n" }
                    ?: c.taxon.scientificName,
                style = MaterialThemeTypography().bodySmall,
                // Bold where it can kill. A lethal taxon that is also the closest match
                // used to be printed plain here and then again, bold, on the line below
                // — the same name twice, two lines apart, which reads as a fault rather
                // than as a warning. It is marked where it stands instead.
                fontWeight = if (c.taxon.hazard.severity == Hazard.Severity.LETHAL) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCandidate(c.taxon.id) }
                    .padding(vertical = 3.dp),
            )
        }

        // And the line below names only what is not already on the screen. Nothing is
        // dropped: a lethal candidate is either standing in the list in bold or named
        // here, and never neither.
        val visible = shown.map { it.taxon.id }.toSet()
        val hazards = ranking.hazards
            .filter { it.taxon.hazard.severity == Hazard.Severity.LETHAL }
            .filter { it.taxon.id !in visible }
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
        if (onNext != null) {
            // Answering the question in front of you beats ending the whole entry, so
            // while there is something chosen this is what the solid button does.
            ButtonMMD(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Next") }
        } else {
            ButtonMMD(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Done") }
        }
    }
}

@Composable
private fun MaterialThemeTypography() = androidx.compose.material3.MaterialTheme.typography
