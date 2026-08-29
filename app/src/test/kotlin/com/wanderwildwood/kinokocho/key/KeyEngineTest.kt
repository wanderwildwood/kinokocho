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

    @Test
    fun `nothing dangerous is left with no confusion recorded`() {
        // An audit found nine of them, the false chanterelle among them, while all four
        // chanterelles also had an empty list — which is the one confusion a chanterelle
        // hunter here actually makes. It cost two things: the page a reader checks last
        // was blank, and the key can only pull a settling question forward when it has a
        // hand-declared discriminator to pull.
        val named = pack.taxa.flatMap { taxon ->
            taxon.lookalikes.flatMap { listOf(taxon.id, it.taxon) }
        }.toSet()
        val orphaned = pack.taxa
            .filter {
                it.hazard.severity == Hazard.Severity.LETHAL ||
                    it.hazard.severity == Hazard.Severity.SEVERE ||
                    it.hazard.severity == Hazard.Severity.GI
            }
            .map { it.id }
            .filterNot { it in named }
        assertTrue("nothing is confused with $orphaned", orphaned.isEmpty())
    }

    @Test
    fun `every discriminator names a character the schema has`() {
        // A typo here is silent: the key looks the character up, gets nothing, and the
        // settling boost quietly does not happen for that pair.
        val ids = schema.characters.map { it.id }.toSet()
        pack.taxa.flatMap { it.lookalikes }.flatMap { it.discriminators }.distinct()
            .forEach { assertTrue("no such character: $it", it in ids) }
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
        // Bondarzewia records no odour at all. Answering odour must not push it down,
        // or the best-documented taxa would rank worst.
        //
        // It used to assert the score was left exactly where it was, which is the same
        // idea stated too weakly: no penalty, but no credit either, and since the score
        // is a sum that quietly meant a row documented on eight characters could never
        // out-rank one documented on thirty that agreed no better. The pack runs from
        // eight to thirty. What silence earns now is what this row earns where it does
        // speak, damped — so it is neither punished nor rewarded for how much of it has
        // been written down.
        // The taxon is found rather than named: this test used to point at Bondarzewia,
        // which then had its odour written down, and the test went on asserting
        // something about a row that no longer said nothing.
        val before = answers("fruitbody_type" to "polypore", "substrate" to "wood")
        val silent = engine.rank(before).candidates
            .first { it.mismatched == 0 && "odour" !in it.taxon.characters }
            .taxon.id
        val after = before.with("odour", setOf("almond"))
        val b = engine.rank(before).candidates.first { it.taxon.id == silent }
        val a = engine.rank(after).candidates.first { it.taxon.id == silent }
        assertTrue("saying nothing cost $silent score", a.score >= b.score - 1e-9)
        assertEquals(1, a.unscored)
    }

    @Test
    fun `standing in for silence is damped, so one lucky match proves nothing`() {
        // Silence is scored at what the row scores where it speaks, which without
        // damping would let a taxon that records one character and happens to match it
        // claim a perfect record across the whole schema.
        val a = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "soil",
            "stipe_base" to "sac_volva",
            "ring" to "skirt",
            "hymenophore_colour" to "white",
            "spore_print" to "pale",
        )
        val ranked = engine.rank(a).candidates
        val leader = ranked.first()
        // Whatever leads, it leads on characters actually recorded rather than on a
        // handful of matches stretched over everything it never mentions.
        assertTrue("leader is ${leader.taxon.id}", leader.matched >= 4)
        // Standing in for silence can at most double a row's score. More than that and
        // most of what put a taxon at the top would be arithmetic over what it does not
        // say.
        ranked.forEach {
            assertTrue(
                "${it.taxon.id} scores ${it.score} off ${it.matched} matches",
                it.score <= it.matched * FULL_MATCH * 2 + 1e-9,
            )
        }
    }

    private companion object {
        const val FULL_MATCH = 1.0
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
        // With the month set, because the app always knows it and this test did not.
        // A season bonus unties every score by a fraction, which used to be enough to
        // engage the safety boost on question one and open the key with "what is the
        // flesh like?" — a question that needs a knife and a cut mushroom.
        (1..12).forEach { month ->
            val first = engine.nextQuestion(KeyEngine.Answers(month = month))
            assertTrue("in month $month it opens with $first",
                first == "fruitbody_type" || first == "substrate")
        }
        val noMonth = engine.nextQuestion(KeyEngine.Answers())
        assertTrue("got $noMonth", noMonth == "fruitbody_type" || noMonth == "substrate")
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
    fun `saying you could not tell never eliminates anything`() {
        // The base was left in the ground, or the specimen is too old to say whether
        // there was a ring. Those answers describe the reader, not the mushroom, and
        // scoring them as states mismatched every taxon that had the character
        // recorded - which dropped the hazards too, because a hazard is only carried
        // while it has no mismatches. The honest answer used to hide the destroying
        // angel; this is the test that says it must not.
        val base = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "soil")
        val before = engine.rank(base)

        val unsure = base
            .with("stipe_base", setOf("buried"))
            .with("ring", setOf("cannot_tell"))
        val after = engine.rank(unsure)

        assertEquals(
            "an uncertain answer must not eliminate a candidate",
            before.candidates.count { it.mismatched == 0 },
            after.candidates.count { it.mismatched == 0 },
        )
        assertTrue(
            "an uncertain answer must not drop a hazard",
            after.hazards.map { it.taxon.id }
                .containsAll(before.hazards.map { it.taxon.id }),
        )
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

    @Test
    fun `it never asks what a possible destroying angel tastes like`() {
        // Tasting is a real field method and the schema explains how to do it. But an
        // app that has just said it cannot rule out a destroying angel must not, on the
        // next screen, ask what it tastes like.
        var a = KeyEngine.Answers(month = 9)
        repeat(14) {
            val q = engine.nextQuestion(a) ?: return@repeat
            val lethal = engine.rank(a).hazards
                .filter { it.taxon.hazard.severity == Hazard.Severity.LETHAL }
            assertTrue(
                "asked for a taste while ${lethal.map { it.taxon.id }} was live",
                q != "taste" || lethal.isEmpty(),
            )
            val v = pack.taxon("amanita_bisporigera")!!.characters[q]?.firstOrNull()?.value
            a = if (v != null) a.with(q, setOf(v)) else a.markNotTested(q)
        }
    }

    @Test
    fun `taste comes back once nothing deadly is left`() {
        // Held back, not removed. Every guide uses it, and it is one of the better
        // characters once the dangerous ones are gone.
        val a = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "soil",
            "latex" to "present",
        )
        assertTrue(engine.rank(a).hazards.none {
            it.taxon.hazard.severity == Hazard.Severity.LETHAL
        })
        assertTrue(
            "taste should be available again",
            engine.mostValuableMissing(a).any { it.first == "taste" },
        )
    }

    // ---- an old specimen -------------------------------------------------------

    @Test
    fun `on an old specimen, a missing ring does not hide the destroying angel`() {
        // A fortnight of rain takes the ring off. Saying so used to mismatch every taxon
        // that has one — which does not merely reorder the list, it takes the taxon out
        // of the hazards, because a hazard is only carried while it has no mismatches.
        // The honest observation of an old mushroom was hiding what it was made for.
        val seen = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "soil",
            "stipe_base" to "sac_volva",
            "hymenophore_colour" to "white",
        ).with("ring", setOf("absent")).with("veil_remnants", setOf("none"))

        val fresh = engine.rank(seen)
        val old = engine.rank(seen.with("age", setOf("old")))

        assertFalse(
            "a fresh reading of no ring should still count against it",
            fresh.hazards.any { it.taxon.id == "amanita_bisporigera" },
        )
        assertTrue(
            "an old specimen with no ring must keep the destroying angel in view",
            old.hazards.any { it.taxon.id == "amanita_bisporigera" },
        )
    }

    @Test
    fun `age only forgives what time takes away`() {
        // Seeing a skirt on an old mushroom is still seeing a skirt. It is the absence
        // that rots away, so a positive answer is scored exactly as it always was.
        val base = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "soil")
        val sawARing = base.with("ring", setOf("skirt"))
        val fresh = engine.rank(sawARing).candidates
            .first { it.taxon.id == "agaricus_campestris" }
        val old = engine.rank(sawARing.with("age", setOf("old"))).candidates
            .first { it.taxon.id == "agaricus_campestris" }
        assertEquals(fresh.matched, old.matched)
        assertEquals(fresh.mismatched, old.mismatched)
    }

    @Test
    fun `a weathered cap shape is softened but not forgiven`() {
        // Widened tolerance, not a free pass: an old cap is flatter and more ragged than
        // the book says, and that is worth something less than the book being wrong.
        val base = answers("fruitbody_type" to "gilled_stemmed", "substrate" to "soil")
        fun capShapeOf(a: KeyEngine.Answers, id: String) =
            engine.rank(a).candidates.first { it.taxon.id == id }
        val wrong = base.with("cap_shape", setOf("funnel"))
        val fresh = capShapeOf(wrong, "amanita_bisporigera")
        val old = capShapeOf(wrong.with("age", setOf("old")), "amanita_bisporigera")
        assertTrue("an old cap shape should cost less", old.score > fresh.score)
        assertEquals("but it is still not a match", 0, old.matched - fresh.matched)
    }

    // ---- size ------------------------------------------------------------------

    @Test
    fun `a measured cap favours taxa whose published range covers it`() {
        val ranked = engine.rank(
            KeyEngine.Answers(measurements = mapOf("cap_width_mm" to 300))
        ).candidates
        assertTrue(
            "top is ${ranked.first().taxon.id}",
            300 in ranked.first().taxon.measurements.getValue("cap_width_mm"),
        )
        assertFalse(
            "bottom is ${ranked.last().taxon.id}",
            300 in ranked.last().taxon.measurements.getValue("cap_width_mm"),
        )
    }

    @Test
    fun `a size just outside the published range is not treated as a miss`() {
        // Published ranges are roughly the tenth to the ninetieth percentile, so a cap
        // a little under one is an ordinary mushroom, not a different species.
        val range = pack.taxon("galerina_marginata")!!.measurements.getValue("cap_width_mm")
        val slightlyUnder = KeyEngine.Answers(
            measurements = mapOf("cap_width_mm" to range.first - 2)
        )
        val inside = KeyEngine.Answers(
            measurements = mapOf("cap_width_mm" to (range.first + range.last) / 2)
        )
        fun scoreOf(a: KeyEngine.Answers) =
            engine.rank(a).candidates.first { it.taxon.id == "galerina_marginata" }.score
        assertTrue(scoreOf(slightlyUnder) > 0.0)
        assertTrue(scoreOf(inside) > scoreOf(slightlyUnder))
    }

    @Test
    fun `a wrong measurement never rules anything out`() {
        // A number guessed by eye must not be able to remove a taxon from the list, and
        // above all must not be able to take a deadly one off the screen: the hazard
        // list is only shown while a candidate has no mismatches at all.
        val a = answers(
            "fruitbody_type" to "gilled_stemmed",
            "substrate" to "soil",
            "stipe_base" to "sac_volva",
        )
        val before = engine.rank(a)
        val after = engine.rank(a.withMeasurement("cap_width_mm", 1))
        assertEquals(
            before.candidates.count { it.mismatched == 0 },
            after.candidates.count { it.mismatched == 0 },
        )
        assertTrue(
            after.hazards.map { it.taxon.id }.containsAll(before.hazards.map { it.taxon.id }),
        )
    }

    @Test
    fun `gain and split entropy are the same number, so the gain ratio was useless`() {
        // Quinlan divides gain by the entropy of the split, and it works because his
        // groups hold classes whose entropy is measured separately. Here each taxon is
        // its own class, so what remains inside a group is the log of its size - and
        // with that substitution the two quantities are algebraically identical. The
        // ratio was 1.000000 for every character in the schema, which meant the key was
        // ordered by power alone while three paragraphs of comment described something
        // adaptive. Pinned here so it cannot come back.
        pack.taxa.let { live ->
            schema.characters.forEach { c ->
                val gain = engine.informationGain(c.id, live)
                if (gain > 0.0) {
                    assertEquals(c.id, gain, engine.splitEntropy(c.id, live), 1e-9)
                }
            }
        }
    }

    @Test
    fun `answering truthfully reaches the right taxon within a few questions`() {
        // The whole key in one number. Walk every taxon in the pack, answer whatever is
        // asked the way that taxon is described, and see how long it takes to reach the
        // top of the list.
        //
        // Every one of them, not most. A taxon that cannot be reached by answering
        // truthfully is a row too thin to be found — which is what *Meripilus* was, and
        // it agreed with everything it was asked and simply had fewer places to agree.
        // A new taxon that fails this needs writing down properly, not excusing.
        var reached = 0
        val steps = mutableListOf<Int>()
        val unreachable = mutableListOf<String>()
        pack.taxa.forEach { target ->
            var a = KeyEngine.Answers(month = 9)
            for (i in 1..12) {
                val q = engine.nextQuestion(a) ?: break
                val v = target.characters[q]?.firstOrNull()?.value
                a = if (v != null) a.with(q, setOf(v)) else a.markNotTested(q)
                if (engine.rank(a).candidates.first().taxon.id == target.id) {
                    reached++
                    steps += i
                    break
                }
                if (i == 12) unreachable += target.id
            }
        }
        assertTrue("never reached first place: $unreachable", unreachable.isEmpty())
        assertEquals(pack.taxa.size, reached)
        assertTrue("median was ${steps.sorted()[steps.size / 2]}", steps.sorted()[steps.size / 2] <= 6)
    }

    @Test
    fun `size is never offered as a question`() {
        // Not an oversight. A measurement cannot leave one taxon standing, so it would
        // lose to every state character for ever; it is recorded on the entry screen
        // with the place and the photographs instead of pretending to be a question.
        var a = KeyEngine.Answers()
        val asked = mutableListOf<String>()
        repeat(20) {
            val q = engine.nextQuestion(a) ?: return@repeat
            asked += q
            val v = pack.taxa.first().characters[q]?.firstOrNull()?.value
            a = if (v != null) a.with(q, setOf(v)) else a.markNotTested(q)
        }
        assertFalse("size was asked: $asked", asked.contains("size"))
        assertFalse(engine.mostValuableMissing(KeyEngine.Answers()).any { it.first == "size" })
    }
}
