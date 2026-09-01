package com.wanderwildwood.kinokocho.net

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.TimeZone

/**
 * What is actually sent, and what is actually understood.
 *
 * Robolectric because `org.json` is an Android class and the JVM stub throws. Nothing
 * here touches a network.
 */
@RunWith(RobolectricTestRunner::class)
class INatPayloadTest {

    private fun body(
        uuid: String = "AAAA1111-2222-3333-4444-555555555555",
        latitude: Double? = 35.89,
        longitude: Double? = -82.83,
        geoprivacy: String? = "obscured",
        placeNote: String = "the big oak below the spring",
    ) = JSONObject(
        INatPayload.observation(
            uuid = uuid,
            recordedAt = 1_756_000_000_000L,
            description = "what I could see",
            placeNote = placeNote,
            latitude = latitude,
            longitude = longitude,
            geoprivacy = geoprivacy,
            timeZone = TimeZone.getTimeZone("America/New_York"),
        )
    ).getJSONObject("observation")

    /**
     * The one that makes a retry safe.
     *
     * iNaturalist's controller looks for an existing observation by uuid and then
     * retries the same lookup downcased. Send upper-case hex and the retry after a lost
     * connection matches nothing, and the mushroom is posted a second time — which is
     * the exact failure the uuid exists to prevent, and which nobody would see until it
     * happened in a wood.
     */
    @Test
    fun `the uuid goes up lower-cased`() {
        assertEquals("aaaa1111-2222-3333-4444-555555555555", body().getString("uuid"))
    }

    /**
     * An observation with no taxon lands in iNaturalist's "Unknown" pile, which
     * identifiers filter past. Kingdom is true, costs nothing, and is what puts a
     * mushroom in front of somebody who identifies mushrooms.
     */
    @Test
    fun `it is posted at kingdom Fungi and never lower`() {
        assertEquals(47170, body().getInt("taxon_id"))
    }

    /** The app narrows and does not decide. A species guess would be it deciding. */
    @Test
    fun `no species is guessed`() {
        assertTrue(body().isNull("species_guess"))
    }

    @Test
    fun `geoprivacy is sent when it is set and absent when it is not`() {
        assertEquals("obscured", body().getString("geoprivacy"))
        // Their model's list is [obscured, private]; a third value would be silently
        // dropped to null anyway, so saying nothing says the same thing clearly.
        assertFalse(body(geoprivacy = null).has("geoprivacy"))
    }

    /**
     * The journal rounds coordinates to two decimal places before storing them. Sending
     * them without saying so would have iNaturalist draw a pin implying a precision this
     * app never had.
     */
    @Test
    fun `coordinates carry the accuracy the journal actually has`() {
        assertEquals(1200, body().getInt("positional_accuracy"))
        val none = body(latitude = null, longitude = null)
        assertFalse(none.has("latitude"))
        assertFalse(none.has("positional_accuracy"))
    }

    @Test
    fun `an empty place note is left out rather than sent blank`() {
        assertFalse(body(placeNote = "").has("place_guess"))
    }

    /**
     * iNaturalist parses this with Chronic, which reads a great many shapes and is
     * therefore an excellent way to be misunderstood. An explicit offset is the one
     * shape that cannot be read as another date.
     *
     * The instant chosen here is the argument for all of it: it is the 24th of August
     * in UTC and the 23rd in the wood, because it is a quarter to ten at night. A
     * mushroom found after dark is filed on the wrong day by anything that sends a bare
     * date or leaves the zone to be guessed at the far end — and it would be filed on
     * the wrong day quietly, in a record whose whole value is when and where.
     */
    @Test
    fun `the date is the date in the wood, not the date in UTC`() {
        val observed = body().getString("observed_on_string")
        assertTrue(observed, Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[-+]\d{2}:\d{2}$""").matches(observed))
        assertEquals("2025-08-23T21:46:40-04:00", observed)
        assertEquals("America/New_York", body().getString("time_zone"))
    }

    /** The v1 API answers a create with a one-element array, not an object. */
    @Test
    fun `a create response is read whether it is an array or an object`() {
        val expected = INatPayload.Created(1, "u")
        assertEquals(expected, INatPayload.created("""[{"id":1,"uuid":"u"}]"""))
        assertEquals(expected, INatPayload.created("""{"id":1,"uuid":"u"}"""))
        assertEquals(expected, INatPayload.created("""{"results":[{"id":1,"uuid":"u"}]}"""))
        assertNull(INatPayload.created("""{"errors":["no"]}"""))
        assertNull(INatPayload.created("not json at all"))
    }

    /**
     * The community's name, not the observer's own.
     *
     * This app puts kingdom Fungi on every observation it posts. Reading `taxon` back
     * would have the journal report iNaturalist's opinion as "Fungi" for ever, sourced
     * from itself — a loop that would look like the feature working.
     */
    @Test
    fun `the app's own kingdom identification is never read back as an answer`() {
        assertNull(
            INatPayload.identification(
                """{"results":[{"taxon":{"id":47170,"name":"Fungi","rank":"kingdom"}}]}"""
            )
        )
        assertNull(
            INatPayload.identification(
                """{"results":[{"community_taxon":{"id":47170,"name":"Fungi","rank":"kingdom"}}]}"""
            )
        )
        // Any other kingdom is equally uninformative.
        assertNull(
            INatPayload.identification(
                """{"results":[{"community_taxon":{"id":1,"name":"Animalia","rank":"kingdom"}}]}"""
            )
        )
    }

    @Test
    fun `a real community identification is read, with its common name`() {
        val id = INatPayload.identification(
            """{"results":[{"community_taxon":{"id":48701,"name":"Amanita bisporigera",
               "rank":"species","preferred_common_name":"Destroying Angel"}}]}"""
        )
        assertEquals(48701L, id?.taxonId)
        assertEquals("Amanita bisporigera", id?.name)
        assertEquals("Destroying Angel", id?.commonName)
    }

    @Test
    fun `nobody having said anything reads as nothing rather than as a failure`() {
        assertNull(INatPayload.identification("""{"results":[{"community_taxon":null}]}"""))
        assertNull(INatPayload.identification("""{"results":[]}"""))
    }

    /**
     * Their errors arrive in at least three shapes. An app that understands one of them
     * says "something went wrong" for the two cases where the server explained itself.
     */
    @Test
    fun `an error is read out of all three shapes iNaturalist uses`() {
        assertEquals("nope", INatPayload.errorFrom("""{"error":"nope"}"""))
        assertEquals("a; b", INatPayload.errorFrom("""{"errors":["a","b"]}"""))
        assertEquals(
            "observed_on is not a date",
            INatPayload.errorFrom("""{"errors":{"observed_on":["is not a date"]}}"""),
        )
        assertNull(INatPayload.errorFrom("""{"ok":true}"""))
        assertNull(INatPayload.errorFrom("<html>502 Bad Gateway</html>"))
    }

    @Test
    fun `a photo id is read from either shape`() {
        assertEquals(9L, INatPayload.photoId("""{"photo":{"id":9}}"""))
        assertEquals(9L, INatPayload.photoId("""{"photo_id":9}"""))
        assertNull(INatPayload.photoId("""{"photo":{}}"""))
    }
}
