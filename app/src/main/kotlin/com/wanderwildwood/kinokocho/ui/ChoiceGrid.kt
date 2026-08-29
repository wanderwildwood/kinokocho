package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
    values.chunked(2).forEach { pair ->
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pair.forEach { value ->
                PictureChoice(
                    art = CharacterArt.of(characterId, value.id),
                    label = value.label,
                    selected = value.id in picked,
                    onClick = { onPick(value.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            // Keeps a lone last choice the same width as the others rather than
            // stretching it across the row and making it look like something else.
            if (pair.size == 1) Column(Modifier.weight(1f)) {}
        }
    }
}

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
            modifier = Modifier.padding(top = if (art != null) 4.dp else 0.dp),
        )
    }
}
