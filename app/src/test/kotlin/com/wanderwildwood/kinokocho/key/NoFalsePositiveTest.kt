package com.wanderwildwood.kinokocho.key

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.schema.Character
import com.wanderwildwood.kinokocho.schema.SchemaLoader
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A mushroom listed as fitting has to fit everything that was tapped.
 *
 * The engine keeps a taxon in [KeyEngine.Ranking.live] until an answer contradicts it,
 * and a character the pack says nothing about can never contradict anything. So every
 * blank in the pack was a place where a mushroom survived an answer it does not match:
 * in 0.3.8, 755 of the 3,450 cells that apply were blank, and a tap on "warts on the
 * cap" left a hundred and fifteen mushrooms standing that have none. The engine was
 * right and the list was wrong.
 *
 * The fix is the data, not the rule. Treating silence as a mismatch would turn every
 * gap still left into a false negative, and a false negative here can be the destroying
 * angel dropping off the screen. So these check the pack is complete where it applies,
 * and that the engine then does what the heading above the list says.
 */
@RunWith(RobolectricTestRunner::class)
class NoFalsePositiveTest {

    private val assets = ApplicationProvider.getApplicationContext<Context>().assets
    private val schema = SchemaLoader.load(assets)
    private val pack = PackLoader.load(assets, "packs/southern-appalachia-v1.json")
    private val engine = KeyEngine(schema, pack)

    /**
     * Questions about the specimen rather than the species. The schema says of both that
     * they are never keyed on: they only decide how much a missing veil may rule out.
     */
    private val observer = setOf("age", "condition")

    // The spore print and KOH are answered at home rather than in the wood, and a blank
    // there lets a mushroom through just the same.
    private val keyed: List<Character> = schema.characters
        .filter { it.kind != Character.Kind.MEASUREMENT }
        .filter { it.id !in observer }

    /**
     * Whether the key can ask this taxon's finder about this character — every question
     * it depends on has an answer this taxon shows, all the way up.
     */
    private fun applies(taxon: Taxon, characterId: String): Boolean =
        schema.dependencies.filter { it.character == characterId }.all { dep ->
            taxon.characters[dep.requiresCharacter].orEmpty().any { it.value in dep.requiresAnyOf } &&
                applies(taxon, dep.requiresCharacter)
        }

    /**
     * Recorded, or recorded as unknown with the reason, and nothing silent.
     *
     * Some things are not in the literature at all — the taste of a destroying angel, the
     * KOH reaction of most boletes — and inventing them would be worse than the gap. Those
     * are written into `unrecorded` with why, and the key then says it could not check
     * rather than counting the tap as agreement.
     */
    @Test
    fun `every question the key can ask about a mushroom has an answer in the pack`() {
        val blank = pack.taxa.flatMap { t ->
            keyed.filter { applies(t, it.id) && t.characters[it.id].isNullOrEmpty() }
                .filterNot { it.id in t.unrecorded }
                .map { "${t.id}.${it.id}" }
        }
        assertTrue(
            "${blank.size} blanks, each a place where this mushroom survives an answer " +
                "it does not match: $blank",
            blank.isEmpty(),
        )
    }

    @Test
    fun `nothing is marked unknown that is recorded, or that the key cannot ask`() {
        val wrong = pack.taxa.flatMap { t ->
            t.unrecorded.keys.filter { c ->
                !t.characters[c].isNullOrEmpty() || keyed.none { it.id == c } || !applies(t, c)
            }.map { "${t.id}.$it" }
        }
        assertTrue("unrecorded entries that are not blanks the key can reach: $wrong", wrong.isEmpty())
        pack.taxa.forEach { t ->
            t.unrecorded.forEach { (c, why) -> assertTrue("${t.id}.$c gives no reason", why.isNotBlank()) }
        }
    }

    @Test
    fun `a tap on something a mushroom does not show takes it off the list`() {
        val survived = mutableListOf<String>()
        pack.taxa.forEach { t ->
            keyed.filter { applies(t, it.id) && it.id !in t.unrecorded }.forEach { c ->
                val shown = t.characters[c.id].orEmpty().map { it.value }.toSet()
                val parents = schema.dependencies.filter { it.character == c.id }
                    .associate { d ->
                        d.requiresCharacter to t.characters[d.requiresCharacter].orEmpty()
                            .map { it.value }.filter { it in d.requiresAnyOf }.take(1).toSet()
                    }
                schema.valuesOf(c)
                    .filterNot { it.uncertain }
                    .filterNot { it.id in shown }
                    .forEach { v ->
                        val answers = KeyEngine.Answers(values = parents + (c.id to setOf(v.id)))
                        if (engine.rank(answers).live.any { it.taxon.id == t.id }) {
                            survived += "${t.id}: ${c.id}=${v.id}"
                        }
                    }
            }
        }
        assertTrue("still listed as fitting after a contradicting tap: $survived", survived.isEmpty())
    }

    @Test
    fun `a mushroom described exactly as the pack describes it is still on the list`() {
        // The other direction, and the one that matters more: completing the pack must
        // not have left a taxon that its own typical description rules out.
        val lost = pack.taxa.filterNot { t ->
            val typical = t.characters
                .filterKeys { id -> keyed.any { it.id == id } }
                .mapValues { (_, states) ->
                    states.filter {
                        it.frequency == Frequency.ALWAYS || it.frequency == Frequency.USUALLY
                    }.take(1).map { it.value }.toSet()
                }
                .filterValues { it.isNotEmpty() }
            engine.rank(KeyEngine.Answers(values = typical)).live.any { it.taxon.id == t.id }
        }.map { it.id }
        assertTrue("ruled out by its own description: $lost", lost.isEmpty())
    }
}
