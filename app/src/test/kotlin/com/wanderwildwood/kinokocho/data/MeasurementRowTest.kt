package com.wanderwildwood.kinokocho.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.schema.Character
import com.wanderwildwood.kinokocho.schema.SchemaLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MeasurementRowTest {

    @Test
    fun `a measurement survives the round trip`() {
        val stored = MeasurementRow.encode("cap_width_mm", 45)
        assertEquals("cap_width_mm" to 45, MeasurementRow.decode(stored))
    }

    @Test
    fun `a state row is not mistaken for a measurement`() {
        assertNull(MeasurementRow.decode("sac_volva"))
        assertNull(MeasurementRow.decode("=45"))
        assertNull(MeasurementRow.decode("cap_width_mm="))
        assertNull(MeasurementRow.decode("cap_width_mm=large"))
    }

    @Test
    fun `no value id in the schema contains the separator`() {
        // If one ever did, every measurement written against it would come back cut in
        // the wrong place, and the loss would be silent.
        val schema = SchemaLoader.load(
            ApplicationProvider.getApplicationContext<Context>().assets
        )
        schema.characters.forEach { character ->
            schema.valuesOf(character).forEach { value ->
                assertTrue(
                    "${character.id}/${value.id} contains '='",
                    '=' !in value.id,
                )
            }
        }
    }

    @Test
    fun `the schema still declares a measurement for this to be about`() {
        val schema = SchemaLoader.load(
            ApplicationProvider.getApplicationContext<Context>().assets
        )
        assertTrue(schema.characters.any { it.kind == Character.Kind.MEASUREMENT })
    }
}
