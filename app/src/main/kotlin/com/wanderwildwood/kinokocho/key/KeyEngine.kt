package com.wanderwildwood.kinokocho.key

import com.wanderwildwood.kinokocho.schema.Character
import com.wanderwildwood.kinokocho.schema.CharacterSchema
import kotlin.math.ln

/**
 * Ranks candidates against what has been answered, and decides what to ask next.
 *
 * Two rules run through all of it, and both are deliberate:
 *
 * **Nothing is ever eliminated.** A mismatch is a heavy penalty, not a deletion. Hard
 * elimination is what makes multi-access keys infuriating - one misjudged gill spacing
 * and the right answer vanishes, with no way back and no explanation.
 *
 * **An unscored character costs a taxon nothing.** The dataset is incomplete and always
 * will be. If a missing row counted as a mismatch, the taxa documented most carefully
 * would rank worst, which is exactly backwards.
 */
class KeyEngine(
    private val schema: CharacterSchema,
    private val pack: TaxonPack,
) {

    /** What the person has tapped so far: characterId to the values they chose. */
    data class Answers(
        val values: Map<String, Set<String>> = emptyMap(),
        /** Characters they looked at and could not answer. Scored as unanswered. */
        val notTested: Set<String> = emptySet(),
        val month: Int? = null,
    ) {
        fun with(characterId: String, chosen: Set<String>): Answers =
            copy(values = values + (characterId to chosen), notTested = notTested - characterId)

        fun markNotTested(characterId: String): Answers =
            copy(values = values - characterId, notTested = notTested + characterId)

        val answeredCount: Int get() = values.count { it.value.isNotEmpty() }
    }

    data class Candidate(
        val taxon: Taxon,
        val score: Double,
        val matched: Int,
        val mismatched: Int,
        /** Characters where this taxon says nothing, so nothing could be concluded. */
        val unscored: Int,
    )

    data class Ranking(
        val candidates: List<Candidate>,
        /**
         * Dangerous taxa still consistent with the answers, kept in view however they
         * rank. This is the list that must never be collapsed into a single number.
         */
        val hazards: List<Candidate>,
    )

    fun rank(answers: Answers): Ranking {
        val scored = pack.taxa.map { score(it, answers) }
            .sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.taxon.scientificName })

        val hazards = scored.filter {
            it.taxon.hazard.severity.alwaysShow && it.mismatched == 0
        }
        return Ranking(scored, hazards)
    }

    private fun score(taxon: Taxon, answers: Answers): Candidate {
        var score = 0.0
        var matched = 0
        var mismatched = 0
        var unscored = 0

        answers.values.forEach { (characterId, chosen) ->
            if (chosen.isEmpty()) return@forEach
            val states = taxon.characters[characterId]
            if (states == null) {
                unscored++
                return@forEach
            }
            // A multi-select answer matches if ANY chosen state is one the taxon shows.
            // A cap can be both scaly and dry, and requiring all of them would punish an
            // observant person for noticing more than the reference happened to record.
            val best = chosen.mapNotNull { c -> states.firstOrNull { it.value == c }?.frequency }
                .minByOrNull { it.ordinal }
            when (best) {
                Frequency.ALWAYS, Frequency.USUALLY -> { score += FULL_MATCH; matched++ }
                Frequency.SOMETIMES, Frequency.RARELY -> { score += PARTIAL_MATCH; matched++ }
                null -> { score += MISMATCH; mismatched++ }
            }
        }

        // Season is a tiebreaker and never a filter. Fungi do not read calendars, and an
        // out-of-season find is exactly the kind of thing worth writing down.
        val month = answers.month
        if (month != null && taxon.seasonMonths.isNotEmpty()) {
            score += if (month in taxon.seasonMonths) SEASON_BONUS else SEASON_PENALTY
        }

        return Candidate(taxon, score, matched, mismatched, unscored)
    }

    /**
     * Which question to put in front of the reader next.
     *
     * Information gain over the candidates still in play, not a fixed order. Latex is
     * the reason this cannot be a fixed order: it is near-decisive when present and
     * absent for most taxa, so any list sorted by usefulness asks it far too early.
     * Entropy handles that asymmetry and a hand-written order does not.
     *
     * Until something has been answered there is no candidate set to measure, so the
     * schema's own power hint breaks the tie and the first question is the root one.
     */
    fun nextQuestion(answers: Answers, considerTop: Int = 12): String? {
        val ranking = rank(answers)
        val live = ranking.candidates.take(considerTop).map { it.taxon }
        if (live.isEmpty()) return null

        val settling = charactersThatSettleAHazard(ranking)
        val declared = withGatingQuestions(settling.declared, answers)
        val disagreeing = withGatingQuestions(settling.disagreeing, answers)

        val askable = schema.characters
            .filter { it.id !in answers.values.keys && it.id !in answers.notTested }
            .filter { schema.isApplicable(it.id, answers.values) }
            .filter { it.availability == Character.Availability.FIELD }
            .map { it to askingValue(it, live) }
            // Never ask something that cannot separate anything. Once one candidate
            // stands alone there is no next question, and the honest answer is none.
            // This applies to a settling character too: boosting a question that
            // splits nothing would be asking for the appearance of safety.
            .filter { it.second > 0.0 }

        // Anything that would settle a live deadly candidate is asked before anything
        // that would not, rather than merely scoring higher than it.
        //
        // This was a x3 multiplier, which is a bet that no ordinary question can ever
        // score three times a lethal one. The bet held at twenty taxa and lost at
        // thirty-nine: adding two more gilled mushrooms on wood was enough to push
        // Galerina's settling question past the fourth question asked, with no warning
        // and no failure anywhere except one test. A multiplier degrades as the pack
        // grows, and this pack is meant to grow to a hundred. Precedence does not.
        //
        // Within the tier, ordering is still gain ratio weighted by power - the boost
        // decides which questions are eligible, never which of them is best.
        // Three grades, in order: what a person said settles a deadly pair, what the
        // data implies would settle it, then everything else. Within each grade the
        // ordering is still gain ratio weighted by power.
        return askable.filter { it.first.id in declared }
            .ifEmpty { askable.filter { it.first.id in disagreeing } }
            .ifEmpty { askable }
            .maxByOrNull { it.second }
            ?.first?.id
    }

    /**
     * Characters that would tell a live deadly candidate apart from what is currently
     * leading the list.
     *
     * These get asked ahead of their information gain, and they have to be. A first
     * walkthrough of this key, holding a destroying angel, asked about odour and nearby
     * trees and never once asked about the base of the stem - which is the character
     * the entire Amanita problem turns on. Entropy has no idea which mistakes are fatal
     * and which are merely wrong, so it cannot be left to decide alone.
     */
    private fun charactersThatSettleAHazard(ranking: Ranking): Settling {
        val hazards = ranking.hazards
        if (hazards.isEmpty()) return Settling(emptySet(), emptySet())

        val leader = ranking.candidates.firstOrNull()?.taxon
        return hazards.flatMap { hazard ->
            // What its own data says would separate it, plus what anything it is
            // confusable with says.
            val declared = hazard.taxon.lookalikes.flatMap { it.discriminators } +
                pack.taxa.flatMap { other ->
                    other.lookalikes.filter { it.taxon == hazard.taxon.id }.flatMap { it.discriminators }
                }

            // And, regardless of what anyone wrote down: any character where the hazard
            // and the current leader genuinely disagree. That is what would settle it.
            val disagreeing = if (leader == null || leader.id == hazard.taxon.id) {
                emptyList()
            } else {
                hazard.taxon.characters.keys.filter { characterId ->
                    val mine = definiteStates(hazard.taxon, characterId)
                    val theirs = definiteStates(leader, characterId)
                    mine.isNotEmpty() && theirs.isNotEmpty() && mine.intersect(theirs).isEmpty()
                }
            }
            declared.map { it to true } + disagreeing.map { it to false }
        }.let { pairs ->
            val declared = pairs.filter { it.second }.map { it.first }.toSet()
            // A character named by hand is not put in the same bag as one merely
            // inferred, even if both would settle the pair.
            Settling(declared, pairs.map { it.first }.toSet() - declared)
        }
    }

    /**
     * What would settle a live deadly candidate, in two grades.
     *
     * [declared] is what a person wrote into a lookalike entry: this is the character
     * that tells these two apart. [disagreeing] is what the data merely implies,
     * because the hazard and the current leader happen to hold different states.
     *
     * They are kept apart because the second grows with the pack and the first does
     * not. At thirty-nine taxa the inferred set had swollen enough to bury Galerina's
     * hand-written discriminators under questions that were technically settling and
     * practically beside the point - which is how the pack getting better made the key
     * get worse.
     */
    private data class Settling(val declared: Set<String>, val disagreeing: Set<String>)

    /**
     * Adds the questions standing between the reader and a character that would settle
     * a deadly candidate.
     *
     * Without this the boost is useless in the case it matters most. The base of the
     * stem is only askable once "is there a stem?" has been answered, and that gating
     * question carries almost no information - nearly everything with gills has a
     * central stem, so entropy will never choose it. The result was a key that boosted
     * the volva question and then never asked it, because it was locked behind a door
     * nothing had a reason to open.
     */
    private fun withGatingQuestions(settling: Set<String>, answers: Answers): Set<String> {
        if (settling.isEmpty()) return settling
        val out = settling.toMutableSet()
        val queue = ArrayDeque(settling)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (schema.isApplicable(id, answers.values)) continue
            schema.dependencies.filter { it.character == id }.forEach { dep ->
                if (dep.requiresCharacter !in answers.values.keys && out.add(dep.requiresCharacter)) {
                    queue.addLast(dep.requiresCharacter)
                }
            }
        }
        return out
    }

    private fun definiteStates(taxon: Taxon, characterId: String): Set<String> =
        taxon.characters[characterId]
            ?.filter { it.frequency == Frequency.ALWAYS || it.frequency == Frequency.USUALLY }
            ?.map { it.value }?.toSet()
            .orEmpty()

    /**
     * How worth asking a character is, over the live candidates.
     *
     * Two corrections to raw information gain, both needed:
     *
     * **Gain ratio, not gain.** Raw gain rewards a character simply for having many
     * states, because more states shatter the set into smaller groups. Cap colour has
     * sixteen and would be chosen as the opening question - and cap colour is the one
     * character every source warns is unreliable, since rain, age and sun all change it.
     * Dividing by the entropy of the split itself is Quinlan's correction and it removes
     * that bias.
     *
     * **Reliability weighting.** DELTA weights characters by how dependably a person can
     * apply them, and the schema's power field is that judgement. A character that is
     * slightly less informative but far easier to get right is the better question.
     */
    private fun askingValue(character: Character, live: List<Taxon>): Double {
        val gain = informationGain(character.id, live)
        if (gain <= 0.0) return 0.0
        val split = splitEntropy(character.id, live)
        val ratio = if (split <= 0.0) 0.0 else gain / split
        return ratio * (character.power / 5.0)
    }

    /**
     * The entropy of the partition a character induces, regardless of how useful the
     * partition is. This is the denominator that stops a sixteen-state character from
     * winning on cardinality alone.
     */
    fun splitEntropy(characterId: String, live: List<Taxon>): Double {
        if (live.size < 2) return 0.0
        val n = live.size.toDouble()
        return groupsFor(characterId, live).values
            .sumOf { group ->
                val p = group.size / n
                if (p <= 0.0) 0.0 else -p * (ln(p) / LN2)
            }
    }

    /**
     * Shannon entropy removed by asking about a character, over the live candidates.
     *
     * Taxa that say nothing about a character are counted as their own group rather
     * than dropped. "The data cannot answer this" is a real outcome of asking, and
     * pretending otherwise makes sparsely-documented characters look more useful than
     * they are.
     */
    fun informationGain(characterId: String, live: List<Taxon>): Double {
        if (live.size < 2) return 0.0
        val n = live.size.toDouble()
        val remaining = groupsFor(characterId, live).values
            .sumOf { group -> (group.size / n) * entropyOf(group.size) }
        return entropyOf(live.size) - remaining
    }

    /** Live taxa bucketed by the answer this character would give for each of them. */
    private fun groupsFor(characterId: String, live: List<Taxon>): Map<String, List<Taxon>> =
        live.groupBy { taxon ->
            taxon.characters[characterId]
                ?.filter { it.frequency == Frequency.ALWAYS || it.frequency == Frequency.USUALLY }
                ?.map { it.value }?.toSortedSet()?.toString()
                ?: UNSCORED
        }

    private fun entropyOf(size: Int): Double = if (size <= 1) 0.0 else ln(size.toDouble()) / LN2

    /**
     * The dangerous taxa still consistent with what has been answered, each with the
     * characters that would separate it from what the reader is probably hoping for.
     */
    fun safetyNotes(answers: Answers): List<SafetyNote> {
        val ranking = rank(answers)
        val liveIds = ranking.candidates.filter { it.mismatched == 0 }.map { it.taxon.id }.toSet()
        return ranking.hazards.map { candidate ->
            val fromHazard = candidate.taxon.lookalikes.filter { it.taxon in liveIds }
            val fromOthers = pack.taxa
                .filter { it.id in liveIds }
                .flatMap { other -> other.lookalikes.filter { it.taxon == candidate.taxon.id } }
            val all = fromHazard + fromOthers
            SafetyNote(
                taxon = candidate.taxon,
                discriminators = all.flatMap { it.discriminators }.distinct()
                    .filter { it !in answers.values.keys },
                notes = all.map { it.note }.distinct(),
            )
        }
    }

    data class SafetyNote(
        val taxon: Taxon,
        val discriminators: List<String>,
        val notes: List<String>,
    )

    /**
     * The character that would most reduce the candidate set but was never recorded.
     *
     * This is the "what you did not write down" answer, and it is the highest-value
     * thing a journal can offer, because it is what trains the next walk. Deferred
     * characters are included here even though [nextQuestion] excludes them: at home,
     * the spore print is exactly the thing worth going back for.
     */
    fun mostValuableMissing(answers: Answers, considerTop: Int = 12): List<Pair<String, Double>> {
        val live = rank(answers).candidates.take(considerTop).map { it.taxon }
        if (live.size < 2) return emptyList()
        return schema.characters
            .filter { it.id !in answers.values.keys }
            .filter { schema.isApplicable(it.id, answers.values) }
            .map { it.id to informationGain(it.id, live) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
    }

    private companion object {
        const val FULL_MATCH = 1.0
        const val PARTIAL_MATCH = 0.4

        // Heavy, but finite. One wrong tap must cost a taxon its place at the top
        // without erasing it from the list entirely.
        const val MISMATCH = -2.0

        const val SEASON_BONUS = 0.15
        const val SEASON_PENALTY = -0.15
        const val UNSCORED = " unscored"
        val LN2 = ln(2.0)
    }
}
