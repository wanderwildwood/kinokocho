package com.wanderwildwood.kinokocho.net

import android.content.Context
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.data.JournalDao
import com.wanderwildwood.kinokocho.key.KeyEngine
import com.wanderwildwood.kinokocho.schema.CharacterSchema
import com.wanderwildwood.kinokocho.ui.photoDir
import java.io.File
import java.util.UUID

/**
 * Publishing one find, once, because somebody pressed a button.
 *
 * There is no sync here and there is not going to be one. Nothing runs in the
 * background, nothing is queued, nothing is retried on a timer, and closing the app
 * mid-push simply stops it. What makes that safe rather than careless is that every
 * step is idempotent by design — the observation and each photograph carry a uuid this
 * app minted, and iNaturalist matches on them — so "run it again" is the whole recovery
 * story, and it cannot produce a second mushroom.
 *
 * The order matters and is not arbitrary. The observation is created first and written
 * down as pushed **before** any photograph is uploaded, so that a push interrupted
 * halfway leaves a find that is on iNaturalist and short some pictures, rather than an
 * orphaned upload nobody can find again.
 */
class INatPush(
    private val context: Context,
    private val dao: JournalDao,
    private val account: INatAccount,
    private val client: INatApi = INatClient(account),
) {

    sealed interface Outcome {
        /**
         * It is up. [photosSent] of [photosTotal] pictures went with it — reported
         * rather than assumed, because a find on iNaturalist with two of its five
         * photographs is a true thing that happened and the reader should be able to
         * press the button again.
         */
        data class Ok(
            val url: String,
            val photosSent: Int,
            val photosTotal: Int,
        ) : Outcome

        /** No usable token. The screen offers to sign in rather than reporting a fault. */
        data object NeedsSignIn : Outcome

        data class Failed(val said: String) : Outcome
    }

    suspend fun push(
        schema: CharacterSchema,
        engine: KeyEngine,
        draft: JournalViewModel.Draft,
        geoprivacy: String?,
    ): Outcome {
        val observationId = draft.observationId ?: return Outcome.Failed("Nothing to send yet.")

        val jwt = when (val r = client.apiToken()) {
            is INatClient.Result.Failed -> return if (r.signedOut) Outcome.NeedsSignIn
            else Outcome.Failed(r.said)
            is INatClient.Result.Ok -> r.value
        }

        // Read the find back out of the database rather than trusting the draft. The
        // draft's photographs are built in memory as they are taken and carry id 0
        // until they are written; the rows are what have ids, uuids and a settled idea
        // of what is actually on this phone.
        val stored = dao.findOne(observationId)
            ?: return Outcome.Failed("That find is no longer in the journal.")

        val body = INatPayload.observation(
            uuid = stored.observation.uuid,
            recordedAt = stored.observation.recordedAt,
            description = INatDescription.of(schema, engine, draft),
            placeNote = stored.observation.placeNote,
            latitude = stored.observation.latitude,
            longitude = stored.observation.longitude,
            geoprivacy = geoprivacy,
        )

        val created = when (val r = client.createObservation(jwt, body)) {
            is INatClient.Result.Failed -> return Outcome.Failed(r.said)
            is INatClient.Result.Ok -> r.value
        }

        // Written down before a single picture goes up. See the note on the class.
        dao.recordPush(observationId, created.uuid, System.currentTimeMillis())

        val photos = stored.photos
        var sent = 0
        for (photo in photos) {
            val file = File(photoDir(context), photo.fileName)
            if (!file.exists()) continue

            // A photograph taken before this feature existed has no uuid. Mint one and
            // write it down *before* the upload, so that if this is the picture the
            // connection dies on, the retry matches it rather than duplicating it.
            val uuid = photo.uuid.ifBlank {
                UUID.randomUUID().toString().also { dao.setPhotoUuid(photo.id, it) }
            }

            when (val r = client.uploadPhoto(jwt, created.id, uuid, file)) {
                is INatClient.Result.Failed ->
                    // Stop at the first refusal rather than hammering through four more.
                    // Whatever turned this one down - no signal, a rate limit, a token
                    // that expired mid-push - will turn down the rest, and the find is
                    // already safely up.
                    return Outcome.Ok(urlOf(created.uuid), sent, photos.size)
                is INatClient.Result.Ok -> {
                    dao.recordPhotoPush(photo.id, r.value)
                    sent++
                }
            }
        }

        return Outcome.Ok(urlOf(created.uuid), sent, photos.size)
    }

    /**
     * Asks iNaturalist what the community has decided, for finds already published.
     *
     * Only ever for observations this app put there, and only when the reader asks.
     * Returns how many entries gained a name, which is the whole report worth giving.
     */
    suspend fun refreshIdentifications(): Int {
        val jwt = when (val r = client.apiToken()) {
            is INatClient.Result.Failed -> return 0
            is INatClient.Result.Ok -> r.value
        }
        var named = 0
        for (observation in dao.awaitingIdentification()) {
            val uuid = observation.inat.uuid ?: continue
            val body = when (val r = client.fetchObservation(jwt, uuid)) {
                is INatClient.Result.Failed -> return named
                is INatClient.Result.Ok -> r.value
            }
            INatPayload.identification(body)?.let { id ->
                dao.recordIdentification(
                    observation.id,
                    id.taxonId,
                    id.commonName?.let { "${id.name} — $it" } ?: id.name,
                    System.currentTimeMillis(),
                )
                named++
            }
        }
        return named
    }

    private fun urlOf(uuid: String) = "https://www.inaturalist.org/observations/$uuid"
}
