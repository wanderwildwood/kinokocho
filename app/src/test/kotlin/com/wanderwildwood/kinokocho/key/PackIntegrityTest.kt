package com.wanderwildwood.kinokocho.key

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.schema.SchemaLoader
import com.wanderwildwood.kinokocho.ui.TaxonPlate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The pack is the one file in this app whose contents came out of a language model, and
 * a language model will produce a confident, well-formed, entirely fictional species.
 * These are the checks that a name cannot drift, be invented, or point at nothing —
 * cheap to run, and the only defence against a plausible lie in a field guide.
 */
@RunWith(RobolectricTestRunner::class)
class PackIntegrityTest {

    private val assets = ApplicationProvider.getApplicationContext<Context>().assets
    private val schema = SchemaLoader.load(assets)
    private val pack = PackLoader.load(assets, "packs/southern-appalachia-v1.json")

    /** Genus, species, and at most one infraspecific rank. Nothing else is a name. */
    private val binomial = Regex("""[A-Z][a-z]+ [a-z-]+( (var\.|subsp\.|f\.) [a-z-]+)?""")

    /**
     * Every mushroom in the pack, with the outside authority that says it exists.
     *
     * Written by `tools/verify-names.py` and checked in. See the test below for why this
     * is a file rather than a lookup.
     */
    private val verified: Map<String, String> by lazy {
        val json = javaClass.getResourceAsStream("/verified-names.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("verified-names.json is missing; run tools/verify-names.py")
        val root = org.json.JSONObject(json)
        root.keys().asSequence().associateWith { name ->
            val row = root.getJSONObject(name)
            "${row.getString("authority")}:${row.getString("id")}"
        }
    }

    /**
     * Nothing reaches the app under a name no outside authority has heard of.
     *
     * This is the failure that would matter more than any other here. The characteristic
     * mistake of the thing that wrote this file is not a typo — it is a confident,
     * well-formed, entirely plausible species that does not exist. `Suillellus
     * subvelutipes` sat in the pack for weeks: correct Latin, real genus, real epithet, a
     * combination nobody ever published. Every test above was green for it, because every
     * test above asks whether a name has the right *shape*, and a fabricated name has a
     * perfect shape. The porcini's page warned a reader about a mushroom they could not
     * have looked up.
     *
     * `tools/check-names.py` finds those, and only when somebody remembers to run it.
     * Remembering is not a control. So the answer is written down: verify-names.py asks
     * GBIF and iNaturalist and records who vouched for each name, and this refuses any
     * taxon missing from that record. A name nobody outside this repository has confirmed
     * cannot reach a reader, whether or not anyone thought to check.
     *
     * The network stays in the tool, where a flaky lookup delays a change. The rule stays
     * here, offline, where it always runs.
     */
    @Test
    fun `every name has been confirmed by someone outside this repository`() {
        val unconfirmed = pack.taxa.map { it.scientificName }.filterNot { it in verified }
        assertTrue(
            "no outside authority has vouched for these — run tools/verify-names.py, and " +
                "if it cannot find one, the mushroom does not exist and must come out: " +
                unconfirmed,
            unconfirmed.isEmpty(),
        )
    }

    /**
     * And nothing lingers in the record for a taxon the pack no longer has.
     *
     * A stale blessing is how a name creeps back in unchecked: delete a taxon, add one
     * later with the same name, and the record still says an authority vouched for it.
     */
    @Test
    fun `the record of confirmed names has nothing in it the pack does not carry`() {
        val names = pack.taxa.map { it.scientificName }.toSet()
        assertEquals("stale entries in verified-names.json", emptySet<String>(),
                     verified.keys - names)
    }

    /** The id a name must produce: lowercased, ranks dropped, spaces to underscores. */
    private fun slug(name: String) = name
        .replace(Regex("""\b(var|subsp|f)\.\s*"""), "")
        .lowercase()
        .replace(Regex("""[^a-z0-9]+"""), "_")
        .trim('_')

    @Test
    fun `every name is a well-formed binomial`() {
        pack.taxa.forEach {
            assertTrue(
                "'${it.scientificName}' is not shaped like a species name",
                binomial.matches(it.scientificName),
            )
        }
    }

    @Test
    fun `an id and its name say the same thing`() {
        // This is what catches a name quietly drifting away from the taxon it labels:
        // an id claiming a variety the displayed name has dropped, or the reverse.
        pack.taxa.forEach {
            assertEquals(
                "'${it.scientificName}' is filed under '${it.id}'",
                it.id,
                slug(it.scientificName),
            )
        }
    }

    @Test
    fun `no taxon is in the pack twice`() {
        assertEquals(pack.taxa.size, pack.taxa.map { it.id }.toSet().size)
        assertEquals(pack.taxa.size, pack.taxa.map { it.scientificName }.toSet().size)
    }

    @Test
    fun `every lookalike points at a mushroom that exists`() {
        pack.taxa.forEach { t ->
            t.lookalikes.forEach { l ->
                assertTrue(
                    "${t.id} is confused with '${l.taxon}', which is not in the pack",
                    pack.taxon(l.taxon) != null,
                )
            }
        }
    }

    @Test
    fun `every discriminator names a character the app can ask about`() {
        pack.taxa.forEach { t ->
            t.lookalikes.forEach { l ->
                l.discriminators.forEach { d ->
                    assertTrue(
                        "${t.id} vs ${l.taxon} is told apart by '$d', which is not a character",
                        schema.character(d) != null,
                    )
                }
            }
        }
    }

    @Test
    fun `every scored character and value is one the schema defines`() {
        pack.taxa.forEach { t ->
            t.characters.forEach { (id, states) ->
                val character = schema.character(id)
                assertTrue("${t.id} scores '$id', which is not a character", character != null)
                val known = schema.valuesOf(character!!).map { it.id }.toSet()
                states.forEach {
                    assertTrue(
                        "${t.id} scores $id = '${it.value}', which is not a choice",
                        it.value in known,
                    )
                }
            }
        }
    }

    @Test
    fun `every drawing belongs to a mushroom in the pack`() {
        // A plate filename is written by hand when a sheet is cut up, so it is exactly
        // the place a misspelling or an invented species would get in.
        TaxonPlate.ids.forEach {
            assertTrue("there is a drawing of '$it', which is not in the pack", pack.taxon(it) != null)
        }
    }

    @Test
    fun `no page raises an alarm it does not explain`() {
        // The candidate screen prints a heading for any severity but NONE_KNOWN. A
        // heading with nothing under it tells a person holding the mushroom that
        // something is wrong and then refuses to say what.
        //
        // The emptiness rule has to be the screen's rule, not a looser one. This test
        // first passed while eleven pages were still blank, because the pack writes an
        // em dash where an unstudied mushroom's onset would go and the screen hides
        // that row — "not blank" was true and the reader still saw nothing. UNKNOWN is
        // exempt because the screen now answers it in one standing sentence.
        fun said(s: String?) = !s.isNullOrBlank() && s.trim() != "—"
        pack.taxa
            .filter { it.hazard.severity != Hazard.Severity.NONE_KNOWN }
            .filter { it.hazard.severity != Hazard.Severity.UNKNOWN }
            .forEach {
                assertTrue(
                    "${it.id} warns and then says nothing",
                    said(it.hazard.note) || said(it.hazard.onset),
                )
                assertTrue("${it.id} warns and cites nothing", !it.hazard.source.isNullOrBlank())
            }
    }

    @Test
    fun `anything that can kill is drawn, and says so`() {
        pack.taxa.filter { it.hazard.severity == Hazard.Severity.LETHAL }.forEach {
            assertTrue("${it.id} can kill and has no drawing", TaxonPlate.of(it.id) != null)
            assertTrue("${it.id} can kill and carries no note", !it.note.isNullOrBlank())
        }
    }
    @Test
    fun `the pack does not shout`() {
        // Fourteen notes carried emphasis in capitals — "Margin NOT lined", "FALSE
        // gills", "DULL GREYISH GREEN". Sentence case everywhere is the house rule, and
        // on a panel with one ink an all-capital word is the only shout available, so it
        // gets used for whatever the writer felt strongest about rather than for what
        // matters most. The emphasis belongs in the wording.
        val shout = Regex("""\b[A-Z]{2,}\b""")
        val allowed = setOf("NAMA", "GBIF", "KOH", "DNA", "UV", "NC")
        fun check(where: String, text: String?) {
            text ?: return
            shout.findAll(text).map { it.value }.filterNot { it in allowed }.forEach {
                throw AssertionError("$where shouts '$it': $text")
            }
        }
        pack.taxa.forEach { t ->
            check("${t.id} note", t.note)
            check("${t.id} hazard", t.hazard.note)
            t.lookalikes.forEach { check("${t.id} vs ${it.taxon}", it.note) }
        }
    }

    @Test
    fun `a hyphen is not a dash`() {
        pack.taxa.forEach { t ->
            listOf(t.note, t.hazard.note).plus(t.lookalikes.map { it.note }).forEach {
                assertTrue("${t.id} uses ' - ' where a dash belongs: $it", it?.contains(" - ") != true)
            }
        }
    }
}
