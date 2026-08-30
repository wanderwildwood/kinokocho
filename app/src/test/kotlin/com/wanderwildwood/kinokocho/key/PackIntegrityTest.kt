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
        pack.taxa.filter { it.hazard.severity != Hazard.Severity.NONE_KNOWN }.forEach {
            assertTrue(
                "${it.id} warns and then says nothing",
                !it.hazard.note.isNullOrBlank() || !it.hazard.onset.isNullOrBlank(),
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
}
