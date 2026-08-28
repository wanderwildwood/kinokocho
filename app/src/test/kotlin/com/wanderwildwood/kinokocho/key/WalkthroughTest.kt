package com.wanderwildwood.kinokocho.key

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.kinokocho.schema.SchemaLoader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Not an assertion - a way to read what the key actually asks, in order. */
@RunWith(RobolectricTestRunner::class)
class WalkthroughTest {

    private val assets = ApplicationProvider.getApplicationContext<Context>().assets
    private val schema = SchemaLoader.load(assets)
    private val pack = PackLoader.load(assets, "packs/southern-appalachia-v1.json")
    private val engine = KeyEngine(schema, pack)

    private fun walk(target: String, steps: Int = 7) {
        val taxon = pack.taxon(target)!!
        var a = KeyEngine.Answers(month = 9)
        println("\n=== pretending to hold a ${taxon.scientificName} ===")
        repeat(steps) { i ->
            val q = engine.nextQuestion(a) ?: run { println("  no further question"); return }
            val ch = schema.character(q)!!
            val v = taxon.characters[q]?.firstOrNull()?.value
            a = if (v != null) a.with(q, setOf(v)) else a.markNotTested(q)
            val label = v?.let { id ->
                schema.valuesOf(ch).firstOrNull { it.id == id }?.label ?: id
            } ?: "(not tested)"
            val top = engine.rank(a).candidates.take(3)
            val hz = engine.rank(a).hazards.map { it.taxon.scientificName }
            println("  ${i + 1}. ${ch.label}  ->  $label")
            println("       leading: " + top.joinToString(", ") {
                "${it.taxon.scientificName} ${"%.1f".format(it.score)}"
            })
            if (hz.isNotEmpty()) println("       still possible and dangerous: ${hz.joinToString(", ")}")
        }
    }

    @Test fun walkthroughs() {
        walk("amanita_bisporigera")
        walk("cantharellus_lateritius")
        walk("galerina_marginata")
    }
}
