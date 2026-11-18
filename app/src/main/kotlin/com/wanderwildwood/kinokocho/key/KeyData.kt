package com.wanderwildwood.kinokocho.key

import android.content.res.AssetManager
import com.wanderwildwood.kinokocho.schema.CharacterSchema
import com.wanderwildwood.kinokocho.schema.SchemaLoader

/**
 * The character schema, the region's pack and the engine over them, read once a process.
 *
 * The pack is over a megabyte of JSON, and parsing it while the first screen was being
 * built held that screen back on the phone's slow processor. The activity reads it here
 * off the main thread before it draws the journal, so the view model finds it ready.
 * Nothing in it changes once read, so one copy serves everything.
 */
class KeyData private constructor(
    val schema: CharacterSchema,
    val pack: TaxonPack,
) {
    val engine = KeyEngine(schema, pack)

    companion object {
        private const val PACK = "packs/southern-appalachia-v1.json"

        @Volatile
        private var loaded: KeyData? = null

        /** Read on the calling thread if it has not been read yet, so call it off the main one first. */
        fun get(assets: AssetManager): KeyData =
            loaded ?: synchronized(this) {
                loaded ?: KeyData(SchemaLoader.load(assets), PackLoader.load(assets, PACK))
                    .also { loaded = it }
            }
    }
}
