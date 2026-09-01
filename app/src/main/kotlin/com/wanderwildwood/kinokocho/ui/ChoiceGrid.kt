package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
@Composable
fun ChoiceGrid(
    characterId: String,
    values: List<CharacterValue>,
    picked: Set<String>,
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

    values.chunked(2).forEach { pair ->
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(bottom = 8.dp),
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
private fun linesFor(labels: List<String>): Int {
    val longest = labels.maxOfOrNull { it.length } ?: 0
    return ((longest + PER_LINE - 1) / PER_LINE).coerceIn(1, 4)
}

/** Characters of `bodySmall` that fit across one tile on a 480px, 210dpi panel. */
private const val PER_LINE = 25

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
