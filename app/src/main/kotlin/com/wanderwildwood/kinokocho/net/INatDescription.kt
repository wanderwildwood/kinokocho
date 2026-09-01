package com.wanderwildwood.kinokocho.net

import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.key.KeyEngine
import com.wanderwildwood.kinokocho.schema.Character
import com.wanderwildwood.kinokocho.schema.CharacterSchema
import com.wanderwildwood.kinokocho.ui.inFull

/**
 * The find, written for an identifier.
 *
 * Deliberately not [com.wanderwildwood.kinokocho.ui.ShareFind.summary], which this
 * started as a copy of. The share sheet writes to a person the reader chose — a friend,
 * a forum, somebody who will read the whole thing. iNaturalist is a queue, and the
 * reader on the other end is working through it. Three differences follow:
 *
 * - **No shortlist.** The share text offers what the key has not ruled out, framed as
 *   *not ruled out*. On iNaturalist that framing does not survive contact: an
 *   identifier reads a list of names under an observation as a claim to argue with, and
 *   the app's whole position is that it narrows and does not decide. The characters are
 *   the evidence; the names are this app's opinion, and its opinion is not what the
 *   queue is short of.
 * - **What was not recorded stays.** It is the first thing anyone experienced asks —
 *   whether the base came up, whether there is a print — and answering it in advance
 *   saves a round trip that on iNaturalist can take a week.
 * - **No date and no place line.** iNaturalist has its own fields for both and shows
 *   them above the description; repeating them is furniture.
 */
object INatDescription {

    fun of(
        schema: CharacterSchema,
        engine: KeyEngine,
        draft: JournalViewModel.Draft,
    ): String = buildString {
        // What the reader was already told it is, if anything. An identifier should
        // know somebody has answered before them — and this is the reader's own note,
        // never this app's guess, which is why it is quoted rather than asserted.
        if (draft.identifiedAs.isNotBlank()) {
            appendLine("Recorded as: ${draft.identifiedAs}")
            appendLine()
        }

        appendLine("What I could see")
        draft.answers.values.forEach { (characterId, chosen) ->
            val character = schema.character(characterId) ?: return@forEach
            val labels = chosen.mapNotNull { c ->
                schema.valuesOf(character).firstOrNull { it.id == c }?.label
            }
            if (labels.isNotEmpty()) appendLine("  ${named(character)}: ${labels.joinToString(", ")}")
        }

        if (draft.answers.measurements.isNotEmpty()) {
            schema.characters
                .filter { it.kind == Character.Kind.MEASUREMENT }
                .forEach { character ->
                    schema.valuesOf(character).forEach { value ->
                        draft.answers.measurements[value.id]?.let { mm ->
                            appendLine("  ${value.label.substringBefore(" (")}: $mm mm")
                        }
                    }
                }
        }

        if (draft.answers.notTested.isNotEmpty()) {
            appendLine()
            appendLine("Looked at and could not say")
            draft.answers.notTested.forEach { id ->
                schema.character(id)?.let { appendLine("  ${named(it)}") }
            }
        }

        val missing = engine.mostValuableMissing(draft.answers).take(3)
        if (missing.isNotEmpty()) {
            appendLine()
            appendLine("Not recorded")
            missing.forEach { (id, _) ->
                schema.character(id)?.let { appendLine("  ${named(it)}") }
            }
        }

        if (draft.note.isNotBlank()) {
            appendLine()
            appendLine(draft.note)
        }

        appendLine()
        // Why this line is here rather than pared away: an identifier reading a tidy
        // list of characters needs to know whether a person answered them or a machine
        // guessed them, and that no identification is being claimed underneath.
        append(
            "Characters recorded in the field with Mushroom Journal, a notebook rather " +
                "than an identifier. No identification is claimed here."
        )
    }

    private fun named(character: Character): String =
        character.inFull().replaceFirstChar { it.uppercase() }
}
