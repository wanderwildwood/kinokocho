package com.wanderwildwood.kinokocho.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.key.Hazard
import com.wanderwildwood.kinokocho.key.PackLoader
import com.wanderwildwood.kinokocho.key.Prevalence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeasonTest {

    private val pack = PackLoader.load(
        ApplicationProvider.getApplicationContext<Context>().assets,
        "packs/southern-appalachia-v1.json",
    )

    private fun inMonth(month: Int) =
        pack.taxa.filter { it.seasonMonths.isEmpty() || month in it.seasonMonths }

    @Test
    fun `the month view names a handful, not the pack`() {
        // September has most of the pack in it. A calendar of eighty-six things is a
        // calendar of nothing, which is what this page was.
        (1..12).forEach { month ->
            assertTrue(worthKnowing(inMonth(month)).size <= 10)
        }
    }

    @Test
    fun `a lethal taxon is never dropped for being uncommon`() {
        // The death cap is genuinely uncommon in these mountains. Filtering the top of
        // the page by prevalence must not be able to take it off.
        val month = pack.taxon("amanita_phalloides")!!.seasonMonths.first()
        assertTrue(worthKnowing(inMonth(month)).any { it.id == "amanita_phalloides" })
    }

    @Test
    fun `the ones people look for are named too, not only the ones that hurt`() {
        // The page used to hold nothing but poisons, which is a page nobody opens.
        val named = (1..12).flatMap { worthKnowing(inMonth(it)) }.toSet()
        assertTrue("nothing sought after is ever named", named.any { it.sought })
        assertTrue("nothing dangerous is ever named", named.any { it.hazard.severity.alwaysShow })
    }

    @Test
    fun `nothing uncommon and harmless takes a place at the top`() {
        (1..12).forEach { month ->
            worthKnowing(inMonth(month)).forEach {
                assertTrue(
                    "${it.id} is uncommon and not lethal",
                    it.prevalence != Prevalence.UNCOMMON ||
                        it.hazard.severity == Hazard.Severity.LETHAL,
                )
            }
        }
    }

    @Test
    fun `every taxon says how often it is met`() {
        // Left unset it defaults to uncommon, which silently removes a taxon from the
        // month view. A new row has to say.
        assertTrue(pack.taxa.count { it.prevalence != Prevalence.UNCOMMON } >= 60)
    }

    @Test
    fun `the month view gives the sought-after ones room of their own`() {
        // Sorted by severity alone, August put eight hazards above the first mushroom
        // anybody was looking for — which is the page it was before they were added.
        val august = worthKnowing(inMonth(8))
        assertTrue("nothing sought after in August: $august", august.count { it.sought } >= 3)
        assertTrue(august.any { it.hazard.severity.alwaysShow })
    }

    @Test
    fun `the dangerous ones still come first`() {
        (1..12).forEach { month ->
            val named = worthKnowing(inMonth(month))
            val lastDangerous = named.indexOfLast { it.hazard.severity.alwaysShow }
            val firstSought = named.indexOfFirst { !it.hazard.severity.alwaysShow }
            if (lastDangerous >= 0 && firstSought >= 0) {
                assertTrue("month $month interleaves them", lastDangerous < firstSought)
            }
        }
    }
    @Test
    fun `a sought-after mushroom names what it is taken for`() {
        // The five sought-after rows all used to read "Sought after." and nothing else,
        // which is five lines that say what the heading has already said. What each one
        // is confused with, while both are out, is the reason to read the page at all.
        val august = inMonth(8)
        val chanterelle = pack.taxon("cantharellus_appalachiensis")!!
        val taken = confusedWith(chanterelle, august)
        assertNotNull("the chanterelle is taken for something in August", taken)
        assertTrue(
            "it should name the worst of them, not the first",
            taken!!.hazard.severity == Hazard.Severity.SEVERE,
        )
    }

    @Test
    fun `nothing harmless is ever named as the thing it is taken for`() {
        (1..12).forEach { month ->
            val out = inMonth(month)
            out.forEach { t ->
                confusedWith(t, out)?.let {
                    assertTrue(
                        "${t.id} is warned about ${it.id}, which is not documented as harmful",
                        it.hazard.severity.ordinal > Hazard.Severity.UNKNOWN.ordinal,
                    )
                }
            }
        }
    }

    @Test
    fun `what it is taken for is out in the same month`() {
        (1..12).forEach { month ->
            val out = inMonth(month).toSet()
            out.forEach { t ->
                confusedWith(t, out.toList())?.let {
                    assertTrue("${it.id} is not out in month $month", it in out)
                }
            }
        }
    }

    /**
     * The warning reaches the row somebody is actually reading.
     *
     * Galerina marginata lists Armillaria mellea as a lookalike and its own note says
     * people have died from a small quantity picked among honey mushrooms. The honey
     * mushroom does not list Galerina back — the pack records the pair once, from
     * whichever end got written up first.
     *
     * [confusedWith] used to read only a taxon's own list, so the sentence could only
     * appear on the deadly one's row, whose page already says it at length. The row that
     * needs it is the sought-after one, which is what a person reads before going out.
     */
    @Test
    fun `a sought-after mushroom is told what it is taken for, whichever end the pack recorded`() {
        val out = inMonth(10)
        val honey = out.single { it.id == "armillaria_mellea" }
        val galerina = out.singleOrNull { it.id == "galerina_marginata" }

        assertTrue(
            "the deadly galerina must be in season when honey mushrooms are picked",
            galerina != null,
        )
        assertEquals(
            "the honey mushroom's row should name what it is taken for",
            "galerina_marginata",
            confusedWith(honey, out)?.id,
        )
    }

    /**
     * A mushroom people go looking for can always be warned about.
     *
     * A confusion in the pack is only ever shown on a month page where both are in
     * season, so a pair whose months do not touch is a warning that exists in the data
     * and cannot reach anybody. Two were like that. Galerina was listed December to
     * March against a honey mushroom in September and October. And Meripilus sumstinei
     * was listed **for July alone** — the hen of the woods is picked in September and
     * October, so the one thing it is confused with was never named beside it. Records
     * put Meripilus from May to October, three hundred of them in July and a hundred and
     * fifty in September; the single month was simply wrong.
     *
     * Restricted to the sought-after ones on purpose. Two taxa nobody hunts may genuinely
     * not overlap — Sarcoscypha dudleyi and Legaliana badia do not, and both match their
     * records — and that is a fact about mushrooms rather than a fault in the pack. The
     * ones somebody carries a basket for are the ones where silence costs something.
     */
    @Test
    fun `anything sought after shares a month with what it is taken for`() {
        val unreachable = pack.taxa.filter { it.sought }.mapNotNull { sought ->
            val partners = pack.taxa.filter { other ->
                other.id != sought.id &&
                    (sought.lookalikes.any { it.taxon == other.id } ||
                        other.lookalikes.any { it.taxon == sought.id })
            }
            val harmful = partners.filter {
                it.hazard.severity.ordinal > Hazard.Severity.UNKNOWN.ordinal
            }
            val overlapping = harmful.filter { partner ->
                sought.seasonMonths.isEmpty() || partner.seasonMonths.isEmpty() ||
                    (sought.seasonMonths intersect partner.seasonMonths).isNotEmpty()
            }
            if (harmful.isNotEmpty() && overlapping.isEmpty()) {
                "${sought.scientificName} ${sought.seasonMonths.sorted()} is taken for " +
                    harmful.joinToString { "${it.scientificName} ${it.seasonMonths.sorted()}" }
            } else {
                null
            }
        }
        assertTrue(
            "a warning the pack holds can never be shown:\n" + unreachable.joinToString("\n"),
            unreachable.isEmpty(),
        )
    }

    /**
     * The deadly galerina is in season in every month, because it is found in every
     * month.
     *
     * The pack's own season note said "found year-round" while its months listed four —
     * and the app reads the months, so a mushroom the data itself described as
     * year-round was absent from the month page for two thirds of the year, including
     * every month its lookalike is collected in. The note is not parsed into the model
     * at all, so nothing could have noticed the two disagreeing.
     */
    @Test
    fun `the deadly galerina is in season in every month`() {
        val galerina = pack.taxa.single { it.id == "galerina_marginata" }
        assertEquals(Hazard.Severity.LETHAL, galerina.hazard.severity)
        assertEquals(
            "it is found year-round and the month page reads these",
            (1..12).toSet(),
            galerina.seasonMonths,
        )
    }
}
