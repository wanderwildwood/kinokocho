package com.wanderwildwood.kinokocho.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.key.Hazard
import com.wanderwildwood.kinokocho.key.PackLoader
import com.wanderwildwood.kinokocho.key.Prevalence
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
}
