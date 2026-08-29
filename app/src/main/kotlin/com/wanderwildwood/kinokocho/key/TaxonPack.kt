package com.wanderwildwood.kinokocho.key

import android.content.res.AssetManager
import org.json.JSONObject

/**
 * A region's taxon data. The character schema is global; this is the part that
 * changes when you carry the phone somewhere else.
 */
data class TaxonPack(
    val packId: String,
    val packName: String,
    val schemaVersion: Int,
    val taxa: List<Taxon>,
) {
    private val byId = taxa.associateBy { it.id }
    fun taxon(id: String): Taxon? = byId[id]
}

data class Taxon(
    val id: String,
    val scientificName: String,
    val commonName: String? = null,
    val hazard: Hazard,
    val note: String? = null,
    /** characterId -> the states this taxon can show, each with how often. */
    val characters: Map<String, List<ScoredState>>,
    val measurements: Map<String, IntRange> = emptyMap(),
    val seasonMonths: Set<Int> = emptySet(),
    /**
     * How often a walker actually meets it here. The month view needs this: a calendar
     * that lists everything recorded in September is a calendar of eighty-six things.
     */
    val prevalence: Prevalence = Prevalence.UNCOMMON,
    /**
     * Whether people go out looking for this one.
     *
     * **Not an edibility field, and it must never become one.** The key never reads it
     * and nothing in this app says a mushroom is safe to eat. It exists because a month
     * view that can only ever warn is one nobody opens, and the destroying angel wants
     * to be on the list somebody actually reads in September.
     */
    val sought: Boolean = false,
    val lookalikes: List<Lookalike> = emptyList(),
    val sources: List<String> = emptyList(),
    /**
     * Whether a person who knows fungi has checked this row. Every row ships false,
     * and that is a fact about the data rather than a placeholder to tidy away.
     */
    val reviewed: Boolean = false,
)

data class ScoredState(val value: String, val frequency: Frequency)

enum class Frequency { ALWAYS, USUALLY, SOMETIMES, RARELY }

enum class Prevalence { COMMON, OCCASIONAL, UNCOMMON }

data class Lookalike(
    val taxon: String,
    val discriminators: List<String>,
    val note: String,
)

data class Hazard(
    val severity: Severity,
    val toxinClass: String? = null,
    val onset: String? = null,
    val note: String? = null,
    val source: String? = null,
) {
    /**
     * How much it matters to get this one wrong. Ordering is deliberate: [LETHAL] and
     * [SEVERE] taxa are never dropped from view for ranking poorly, because the whole
     * point is that they are what you must rule out rather than what you hope to find.
     */
    enum class Severity {
        /** No documented harm. Not a claim that it is edible; this app never makes one. */
        NONE_KNOWN,
        UNKNOWN,
        INTOXICATION,
        GI,
        SEVERE,
        LETHAL,
        ;

        val alwaysShow: Boolean get() = this == LETHAL || this == SEVERE
    }
}

object PackLoader {

    fun load(assets: AssetManager, fileName: String): TaxonPack =
        assets.open(fileName).use { parse(it.readBytes().decodeToString()) }

    fun parse(json: String): TaxonPack {
        val root = JSONObject(json)
        val arr = root.getJSONArray("taxa")
        val taxa = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)

            val characters = o.getJSONObject("characters").let { co ->
                co.keys().asSequence().associateWith { key ->
                    val states = co.getJSONArray(key)
                    (0 until states.length()).map { j ->
                        val s = states.getJSONObject(j)
                        ScoredState(
                            s.getString("value"),
                            Frequency.valueOf(s.getString("frequency").uppercase()),
                        )
                    }
                }
            }

            val measurements = o.optJSONObject("measurements")?.let { mo ->
                mo.keys().asSequence().mapNotNull { key ->
                    val a = mo.getJSONArray(key)
                    if (a.length() == 2) key to a.getInt(0)..a.getInt(1) else null
                }.toMap()
            }.orEmpty()

            val season = o.optJSONObject("season")?.optJSONArray("months")?.let { a ->
                (0 until a.length()).map { a.getInt(it) }.toSet()
            }.orEmpty()

            val lookalikes = o.optJSONArray("lookalikes")?.let { a ->
                (0 until a.length()).map { j ->
                    val l = a.getJSONObject(j)
                    val d = l.getJSONArray("discriminators")
                    Lookalike(
                        l.getString("taxon"),
                        (0 until d.length()).map { d.getString(it) },
                        l.getString("note"),
                    )
                }
            }.orEmpty()

            val sources = o.optJSONArray("sources")?.let { a ->
                (0 until a.length()).map { a.getString(it) }
            }.orEmpty()

            val h = o.getJSONObject("hazard")
            Taxon(
                id = o.getString("id"),
                scientificName = o.getString("scientificName"),
                commonName = o.optString("commonName").ifEmpty { null },
                hazard = Hazard(
                    severity = Hazard.Severity.valueOf(h.getString("severity")),
                    toxinClass = h.optString("toxinClass").ifEmpty { null },
                    onset = h.optString("onset").ifEmpty { null },
                    note = h.optString("note").ifEmpty { null },
                    source = h.optString("source").ifEmpty { null },
                ),
                note = o.optString("note").ifEmpty { null },
                characters = characters,
                measurements = measurements,
                prevalence = when (val p = o.optString("prevalence", "uncommon")) {
                    "common" -> Prevalence.COMMON
                    "occasional" -> Prevalence.OCCASIONAL
                    "uncommon" -> Prevalence.UNCOMMON
                    else -> error("unknown prevalence '$p' on ${o.getString("id")}")
                },
                sought = o.optBoolean("sought", false),
                seasonMonths = season,
                lookalikes = lookalikes,
                sources = sources,
                reviewed = o.optBoolean("reviewed", false),
            )
        }
        return TaxonPack(
            root.getString("packId"),
            root.getString("packName"),
            root.getInt("schemaVersion"),
            taxa,
        )
    }
}
