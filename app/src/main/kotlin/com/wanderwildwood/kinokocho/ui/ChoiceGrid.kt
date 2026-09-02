package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wanderwildwood.kinokocho.schema.CharacterValue

/**
 * The states of a character, as pictures where there are pictures.
 *
 * A drawing of a gill running down a stem answers "what does decurrent mean" in the
 * time it takes to look, and a reader holding a mushroom in one hand has no attention
 * to spend on a glossary. Where a character has no drawings yet the labels stand on
 * their own, which is why this falls back rather than insisting.
 *
 * Two per row, not three: at three the drawing is 44dp on this panel and the volva
 * stops being legible, which is the one drawing that must never stop being legible.
 */
fun LazyListScope.choiceGrid(
    characterId: String,
    values: List<CharacterValue>,
    picked: Set<String>,
    /**
     * The height each row must take to divide the page, or null to keep its own.
     * Worked out by the caller, which needs the same number to pad the end of the list.
     */
    rowHeight: Dp?,
    onPick: (String) -> Unit,
) {
    /*
     * Every tile the same size, not each one shrunk to its own words.
     *
     * The labels are wildly uneven — "Gilled, with a stem" against "Spores formed inside
     * (puffball, earthstar, stinkhorn)" — so tiles sized to their contents came out a
     * ragged patchwork, one box half the height of the one beside it. A grid of choices
     * should read as a grid.
     *
     * Two things together do it. The row takes its tallest tile's height and both fill
     * it, which settles each pair against each other by measurement rather than by
     * guesswork. And every label reserves the same number of lines, so one row is not
     * shorter than the next either — [linesFor] works that number out from the longest
     * label in this character, so a set of short labels still makes short tiles and
     * nothing is padded for the sake of a value that is not there.
     */
    val lines = linesFor(values.map { it.label })
    val rows = values.chunked(2)

    /*
     * One list item per row of tiles, not one item for the whole grid.
     *
     * The grid used to go into the list as a single item, which took the list's own
     * paging away from it: there was nothing between the top of the grid and the bottom
     * of it for a page to stop at, so a swipe left off wherever the pixels fell and the
     * bottom row was sliced through the middle of its drawings. On a screen that turns
     * pages rather than scrolling, a half-drawn mushroom at the fold reads as damage.
     *
     * A row per item gives the list the edges it snaps to, and the fold lands between
     * tiles.
     */
    /*
     * And a whole number of rows to the page, so none is ever cut through.
     *
     * A row per item gave the list edges to snap to when it turns a page, and that alone
     * was not enough: the viewport is whatever height is left after the question and the
     * footer, which is no particular number of rows, so the first screenful still ended
     * part-way down a tile. What a reader sees is the top third of two drawings under the
     * fold, which on a panel that turns pages reads as damage rather than as "more below".
     *
     * So the rows are stretched a little to divide the viewport exactly. Only stretched:
     * where every row already fits there is nothing to divide and they keep their natural
     * height, and a character with two choices does not get one tile half a screen tall.
     */

    items(rows.size, key = { rows[it].first().id }) { i ->
        val pair = rows[i]
        Row(
            Modifier
                .fillMaxWidth()
                .then(rowHeight?.let { Modifier.height(it) } ?: Modifier.height(IntrinsicSize.Max))
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pair.forEach { value ->
                PictureChoice(
                    art = CharacterArt.of(characterId, value.id),
                    label = value.label,
                    selected = value.id in picked,
                    lines = lines,
                    onClick = { onPick(value.id) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            // Keeps a lone last choice the same width as the others rather than
            // stretching it across the row and making it look like something else.
            if (pair.size == 1) Column(Modifier.weight(1f)) {}
        }
    }
}

/**
 * How many lines of label every tile in this grid should make room for.
 *
 * Estimated rather than measured, because the number has to be the same for every tile
 * before any of them is laid out — that is the whole point of it. A tile is about 151dp
 * of text on this panel once the screen padding, the gap between the pair and the tile's
 * own padding are taken off, which is around [PER_LINE] characters of the small style.
 *
 * Erring high costs a band of white under the shortest label. Erring low costs nothing
 * at all: a label that needs another line still gets one, and that row grows while the
 * rest stay put — which is the ragged patchwork this exists to avoid, but only for the
 * one row rather than all of them.
 */
internal fun linesFor(labels: List<String>): Int {
    val longest = labels.maxOfOrNull { it.length } ?: 0
    return ((longest + PER_LINE - 1) / PER_LINE).coerceIn(1, 4)
}

/** Characters of `bodySmall` that fit across one tile on a 480px, 210dpi panel. */
private const val PER_LINE = 25

/**
 * How tall to make each of [count] choices so that a whole number of them fills
 * [viewport], or null when they all fit already and should keep their own height.
 *
 * ⚠ [gap] is the list's own spacing between items and is not optional arithmetic. Four
 * choices in a viewport do not get a quarter of it each — they get a quarter of what is
 * left after three gaps. Dividing the whole height and hoping produced a list overflowing
 * by exactly the three gaps, which is one more sliced row and looks identical to the
 * fault being fixed.
 *
 * [natural] must not be *under* what a choice really needs either, or the division comes
 * out too many and the content is squeezed into less room than it takes — the same
 * clipping, moved inside the box instead of under the fold.
 */
internal fun heightToFill(viewport: Dp, natural: Dp, count: Int, gap: Dp): Dp? {
    var fits = count
    while (fits > 1 && natural * fits + gap * (fits - 1) > viewport) fits--
    return if (count <= fits) null else (viewport - gap * (fits - 1)) / fits
}

/** A tile with no words at all: the drawing, its padding, and the gap under the row. */
internal val ROW_MIN = 96.dp

/** One line of `bodySmall`, near enough to divide a viewport by. */
internal val LINE = 16.dp

/** What the question list puts between two items. Must match `verticalArrangement`. */
internal val GAP = 6.dp

/**
 * As tall as the skip button is allowed to get.
 *
 * Its row still takes a whole choice's height, because uniform rows are what let the list
 * rest at a page boundary instead of halfway through a tile. The button in that row does
 * not need to fill it, and filling it made "Skip this" a slab across both columns and the
 * largest thing on the page. It is the answer for when there is nothing to see; it should
 * not be the loudest thing on the screen. On the text questions a row is already about
 * this tall, so nothing changes there.
 */
internal val SKIP_MAX = 56.dp

/**
 * One choice: the drawing, and the words under it.
 *
 * The words stay even when there is a picture. A drawing narrows what a term could
 * mean; it does not say the term, and the reader needs the term to write it down or
 * to ask someone about it later.
 *
 * Chosen is shown by a heavy border, per the house rule that state is carried by the
 * border rather than by fill — a filled tile would swallow line art drawn in the same
 * ink.
 */
@Composable
private fun PictureChoice(
    art: Art?,
    label: String,
    selected: Boolean,
    lines: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .border(
                BorderStroke(if (selected) 3.dp else 1.dp, MaterialTheme.colorScheme.onSurface),
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (art != null) {
            Icon(
                painter = painterResource(art.drawable),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(64.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            // The same room in every tile, so the rows line up. Only a floor: a label
            // that needs more still gets more rather than being cut off, because the
            // word is what the reader writes down afterwards.
            minLines = lines,
            modifier = Modifier.padding(top = if (art != null) 4.dp else 0.dp),
        )
    }
}
