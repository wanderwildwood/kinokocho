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

    @Test
    fun `the not-tested marker cannot collide with any value in the schema`() {
        // It rides in a character row alongside real states, so it has to be something
        // no pack can ever introduce. Value ids are lower-case, digits and underscores.
        assertTrue(MeasurementRow.NOT_TESTED.none { it.isLetterOrDigit() || it == '_' }
            || MeasurementRow.NOT_TESTED.first() !in 'a'..'z')
        val schema = SchemaLoader.load(
            ApplicationProvider.getApplicationContext<Context>().assets
        )
        schema.characters.forEach { character ->
            schema.valuesOf(character).forEach {
                assertTrue(it.id != MeasurementRow.NOT_TESTED)
            }
        }
    }

    @Test
    fun `the not-tested marker is not read back as a measurement`() {
        assertNull(MeasurementRow.decode(MeasurementRow.NOT_TESTED))
    }
}
