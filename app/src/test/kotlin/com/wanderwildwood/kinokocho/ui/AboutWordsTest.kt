package com.wanderwildwood.kinokocho.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AboutWordsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun theSafetyWordsSayNeverEatAndTheCoverageStaysSouthernAppalachian() {
        assertTrue(context.getString(R.string.safety_disclaimer).contains("Never eat"))
        assertTrue(context.getString(R.string.coverage_statement).contains("southern Appalachian"))
    }

    @Test
    fun theAboutHasNoBackupRowsAndTheBackupDialogHasItsWords() {
        assertTrue(context.getString(R.string.backup_export).isNotBlank())
        assertTrue(context.getString(R.string.backup_import).isNotBlank())
        assertTrue(context.getString(R.string.backup_row).isNotBlank())
    }
}
