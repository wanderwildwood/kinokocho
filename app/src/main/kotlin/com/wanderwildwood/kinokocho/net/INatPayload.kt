package com.wanderwildwood.kinokocho.net

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * What gets sent, and what comes back, with no network in sight.
 *
 * Split out from [INatClient] so the shape of a request is a thing a test can read.
 * Every decision below is one somebody could get wrong silently — a lower-case UUID, a
 * date format, a kingdom id — and none of them announce themselves when they are wrong.
 */
object INatPayload {

    /**
     * Kingdom Fungi, and the reason an observation is pushed with a taxon at all.
     *
     * An observation posted with no taxon lands in iNaturalist's "Unknown" pile.
     * Identifiers work the queues they have filtered to, and almost all of those
     * filters start with a taxon — so an unknown mushroom posted as *unknown* is not
     * modestly waiting to be told, it is out of the room. Posting it at kingdom is
     * true, costs nothing, and is what puts it in front of somebody who identifies
     * fungi.
     *
     * Kingdom and no lower, ever. This app does not decide.
     */
    const val FUNGI = 47170

    /**
     * The body of `POST /v1/observations`.
     *
     * The field names are the Rails controller's permitted list, not a guess: see
     * `observation_params` in `observations_controller.rb`.
     *
     * ⚠ **The uuid is lower-cased.** The controller looks for an existing observation
     * by `where( uuid: ... )` and then retries `where( uuid: ...downcase )` — a comment
     * in their source says outright that this is a wart pending everything being
     * lower-case. Sending upper-case hex works, and then a retry after a lost
     * connection matches nothing and posts the mushroom a second time. Java's
     * [java.util.UUID.toString] is already lower-case; this is here so that a later
     * change of id source cannot quietly break the one property that makes a retry
     * safe.
     */
    fun observation(
        uuid: String,
        recordedAt: Long,
        description: String,
        placeNote: String,
        latitude: Double?,
        longitude: Double?,
        geoprivacy: String?,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        val observation = JSONObject()
            .put("uuid", uuid.lowercase())
            .put("species_guess", JSONObject.NULL)
            .put("taxon_id", FUNGI)
            .put("description", description)
            .put("observed_on_string", observedOn(recordedAt, timeZone))
            // Sent explicitly rather than left to the server's guess. Without it an
            // observation recorded at dusk in one place and pushed from another can
            // land on the wrong day, and a date is half of what an identifier has.
            .put("time_zone", timeZone.id)

        if (placeNote.isNotBlank()) observation.put("place_guess", placeNote)

        if (latitude != null && longitude != null) {
            observation.put("latitude", latitude)
            observation.put("longitude", longitude)
            // The journal already rounds what it stores to two decimal places, which is
            // a bit over a kilometre. Telling iNaturalist that is not a confession, it
            // is the accuracy of the record: without it their map draws a pin implying
            // a precision this app never had.
            observation.put("positional_accuracy", COARSE_ACCURACY_METRES)
        }

        // Absent rather than "open". GEOPRIVACIES in their model is [obscured, private]
        // and nothing else; the third state is the column being null, and sending the
        // string "open" sets it to null anyway by way of a validation that silently
        // drops what it does not recognise. Saying nothing is the same thing said
        // clearly.
        if (geoprivacy != null) observation.put("geoprivacy", geoprivacy)

        return JSONObject().put("observation", observation).toString()
    }

    /**
     * iNaturalist parses `observed_on_string` with Chronic, which reads a great many
     * shapes and is therefore an excellent way to be misunderstood. ISO 8601 with an
     * explicit offset is the one shape that cannot be read as anything else.
     */
    fun observedOn(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        // XXX, not ZZZZZ. In java.text.SimpleDateFormat `Z` is RFC 822 however many
        // times it is repeated - "-0400" - and only `X` gives the ISO 8601 "-04:00".
        // The two look alike enough that the wrong one reads as a typo in the right one.
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            .apply { this.timeZone = timeZone }
            .format(Date(millis))

    /** The iNaturalist id and uuid of a freshly created observation. */
    data class Created(val id: Long, val uuid: String)

    /**
     * Reads back what was created.
     *
     * The v1 API answers a create with a one-element array rather than an object, which
     * is easy to miss and produces a null that looks like a server problem.
     */
    fun created(body: String): Created? {
        val obj = firstObject(body) ?: return null
        val id = obj.optLong("id", 0).takeIf { it != 0L } ?: return null
        val uuid = obj.optString("uuid").takeIf { it.isNotBlank() } ?: return null
        return Created(id, uuid)
    }

    /** What the community currently calls it, or null while nobody has said. */
    data class Identification(val taxonId: Long, val name: String, val commonName: String?)

    /**
     * The name to write back into the journal, read from a fetched observation.
     *
     * `community_taxon` in preference to `taxon`, and the difference matters. `taxon` on
     * a fetched observation can be the observer's own identification — which here is
     * always kingdom Fungi, because this app put it there. Writing that back would have
     * the journal report iNaturalist's opinion as "Fungi" for ever, sourced from itself.
     * `community_taxon` is what other people agreed on, which is the thing that was
     * being waited for.
     *
     * Kingdom-rank answers are refused for the same reason: a community taxon that has
     * settled at Fungi is nobody having said anything yet.
     */
    fun identification(body: String): Identification? {
        val obj = firstObject(body) ?: return null
        val taxon = obj.optJSONObject("community_taxon") ?: return null
        val id = taxon.optLong("id", 0).takeIf { it != 0L } ?: return null
        if (id == FUNGI.toLong()) return null
        val rank = taxon.optString("rank")
        if (rank.equals("kingdom", ignoreCase = true)) return null
        val name = taxon.optString("name").takeIf { it.isNotBlank() } ?: return null
        val common = taxon.optString("preferred_common_name").takeIf { it.isNotBlank() }
        return Identification(id, name, common)
    }

    /** The photo id from an `observation_photos` create, for writing back to the row. */
    fun photoId(body: String): Long? =
        runCatching { JSONObject(body) }.getOrNull()
            ?.let { it.optJSONObject("photo")?.optLong("id", 0) ?: it.optLong("photo_id", 0) }
            ?.takeIf { it != 0L }

    /**
     * The error iNaturalist gave, in words a person can read, or null when it said
     * nothing useful.
     *
     * Their errors arrive in at least three shapes — a string, an array of strings, and
     * an object of field to messages — and an app that only understands one of them
     * reports "something went wrong" for the two cases where the server actually
     * explained itself.
     */
    fun errorFrom(body: String): String? {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        json.optString("error").takeIf { it.isNotBlank() }?.let { return it }
        val errors = json.opt("errors") ?: return null
        return when (errors) {
            is String -> errors.takeIf { it.isNotBlank() }
            is org.json.JSONArray -> (0 until errors.length())
                .mapNotNull { errors.optString(it).takeIf(String::isNotBlank) }
                .joinToString("; ").takeIf { it.isNotBlank() }
            is JSONObject -> errors.keys().asSequence()
                .mapNotNull { key ->
                    val value = errors.opt(key)
                    val text = when (value) {
                        is org.json.JSONArray -> (0 until value.length())
                            .mapNotNull { value.optString(it).takeIf(String::isNotBlank) }
                            .joinToString(", ")
                        else -> value?.toString().orEmpty()
                    }
                    text.takeIf { it.isNotBlank() }?.let { "$key $it" }
                }
                .joinToString("; ").takeIf { it.isNotBlank() }
            else -> null
        }
    }

    /** The API answers some calls with an object and some with a one-element array. */
    private fun firstObject(body: String): JSONObject? {
        val trimmed = body.trim()
        return runCatching {
            if (trimmed.startsWith("[")) {
                org.json.JSONArray(trimmed).optJSONObject(0)
            } else {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("results")?.optJSONObject(0) ?: obj
            }
        }.getOrNull()
    }

    /**
     * Two decimal places of latitude is a bit over a kilometre, and that is what the
     * journal stores. See PRIVACY.md.
     */
    const val COARSE_ACCURACY_METRES = 1200
}
