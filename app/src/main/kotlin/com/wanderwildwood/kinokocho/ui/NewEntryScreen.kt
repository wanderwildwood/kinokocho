package com.wanderwildwood.kinokocho.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.R
import com.wanderwildwood.kinokocho.key.Hazard
import com.wanderwildwood.kinokocho.key.KeyEngine
import com.wanderwildwood.kinokocho.schema.Character
import com.wanderwildwood.kinokocho.schema.CharacterSchema

/**
 * One question at a time, with what it has narrowed to underneath.
 *
 * The question is chosen by the engine rather than by a fixed list, so it changes as
 * the candidates change. Every question is skippable: an entry with three characters
 * is a real entry, and a key that insists on answers gets false ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Column(Modifier.fillMaxSize()) {
        // The way to the journal, from the screen the app opens on: the bar's title, which
        // says the app's name and goes to one place. A row rather than a hamburger.
        TopAppBarMMD(
            title = {
                TextMMD(
                    if (journalCount == 0) stringResource(R.string.entry_new_journal_link) else stringResource(R.string.entry_new_journal_link_count, journalCount),
                    modifier = Modifier.clickable(onClick = onJournal),
                )
            },
        )
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {

            if (questionId == null) {
                NothingLeftToAsk(draft, ranking, vm.schema, onAddPhoto, onDone)
                return@Column
            }

            val character = vm.schema.character(questionId)!!
            val values = vm.schema.valuesOf(character)
            val already = draft.answers.values[questionId].orEmpty()
            var picked by remember(questionId) { mutableStateOf(already) }

            TextMMD(
                character.label,
                style = MaterialThemeTypography().titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )
            character.hint?.let {
                TextMMD(it, style = MaterialThemeTypography().bodySmall, modifier = Modifier.padding(top = 2.dp))
            }

            val multi = character.cardinality == Character.Cardinality.MULTI
            if (multi) {
                TextMMD(
                    stringResource(R.string.entry_new_choose_many),
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
            val viewport = maxHeight - 8.dp
            val glossed = values.any { it.gloss != null }

            // What one choice occupies, worked out here rather than inside the list, because
            // the choices are sized to divide the list into whole rows.
            val unit = if (drawn) {
                heightToFill(
                    viewport = viewport,
                    natural = ROW_MIN + LINE * linesFor(values.map { it.label }),
                    count = (values.size + 1) / 2,
                    gap = GAP,
                )
            } else {
                heightToFill(
                    viewport = viewport,
                    natural = if (glossed) 96.dp else 48.dp,
                    count = values.size,
                    gap = GAP,
                )
            }

            // The next question starts at its first row, not wherever the last one was left.
            val listState = rememberLazyListState()
            LaunchedEffect(questionId) { listState.scrollToItem(0) }
            PagedColumn(
                Modifier.fillMaxSize().padding(top = 8.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (drawn) {
                    choiceGrid(
                        characterId = questionId,
                        values = values,
                        picked = picked,
                        rowHeight = unit,
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
                    items(values, key = { it.id }) { value ->
                        val selected = value.id in picked
                        // The gloss where there is one. Colour is the character with no
                        // drawings and the most words that need explaining, and these two
                        // facts are the same fact.
                        val face: @Composable () -> Unit = {
                            if (value.gloss == null) {
                                TextMMD(value.label)
                            } else {
                                Column {
                                    TextMMD(value.label)
                                    TextMMD(
                                        value.gloss!!,
                                        style = MaterialThemeTypography().bodySmall,
                                    )
                                }
                            }
                        }
                        val shape = Modifier.fillMaxWidth()
                            .then(unit?.let { Modifier.height(it) } ?: Modifier)
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
                    //
                    // The *row* keeps a choice's height, because that is what makes the list
                    // one uniform column so any position it rests at is the top of something.
                    // The button inside it does not. Given the row's full height it became a
                    // slab across both columns, half again the size of any real choice and
                    // the largest thing on the page — which is backwards: it is the answer
                    // for when there is nothing to see, and it was shouting.
                    Box(
                        Modifier.fillMaxWidth().then(unit?.let { Modifier.height(it) } ?: Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        OutlinedButtonMMD(
                            onClick = { vm.skip(questionId) },
                            modifier = Modifier.fillMaxWidth()
                                .then(unit?.let { Modifier.height(it.coerceAtMost(SKIP_MAX)) }
                                    ?: Modifier),
                        ) {
                            TextMMD(if (character.notTested) stringResource(R.string.entry_new_cannot_say) else stringResource(R.string.entry_new_skip))
                        }
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
}

@Composable
private fun NothingLeftToAsk(
    draft: JournalViewModel.Draft,
    ranking: KeyEngine.Ranking,
    schema: CharacterSchema,
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
        TextMMD(stringResource(R.string.entry_new_nothing_more), fontWeight = FontWeight.Bold)
        TextMMD(
            if (live.isEmpty()) {
                stringResource(R.string.entry_new_nothing_fits_done)
            } else {
                stringResource(R.string.entry_new_all_answered)
            },
            style = MaterialThemeTypography().bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        shortlist.forEach {
            TextMMD("· ${it.taxon.scientificName}", modifier = Modifier.padding(top = 6.dp))
            it.uncheckedNote(schema, LocalContext.current.resources)?.let { note ->
                TextMMD(note, style = MaterialThemeTypography().bodySmall)
            }
        }
        ButtonMMD(onClick = onAddPhoto, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            TextMMD(stringResource(R.string.entry_new_add_photograph))
        }
        OutlinedButtonMMD(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            TextMMD(stringResource(R.string.entry_new_done))
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
    // No rule above the footer (STYLE.md: space, not lines), and it is kept short: it used
    // to take a divider, a count, two names with their notes and the lethal line, which left
    // the question room for one option per page (Mudita, 25 Oct). The count and the single
    // closest name stay; the full list is one tap away on the entry screen.
    if (draft.answers.answeredCount == 0) {
        TextMMD(
            stringResource(R.string.entry_new_start_narrowing),
            style = MaterialThemeTypography().bodySmall,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    } else {
        val live = ranking.live
        // The ones that still fit, so that "closest" means closest among those counted.
        // Showing the top by score could put a contradicted taxon under "of 2 still
        // possible", because the ranking sorts on score alone and a mismatch is -2.0
        // against a full match's +1.0.
        val shown = ranking.shortlist(1)
        if (live.isEmpty()) {
            // "of 0 still possible" printed above two named candidates is what the screen
            // said whenever the answers contradicted each other, and they often do: a
            // person is looking at a real mushroom and the pack describes a typical one.
            TextMMD(
                stringResource(R.string.entry_new_nothing_fits_nearest),
                style = MaterialThemeTypography().bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        shown.forEach { c ->
            val name = c.taxon.commonName?.let { n -> "${c.taxon.scientificName} — $n" }
                ?: c.taxon.scientificName
            TextMMD(
                // The count and the closest name share one line: two lines of footer left
                // the question room for one option on the colour page.
                if (live.isEmpty()) name else stringResource(R.string.entry_new_closest_of, live.size, name),
                style = MaterialThemeTypography().bodySmall,
                // Bold where it can kill. A lethal taxon that is also the closest match is
                // marked where it stands rather than named twice.
                fontWeight = if (c.taxon.hazard.severity == Hazard.Severity.LETHAL) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCandidate(c.taxon.id) }
                    .padding(vertical = 2.dp),
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
            TextMMD(
                stringResource(R.string.entry_new_not_ruled_out, hazards.joinToString(", ") { it.taxon.scientificName }),
                style = MaterialThemeTypography().bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 0.dp),
            )
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButtonMMD(onClick = onAddPhoto, modifier = Modifier.weight(1f)) {
            TextMMD(if (draft.photos.isEmpty()) stringResource(R.string.entry_new_photo) else stringResource(R.string.entry_new_photo_count, draft.photos.size))
        }
        if (onNext != null) {
            // Answering the question in front of you beats ending the whole entry, so
            // while there is something chosen this is what the solid button does.
            ButtonMMD(onClick = onNext, modifier = Modifier.weight(1f)) { TextMMD(stringResource(R.string.entry_new_next)) }
        } else {
            ButtonMMD(onClick = onDone, modifier = Modifier.weight(1f)) { TextMMD(stringResource(R.string.entry_new_done)) }
        }
    }
}

@Composable
private fun MaterialThemeTypography() = androidx.compose.material3.MaterialTheme.typography
