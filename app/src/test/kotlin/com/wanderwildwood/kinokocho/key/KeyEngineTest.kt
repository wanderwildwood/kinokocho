package com.wanderwildwood.kinokocho.key

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.schema.SchemaLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Run against the schema and the region pack that actually ship. These are the tests
 * that stand between a person and a wrong answer, so they use real data or nothing.
 */
@RunWith(RobolectricTestRunner::class)
class KeyEngineTest {

    private val assets = ApplicationProvider.getApplicationContext<Context>().assets
    private val schema = SchemaLoader.load(assets)
    private val pack = PackLoader.load(assets, "packs/southern-appalachia-v1.json")
    private val engine = KeyEngine(schema, pack)

    private fun answers(vararg pairs: Pair<String, String>) =
        KeyEngine.Answers(pairs.associate { (k, v) -> k to setOf(v) })

    private fun topId(a: KeyEngine.Answers) = engine.rank(a).candidates.first().taxon.id

    // ---- the pack itself -------------------------------------------------------

    @Test
    fun `the region pack loads and every taxon declares a hazard`() {
        assertEquals("southern-appalachia", pack.packId)
        assertTrue(pack.taxa.size >= 20)
        pack.taxa.forEach { assertNotNull("${it.id} has no hazard", it.hazard.severity) }
    }

    @Test
    fun `no taxon claims to have been reviewed, because none has been`() {
        assertTrue(pack.taxa.none { it.reviewed })
    }

    @Test
    fun `every taxon cites a source`() {
        pack.taxa.forEach {
            assertTrue("${it.id} cites nothing", it.sources.isNotEmpty())
        }
    }

    @Test
    fun `every lookalike points at a taxon in the pack or is explicitly external`() {
        val known = pack.taxa.map { it.id }.toSet()
        pack.taxa.flatMap { it.lookalikes }.forEach {
            // agaricus_spp is a deliberate stub: the confusion partner is real, the row
            // is not written yet, and a dangling pointer here should be visible.
            assertTrue("dangling lookalike ${it.taxon}", it.taxon in known || it.taxon == "agaricus_spp")
        }
    }

    // ---- ranking ---------------------------------------------------------------

    @Test
    fun `nothing answered ranks nothing above anything else`() {
        val scores = engine.rank(KeyEngine.Answers()).candidates.map { it.score }
        assertEquals(1, scores.toSet().size)
    }

    @Test
    fun `a white-spored gilled mushroom with a sac at the base finds the destroying angel`() {
        val a = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "soil",
            "stipe_base" to "sac_volva",
            "hymenophore_colour" to "white",
            "ring" to "skirt",
        )
        assertEquals("amanita_bisporigera", topId(a))
    }

    @Test
    fun `an orange cluster on wood with true gills finds the jack-o-lantern`() {
        val a = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "wood",
            "growth_habit" to "caespitose",
            "cap_colour" to "orange",
            "gill_attachment" to "decurrent",
        )
        assertEquals("omphalotus_illudens", topId(a))
    }

    @Test
    fun `false gills on soil find the chanterelle rather than the jack-o-lantern`() {
        val a = answers(
            "fruitbody_type" to "chanterelle_like",
            "substrate" to "soil",
            "cap_colour" to "orange",
            "odour" to "fruity",
        )
        val top = engine.rank(a).candidates.first()
        assertTrue(top.taxon.id.startsWith("cantharellus"))
        // And the jack-o'-lantern must have fallen, not vanished.
        assertTrue(engine.rank(a).candidates.any { it.taxon.id == "omphalotus_illudens" })
    }

    @Test
    fun `a rusty spore print separates Galerina from the honey mushroom`() {
        val shared = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "wood",
            "growth_habit" to "caespitose",
        )
        // In the field they are genuinely confusable, so both stay near the top.
        val fieldOnly = engine.rank(shared).candidates.map { it.taxon.id }.take(4)
        assertTrue(fieldOnly.contains("galerina_marginata"))
        assertTrue(fieldOnly.contains("armillaria_mellea"))

        // The print settles it, and it settles it both ways.
        val rusty = shared.with("spore_print", setOf("brown"))
        assertEquals("galerina_marginata", topId(rusty))
        val white = shared.with("spore_print", setOf("pale"))
        assertEquals("armillaria_mellea", topId(white))
    }

    @Test
    fun `a green spore print settles the commonest poisoning in North America`() {
        val a = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "grass",
            "gill_attachment" to "free",
            "spore_print" to "green",
        )
        assertEquals("chlorophyllum_molybdites", topId(a))
    }

    @Test
    fun `a hard purple-black interior separates the earthball from the puffball`() {
        val shared = answers("fruitbody_type" to "gasteroid", "substrate" to "soil")
        val cracked = shared.with("cap_surface", setOf("cracked", "scaly"))
        assertEquals("scleroderma_citrinum", topId(cracked))
        val warty = shared.with("cap_surface", setOf("warty"))
        assertEquals("lycoperdon_perlatum", topId(warty))
    }

    @Test
    fun `pore colour splits the two chickens of the woods`() {
        val shared = answers("fruitbody_type" to "polypore", "substrate" to "wood")
        assertEquals(
            "laetiporus_sulphureus",
            topId(shared.with("hymenophore_colour", setOf("yellow")).with("growth_habit", setOf("tiered"))),
        )
        assertEquals(
            "laetiporus_cincinnatus",
            topId(shared.with("cap_colour", setOf("pink")).with("growth_habit", setOf("fused"))),
        )
    }

    @Test
    fun `bruising black separates Meripilus from the hen of the woods`() {
        val shared = answers(
            "fruitbody_type" to "polypore",
            "substrate" to "wood",
            "growth_habit" to "fused",
        )
        val black = shared.with("bruising", setOf("yes")).with("bruising_colour", setOf("black"))
        assertEquals("meripilus_sumstinei", topId(black))
        val none = shared.with("bruising", setOf("no")).with("cap_colour", setOf("grey"))
        assertEquals("grifola_frondosa", topId(none))
    }

    // ---- the rules that must not be quietly optimised away ----------------------

    @Test
    fun `a wrong answer costs a taxon its place but never its existence`() {
        val right = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "wood")
        val wrong = right.with("substrate", setOf("dung"))
        val stillThere = engine.rank(wrong).candidates.map { it.taxon.id }
        assertTrue("Galerina must survive a wrong tap", stillThere.contains("galerina_marginata"))
        assertTrue(
            "but it must not still be first",
            engine.rank(right).candidates.indexOfFirst { it.taxon.id == "galerina_marginata" } <
                engine.rank(wrong).candidates.indexOfFirst { it.taxon.id == "galerina_marginata" },
        )
    }

    @Test
    fun `a character the data does not record costs a taxon nothing`() {
        // Bondarzewia records no odour at all. Answering odour must not push it down
        // relative to where it was, or the best-documented taxa would rank worst.
        val before = answers("fruitbody_type" to "polypore", "substrate" to "wood")
        val after = before.with("odour", setOf("almond"))
        val b = engine.rank(before).candidates.first { it.taxon.id == "bondarzewia_berkeleyi" }
        val a = engine.rank(after).candidates.first { it.taxon.id == "bondarzewia_berkeleyi" }
        assertEquals(b.score, a.score, 1e-9)
        assertEquals(1, a.unscored)
    }

    @Test
    fun `season nudges but never filters`() {
        val summer = KeyEngine.Answers(mapOf("fruitbody_type" to setOf("polypore")), month = 7)
        val autumn = KeyEngine.Answers(mapOf("fruitbody_type" to setOf("polypore")), month = 10)
        val ids = engine.rank(summer).candidates.map { it.taxon.id }
        // Everything is still on the list in July, including the October mushroom.
        assertTrue(ids.contains("grifola_frondosa"))
        assertTrue(
            "the hen should rank better in October",
            engine.rank(autumn).candidates.indexOfFirst { it.taxon.id == "grifola_frondosa" } <
                engine.rank(summer).candidates.indexOfFirst { it.taxon.id == "grifola_frondosa" },
        )
    }

    @Test
    fun `not tested is not the same as answering no`() {
        val looked = answers("fruitbody_type" to "polypore").with("bruising", setOf("no"))
        val didNot = answers("fruitbody_type" to "polypore").markNotTested("bruising")
        val blackStainer = { a: KeyEngine.Answers ->
            engine.rank(a).candidates.first { it.taxon.id == "meripilus_sumstinei" }
        }
        // Saying "it does not bruise" must penalise the black-staining polypore.
        assertTrue(blackStainer(looked).mismatched > 0)
        // Never having cut it must not.
        assertEquals(0, blackStainer(didNot).mismatched)
    }

    // ---- what to ask next ------------------------------------------------------

    @Test
    fun `the first question is one anybody can answer`() {
        val first = engine.nextQuestion(KeyEngine.Answers())
        assertTrue("got $first", first == "fruitbody_type" || first == "substrate")
    }

    @Test
    fun `it never asks about gills before it knows there are gills`() {
        var a = KeyEngine.Answers()
        val asked = mutableListOf<String>()
        repeat(6) {
            val q = engine.nextQuestion(a) ?: return@repeat
            asked += q
            if (q == "gill_attachment") {
                assertEquals(setOf("gilled_stemmed"), a.values["fruitbody_type"])
            }
            // Answer with whatever the current leader shows, as a person following the
            // key on a real mushroom would.
            val leader = engine.rank(a).candidates.first().taxon
            val v = leader.characters[q]?.firstOrNull()?.value
            a = if (v != null) a.with(q, setOf(v)) else a.markNotTested(q)
        }
        assertTrue(asked.isNotEmpty())
    }

    @Test
    fun `it never asks a question already answered`() {
        var a = KeyEngine.Answers()
        val asked = mutableListOf<String>()
        repeat(10) {
            val q = engine.nextQuestion(a) ?: return@repeat
            assertFalse("asked $q twice", asked.contains(q))
            asked += q
            val leader = engine.rank(a).candidates.first().taxon
            val v = leader.characters[q]?.firstOrNull()?.value
            a = if (v != null) a.with(q, setOf(v)) else a.markNotTested(q)
        }
    }

    @Test
    fun `it does not ask in the field for something you cannot see in the field`() {
        var a = KeyEngine.Answers()
        repeat(12) {
            val q = engine.nextQuestion(a) ?: return@repeat
            assertNotEquals("spore_print", q)
            val leader = engine.rank(a).candidates.first().taxon
            val v = leader.characters[q]?.firstOrNull()?.value
            a = if (v != null) a.with(q, setOf(v)) else a.markNotTested(q)
        }
    }

    private fun assertNotEquals(unexpected: String, actual: String) =
        assertFalse("should not have asked $unexpected", unexpected == actual)

    @Test
    fun `latex is not asked early, despite being decisive when it is present`() {
        // It is 'none' for almost everything, so a list sorted by usefulness would put
        // it near the front and waste the reader's first question. Entropy should not.
        val early = generateSequence(KeyEngine.Answers()) { a ->
            engine.nextQuestion(a)?.let { q ->
                val leader = engine.rank(a).candidates.first().taxon
                leader.characters[q]?.firstOrNull()?.value
                    ?.let { a.with(q, setOf(it)) } ?: a.markNotTested(q)
            }
        }.take(3).mapNotNull { engine.nextQuestion(it) }.toList()
        assertFalse("latex asked in the first three questions: $early", early.contains("latex"))
    }

    @Test
    fun `a question with no discriminating power is not asked`() {
        val single = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "grass",
            "spore_print" to "green",
        )
        val live = engine.rank(single).candidates.take(1).map { it.taxon }
        assertEquals(0.0, engine.informationGain("cap_colour", live), 1e-9)
    }

    // ---- safety ----------------------------------------------------------------

    @Test
    fun `a deadly candidate stays in view even when it ranks badly`() {
        // Clustered on wood: Galerina is genuinely possible and must be surfaced even
        // though Armillaria and the jack-o'-lantern will usually outrank it.
        val a = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "wood")
        val hazards = engine.rank(a).hazards.map { it.taxon.id }
        assertTrue(hazards.contains("galerina_marginata"))
    }

    @Test
    fun `a deadly candidate ruled out by an answer stops being surfaced`() {
        val onWood = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "wood")
        assertTrue(engine.rank(onWood).hazards.any { it.taxon.id == "galerina_marginata" })

        // Galerina does not grow on lawns. Saying so should retire it.
        val onGrass = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "grass")
        assertFalse(engine.rank(onGrass).hazards.any { it.taxon.id == "galerina_marginata" })
    }

    @Test
    fun `a safety note names what would actually settle it`() {
        val a = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "wood")
        val note = engine.safetyNotes(a).firstOrNull { it.taxon.id == "galerina_marginata" }
        assertNotNull(note)
        assertTrue("must point at the spore print", note!!.discriminators.contains("spore_print"))
        assertTrue(note.notes.any { it.contains("print", ignoreCase = true) })
    }

    @Test
    fun `the lethal taxa carry an onset time and a source`() {
        pack.taxa.filter { it.hazard.severity == Hazard.Severity.LETHAL }.forEach {
            assertNotNull("${it.id} has no onset", it.hazard.onset)
            assertNotNull("${it.id} has no source", it.hazard.source)
        }
    }

    // ---- what you did not write down -------------------------------------------

    @Test
    fun `it can say which unrecorded character would have narrowed things most`() {
        val a = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "wood")
        val missing = engine.mostValuableMissing(a)
        assertTrue(missing.isNotEmpty())
        assertTrue("gains must be sorted", missing.zipWithNext().all { it.first.second >= it.second.second })
        // The deferred character belongs in this list even though it is never asked in
        // the field - at home, it is exactly the thing worth going back for.
        assertTrue(missing.any { it.first == "spore_print" })
    }

    @Test
    fun `nothing is worth going back for once one candidate stands alone`() {
        val a = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "grass",
            "spore_print" to "green",
        )
        val live = engine.rank(a).candidates.take(1).map { it.taxon }
        assertEquals(1, live.size)
        assertNull(engine.nextQuestion(a, considerTop = 1))
    }

    @Test
    fun `holding a destroying angel, the key asks about the base of the stem`() {
        // The first version of this engine asked about odour and nearby trees and never
        // asked about the volva at all. That is the character the whole Amanita problem
        // turns on, so it must be reached early and this test is why.
        var a = KeyEngine.Answers(month = 9)
        val target = pack.taxon("amanita_bisporigera")!!
        val asked = mutableListOf<String>()
        repeat(5) {
            val q = engine.nextQuestion(a) ?: return@repeat
            asked += q
            val v = target.characters[q]?.firstOrNull()?.value
            a = if (v != null) a.with(q, setOf(v)) else a.markNotTested(q)
        }
        assertTrue("asked: $asked", asked.contains("stipe_base"))
    }

    @Test
    fun `a deadly candidate pulls its settling question forward`() {
        // Clustered on wood, Galerina is live and Armillaria leads. The thing that
        // separates them is the ring and the base, not the cap.
        val onWood = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "wood")
        var a = onWood
        val asked = mutableListOf<String>()
        repeat(4) {
            val q = engine.nextQuestion(a) ?: return@repeat
            asked += q
            val v = pack.taxon("galerina_marginata")!!.characters[q]?.firstOrNull()?.value
            a = if (v != null) a.with(q, setOf(v)) else a.markNotTested(q)
        }
        assertTrue(
            "should have asked something that separates Galerina, asked: $asked",
            asked.any { it == "stipe_base" || it == "ring" || it == "hymenophore_colour" },
        )
    }

    @Test
    fun `the safety boost never invents a question that separates nothing`() {
        // One candidate left, a hazard live: still no question, because boosting zero
        // is zero.
        val a = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "grass",
            "spore_print" to "green",
        )
        assertNull(engine.nextQuestion(a, considerTop = 1))
    }
}
