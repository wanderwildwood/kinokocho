package com.wanderwildwood.kinokocho.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.key.KeyEngine
import com.wanderwildwood.kinokocho.schema.Character
import com.wanderwildwood.kinokocho.schema.CharacterSchema
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hands a find to a person.
 *
 * This is the app's stated purpose rather than a nicety: it records an observation so
 * that someone who can identify it has something worth looking at. Until now there was
 * no way to get an entry out, which made "helps you ask someone" a claim the app could
 * not honour.
 *
 * It goes out as plain text plus the photographs, through the share sheet — so it can
 * land in a message, an email, a forum post, whatever the person on the other end
 * uses. No account, no upload, no permission: the FileProvider already declared for
 * the camera grants read on each photo for the life of the intent.
 */
object ShareFind {

    fun intent(
        context: Context,
        schema: CharacterSchema,
        engine: KeyEngine,
        draft: JournalViewModel.Draft,
    ): Intent {
        val photos = draft.photos.mapNotNull { photo ->
            val file = File(photoDir(context), photo.fileName)
            if (file.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
            } else {
                null
            }
        }

        val text = summary(schema, engine, draft)

        // SEND_MULTIPLE with no photos shows an empty chooser on some launchers, so a
        // find without pictures goes out as plain text instead.
        val action = if (photos.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
        return Intent(action).apply {
            when {
                photos.size > 1 -> {
                    type = "image/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(photos))
                }
                photos.size == 1 -> {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, photos.first() as Uri)
                }
                else -> type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "A mushroom, ${dateOf(draft.recordedAt)}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * The find in words.
     *
     * Written for a person who was not there. The characters are given as the question
     * and the answer rather than as ids, the shortlist is offered as *not ruled out*
     * and never as a claim, and what was not recorded is included — because the first
     * thing anyone experienced will ask is whether the base came up, and saying so
     * saves a round trip.
     */
    fun summary(
        schema: CharacterSchema,
        engine: KeyEngine,
        draft: JournalViewModel.Draft,
    ): String = buildString {
        appendLine("A mushroom, ${dateOf(draft.recordedAt)}")
        if (draft.placeNote.isNotBlank()) appendLine(draft.placeNote)
        // What the reader has already been told it is, if anything. Whoever is being
        // asked should know somebody has answered before them.
        if (draft.identifiedAs.isNotBlank()) {
            appendLine("Recorded as: ${draft.identifiedAs}")
        }
        appendLine()

        appendLine("What I could see")
        // Named rather than asked. Every line used to open with the whole question the
        // key would have put — "What is the base of the stem like? A bag-like sac around
        // the base" — which is the same prose problem the candidate page had, in a
        // message somebody has to read on a phone before they can help.
        draft.answers.values.forEach { (characterId, chosen) ->
            val character = schema.character(characterId) ?: return@forEach
            val labels = chosen.mapNotNull { c ->
                schema.valuesOf(character).firstOrNull { it.id == c }?.label
            }
            if (labels.isNotEmpty()) {
                appendLine("  ${character.inFull().replaceFirstChar { c -> c.uppercase() }}: ${labels.joinToString(", ")}")
            }
        }

        // Size goes with what was seen rather than in a section of its own. It is one
        // of the first things anyone asks and it costs a line.
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
                schema.character(id)?.let { appendLine("  ${it.inFull().replaceFirstChar { c -> c.uppercase() }}") }
            }
        }

        val missing = engine.mostValuableMissing(draft.answers).take(3)
        if (missing.isNotEmpty()) {
            appendLine()
            appendLine("Not recorded")
            missing.forEach { (id, _) ->
                schema.character(id)?.let { appendLine("  ${it.inFull().replaceFirstChar { c -> c.uppercase() }}") }
            }
        }

        val ranking = engine.rank(draft.answers)
        val live = ranking.candidates.filter { it.mismatched == 0 }
        if (draft.answers.answeredCount > 0 && live.isNotEmpty()) {
            appendLine()
            appendLine("Not ruled out — ${live.size} of ${ranking.candidates.size}")
            live.take(6).forEach { c ->
                appendLine(
                    "  " + (c.taxon.commonName?.let { "${c.taxon.scientificName} - $it" }
                        ?: c.taxon.scientificName)
                )
            }
        }

        appendLine()
        appendLine(
            "Recorded with Mushroom Journal, which narrows and does not decide. " +
                "Nothing here is an identification."
        )
    }

    private fun dateOf(millis: Long): String =
        SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))
}
