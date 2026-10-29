package com.wanderwildwood.kinokocho.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LicencesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun text(id: Int) = context.resources.openRawResource(id).bufferedReader().readText()

    @Test
    fun builtWithIsNotEmptyAndEveryEntryHasALinkAndALicence() {
        assertTrue(components.size >= 8)
        for (c in components) {
            assertTrue(c.name, c.link.startsWith("https://"))
            assertTrue(c.name, c.licence.isNotBlank())
        }
    }

    @Test
    fun theSourceLinkIsThePublicRepoAndBuildsAViewIntent() {
        assertEquals("https://github.com/wanderwildwood/kinokocho", SOURCE_URL)
        assertTrue(components.any { it.licence == "Apache-2.0" })
    }

    @Test
    fun theFullTextsAreBundledAndPage() {
        val gpl = text(R.raw.gpl_3_0)
        assertTrue(gpl.contains("END OF TERMS AND CONDITIONS"))
        assertTrue(licenceParagraphs(gpl).size > 50)
        for (t in licenceTexts) assertTrue(t.name, text(t.raw).length > 1000)
    }

    @Test
    fun theSafetyWordsSayNeverEatAndTheCoverageStaysSouthernAppalachian() {
        assertTrue(context.getString(R.string.safety_disclaimer).contains("Never eat"))
        assertTrue(context.getString(R.string.coverage_statement).contains("southern Appalachian"))
    }
}
