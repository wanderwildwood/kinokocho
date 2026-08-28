package com.wanderwildwood.kinokocho.schema

import android.content.res.AssetManager
import org.json.JSONObject

/**
 * Reads the character schema from assets and refuses to hand back a broken one.
 *
 * Validation is deliberately strict and happens at load: a dependency naming a
 * character or value that does not exist is a bug in the data, and it should be a
 * loud failure in a test rather than a question that silently never appears on a
 * phone in the woods.
 */
object SchemaLoader {

    fun load(assets: AssetManager, fileName: String = "schema/characters-v1.json"): CharacterSchema =
        assets.open(fileName).use { parse(it.readBytes().decodeToString()) }

    fun parse(json: String): CharacterSchema {
        val root = JSONObject(json)

        val colours = root.getJSONArray("colours").map {
            ColourTerm(it.getString("id"), it.getString("label"), it.getString("gloss"))
        }

        val characters = root.getJSONArray("characters").map { o ->
            Character(
                id = o.getString("id"),
                label = o.getString("label"),
                hint = o.optString("hint").ifEmpty { null },
                kind = when (val k = o.optString("kind", "state")) {
                    "state" -> Character.Kind.STATE
                    "measurement" -> Character.Kind.MEASUREMENT
                    else -> error("unknown character kind '$k' on ${o.getString("id")}")
                },
                cardinality = when (val c = o.getString("cardinality")) {
                    "single" -> Character.Cardinality.SINGLE
                    "multi" -> Character.Cardinality.MULTI
                    else -> error("unknown cardinality '$c' on ${o.getString("id")}")
                },
                availability = when (val a = o.getString("availability")) {
                    "field" -> Character.Availability.FIELD
                    "deferred" -> Character.Availability.DEFERRED
                    else -> error("unknown availability '$a' on ${o.getString("id")}")
                },
                power = o.getInt("power"),
                controlling = o.optBoolean("controlling", false),
                notTested = o.optBoolean("notTested", false),
                regional = o.optBoolean("regional", false),
                note = o.optString("note").ifEmpty { null },
                valuesFromColours = o.optString("valuesFrom") == "colours",
                values = o.optJSONArray("values")?.map {
                    CharacterValue(it.getString("id"), it.getString("label"))
                }.orEmpty(),
            )
        }

        val dependencies = root.optJSONArray("dependencies")?.map { o ->
            val requires = o.getJSONObject("requires")
            Dependency(
                character = o.getString("character"),
                requiresCharacter = requires.getString("character"),
                requiresAnyOf = requires.getJSONArray("anyOf").let { a ->
                    (0 until a.length()).map { a.getString(it) }.toSet()
                },
            )
        }.orEmpty()

        return CharacterSchema(root.getInt("schemaVersion"), colours, characters, dependencies)
            .also(::validate)
    }

    private fun validate(schema: CharacterSchema) {
        val ids = schema.characters.map { it.id }
        require(ids.size == ids.toSet().size) { "duplicate character id" }

        schema.characters.forEach { c ->
            require(c.power in 1..5) { "${c.id}: power ${c.power} out of range" }
            require(c.valuesFromColours || c.values.isNotEmpty()) { "${c.id}: no values" }
            require(!(c.valuesFromColours && c.values.isNotEmpty())) {
                "${c.id}: cannot both borrow the colour list and define its own values"
            }
            val vs = c.values.map { it.id }
            require(vs.size == vs.toSet().size) { "${c.id}: duplicate value id" }
        }

        schema.dependencies.forEach { d ->
            val child = schema.character(d.character)
            requireNotNull(child) { "dependency names unknown character '${d.character}'" }
            val parent = schema.character(d.requiresCharacter)
            requireNotNull(parent) { "dependency names unknown parent '${d.requiresCharacter}'" }
            val parentValues = schema.valuesOf(parent).map { it.id }.toSet()
            d.requiresAnyOf.forEach {
                require(it in parentValues) { "${d.character}: parent has no value '$it'" }
            }
            require(parent.controlling) {
                "${d.requiresCharacter} controls ${d.character} but is not marked controlling"
            }
        }

        // A cycle would make isApplicable non-terminating for a reader and impossible to
        // answer: the question you must answer first is behind the one you cannot reach.
        val edges = schema.dependencies.groupBy({ it.character }, { it.requiresCharacter })
        val visiting = mutableSetOf<String>()
        val done = mutableSetOf<String>()
        fun walk(id: String) {
            require(id !in visiting) { "dependency cycle through '$id'" }
            if (id in done) return
            visiting += id
            edges[id].orEmpty().forEach(::walk)
            visiting -= id
            done += id
        }
        schema.characters.forEach { walk(it.id) }
    }

    private inline fun <T> org.json.JSONArray.map(f: (JSONObject) -> T): List<T> =
        (0 until length()).map { f(getJSONObject(it)) }
}
