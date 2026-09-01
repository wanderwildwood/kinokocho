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
        /**
         * Millimetres, keyed by the measurement's value id - `cap_width_mm` and the
         * rest. Held apart from [values] because a number is not a state: it is matched
         * against a published range by overlap, never by equality.
         */
        val measurements: Map<String, Int> = emptyMap(),
        val month: Int? = null,
    ) {
        fun with(characterId: String, chosen: Set<String>): Answers =
            copy(values = values + (characterId to chosen), notTested = notTested - characterId)

        fun withMeasurement(valueId: String, mm: Int?): Answers = copy(
            measurements =
                if (mm == null) measurements - valueId else measurements + (valueId to mm),
        )

        fun markNotTested(characterId: String): Answers =
            copy(values = values - characterId, notTested = notTested + characterId)

        val answeredCount: Int
            get() = values.count { it.value.isNotEmpty() } + measurements.size
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
    ) {
        /**
         * The ones nothing has contradicted.
         *
         * ⚠ **Not the top of [candidates].** That list is sorted on score alone, and a
         * mismatch is worth -2.0 against a full match's +1.0 — so three good matches and
         * one contradiction outranks two partial matches and none. Every screen that
         * means "still possible" wants this, and four of them were taking the head of
         * [candidates] instead, which put ruled-out taxa under headings that said they
         * had not been ruled out.
         */
        val live: List<Candidate> get() = candidates.filter { it.mismatched == 0 }

        /**
         * What to put in front of somebody: the ones that fit, or the nearest few when
         * nothing fits everything.
         *
         * The fallback is deliberate and is not a way of hiding the empty case.
         * Contradictory answers are ordinary — a real mushroom against a description of a
         * typical one — and the nearest few are then the useful thing to show. But the
         * wording above the list has to change with it, so ask [live] whether it is
         * empty rather than inferring it from the length of this.
         */
        fun shortlist(limit: Int): List<Candidate> =
            live.ifEmpty { candidates }.take(limit)
    }

    fun rank(answers: Answers): Ranking {
        val scored = pack.taxa.map { score(it, answers) }
            .sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.taxon.scientificName })

        val hazards = scored.filter {
            it.taxon.hazard.severity.alwaysShow && it.mismatched == 0
        }
        return Ranking(scored, hazards)
    }

    private fun score(taxon: Taxon, answers: Answers): Candidate {
        val weathered = answers.values["age"].orEmpty().contains("old") ||
            answers.values["condition"].orEmpty().any { it == "rotten" || it == "dry" }
        var score = 0.0
        var matched = 0
        var mismatched = 0
        var unscored = 0

        answers.values.forEach { (characterId, chosen) ->
            if (chosen.isEmpty()) return@forEach

            // Values that describe the observer rather than the mushroom - the base was
            // left in the ground, the specimen is too old to tell whether it had a ring
            // - are dropped before scoring. No taxon carries them, so treating them as
            // states would mismatch everything: answering "I cannot tell" would empty
            // the candidate list and, worse, drop every hazard, because a hazard is only
            // shown while it has no mismatches. The honest answer must never be the one
            // that hides the destroying angel.
            val real = chosen.filterNot { schema.isUncertainValue(characterId, it) }.toSet()
            if (real.isEmpty()) {
                unscored++
                return@forEach
            }

            val states = taxon.characters[characterId]
            if (states == null) {
                unscored++
                return@forEach
            }
            /*
             * What a weathered specimen is allowed to rule out.
             *
             * [Character] has carried this instruction since the schema was written —
             * "an old specimen widens the tolerance on cap shape, gill colour and
             * margin, and suppresses any inference from an absent veil, because veils
             * fall off" — and nothing implemented it. There was no way to record that a
             * specimen was old either, so the note described behaviour that could not
             * have happened.
             *
             * The dangerous half is the veil. A destroying angel a fortnight out has
             * lost its ring to the rain, and "no ring" then mismatched every taxon that
             * has one — which does not merely reorder the list, it takes the taxon out
             * of the hazards, because a hazard is only carried while it has no
             * mismatches at all. The honest observation of an old mushroom was hiding
             * the thing the observation was for.
             *
             * Only in the direction that fails. Seeing a skirt on an old mushroom is
             * still seeing a skirt; it is the absence that rots away.
             */
            if (weathered && characterId in TIME_WORN) {
                val absent = real.all { it in DISAPPEARS }
                val soft = characterId in FALLS_OFF && absent
                if (states.none { it.value in real }) {
                    if (soft) {
                        unscored++
                        return@forEach
                    }
                    score += WEATHERED_MISMATCH
                    return@forEach
                }
            }
            // A multi-select answer matches if ANY chosen state is one the taxon shows.
            // A cap can be both scaly and dry, and requiring all of them would punish an
            // observant person for noticing more than the reference happened to record.
            val best = real.mapNotNull { c -> states.firstOrNull { it.value == c }?.frequency }
                .minByOrNull { it.ordinal }
            when (best) {
                Frequency.ALWAYS, Frequency.USUALLY -> { score += FULL_MATCH; matched++ }
                Frequency.SOMETIMES, Frequency.RARELY -> { score += PARTIAL_MATCH; matched++ }
                null -> { score += MISMATCH; mismatched++ }
            }
        }

        /*
         * Size, matched by overlap with the published range.
         *
         * Two things make this unlike every other character. A published range is
         * roughly the tenth to ninetieth percentile of what actually grows, so falling
         * outside one is ordinary rather than disqualifying - a button three days from
         * opening is under every range in the book. And the reader is estimating
         * millimetres by eye, in a wood, probably without a ruler.
         *
         * So a miss is a penalty and nothing more: it never increments [mismatched].
         * That counter is what decides whether a taxon is still shown as possible and
         * whether a deadly one stays in view, and a guessed number must not be able to
         * push a destroying angel off the screen.
         */
        answers.measurements.forEach { (key, mm) ->
            val range = taxon.measurements[key]
            if (range == null) {
                unscored++
                return@forEach
            }
            val slack = ((range.last - range.first) / 2).coerceAtLeast(1)
            when {
                mm in range -> { score += FULL_MATCH; matched++ }
                mm >= range.first - slack && mm <= range.last + slack -> {
                    score += PARTIAL_MATCH
                    matched++
                }
                else -> score += SIZE_MISS
            }
        }

        /*
         * Silence, scored at what the row scores when it does speak.
         *
         * "An unscored character costs a taxon nothing" was only half true. It cost no
         * penalty, but it earned nothing either, and the score is a sum — so a row
         * documented on eight characters could never out-rank one documented on thirty
         * that agreed just as well. The pack runs from eight to thirty with a median of
         * twenty-one, and *Meripilus sumstinei* was unreachable: it agreed with
         * everything asked and simply had fewer places to agree. That is the same bias
         * the rule above was written to prevent, arrived at from the other side.
         *
         * So an unrecorded character is scored at what this taxon scores where it is
         * recorded, which is the ordinary treatment of missing data and leaves a row
         * neither better nor worse for how much of it has been written.
         *
         * Damped, because a row with one described character that happens to match must
         * not be able to claim a perfect record over the whole schema. And never below
         * zero: silence is not a mismatch, however badly the rest of the row is going.
         */
        val scored = matched + mismatched
        if (unscored > 0 && scored > 0) {
            val mean = (score / scored).coerceAtLeast(0.0)
            // Never more than the evidence it is extrapolating from. Without the cap a
            // row that records four of the twenty characters somebody answered could
            // have most of its score standing in for the sixteen it says nothing about,
            // which is not a mushroom matching well, it is arithmetic.
            score += (unscored * mean * (scored / (scored + IMPUTE_DAMPING))).coerceAtMost(score)
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
     * The window is the leading candidates rather than the whole pack, so that the
     * question suits what is actually in play. But a window is only meaningful once
     * there is a ranking to take the head of. With nothing answered every taxon scores
     * the same and the sort falls through to scientific name, so taking twelve took the
     * twelve nearest the front of the alphabet - four Amanitas and an Armillaria - and
     * chose the opening question of the whole key from them. Knowing the month is not
     * better: it moves every in-season score by the same fraction, which sorts them
     * ahead of the rest without distinguishing any of them from each other. Until a
     * character has been answered the measurement is over the pack entire.
     */
    fun nextQuestion(answers: Answers, considerTop: Int = 12): String? {
        val ranking = rank(answers)
        val leading = ranking.candidates.take(considerTop)
        // Nothing answered means no ranking, whatever the scores say. A season bonus
        // gives every in-season taxon the same fraction of a point, which is enough to
        // sort them ahead of the rest and not enough to mean anything: the head of that
        // list is the first twelve in-season names alphabetically, and the opening
        // question of the whole key was being measured over them.
        val noRanking = answers.values.isEmpty() ||
            (leading.isNotEmpty() && ranking.candidates.size > leading.size &&
                ranking.candidates.last().score == leading.last().score)
        val live = (if (noRanking) ranking.candidates else leading).map { it.taxon }
        if (live.isEmpty()) return null

        val lethalInPlay = ranking.hazards.any {
            it.taxon.hazard.severity == Hazard.Severity.LETHAL
        }
        val settling = charactersThatSettleAHazard(ranking)
        val declared = withGatingQuestions(settling.declared, answers)
        val disagreeing = withGatingQuestions(settling.disagreeing, answers)

        val askable = schema.characters
            .filter { it.id !in answers.values.keys && it.id !in answers.notTested }
            .filter { schema.isApplicable(it.id, answers.values) }
            .filter { it.availability == Character.Availability.FIELD }
            /*
             * A measurement is never a question here.
             *
             * It cannot be. A state character splits the live candidates into groups
             * and, once the field is small, into singletons. Ranges overlap, so a
             * number never leaves one taxon standing; it scores about half what an
             * ordinary state character does, loses to all of them, and would sit at the
             * bottom of the queue for ever.
             *
             * The answer is not to weight it up until it wins. Size is not a narrowing
             * question at all: it is something you write down about the mushroom in
             * front of you, like the place and the photographs, and it is asked for
             * where those are. It still scores - a three-hundred millimetre bracket
             * should not rank a thumb-sized one first - it is just not asked here.
             */
            .filter { it.kind != Character.Kind.MEASUREMENT }
            /*
             * And never asks for a taste while something lethal is still in play.
             *
             * Tasting is a real field method — a small piece chewed and spat out — and
             * the schema explains how. But an app that has just told a reader it cannot
             * rule out a destroying angel must not, on the next screen, ask them what it
             * tastes like. Whatever the pharmacology says about the dose, a key that
             * invites you to put a possible Amanita in your mouth has stopped being
             * careful, and being careful is the entire argument for this app existing.
             *
             * It comes back the moment the deadly ones are ruled out, which is when
             * every guide says to use it.
             */
            .filterNot { it.id == "taste" && lethalInPlay }
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
        // data implies would settle it, then everything else.
        //
        // Within a settling grade the pick is by [Character.power] first and by
        // information only to break the tie, which is the opposite of the general case
        // and deliberate. The question there is not "what shrinks the list fastest" but
        // "what most reliably tells these two apart", and those are different
        // questions: ordering the settling grade by information sent a reader holding a
        // destroying angel through cap surface, cap colour and cap margin - three
        // characters that technically separate an Amanita from something and that rain
        // or a dry week will turn into the wrong answer - and reached the base of the
        // stem sixth.
        // Count before power, tested rather than assumed. Reliability first looked
        // like the right ordering — the tier is about telling two things apart, and a
        // character you can apply is worth more than one you cannot — but it walked a
        // reader holding a destroying angel past six questions without once asking
        // about the base of the stem, and cost the whole pack a fifth of a question on
        // average. How many deadly pairs a character settles is the better signal.
        val settle = compareBy<Pair<Character, Double>>(
            { declared[it.first.id] ?: 0 },
            { it.first.power },
            { it.second },
        )

        // Not until something has actually been answered. Before that every deadly
        // taxon in the pack is live, so "what would settle a hazard" is the union of
        // every discriminator anyone ever wrote — not a hint but noise, and it was
        // choosing the opening question of the whole key.
        //
        // The test for this was "are the scores still tied", and the month broke it.
        // Knowing it is September moves every score by a fraction and unties them, so
        // the app — which always knows the month — engaged the boost on question one
        // and opened with "what is the flesh like?", a question that needs a knife. A
        // season bonus is not somebody telling you something about the mushroom.
        if (answers.values.isNotEmpty()) {
            askable.filter { it.first.id in declared }
                .ifEmpty { askable.filter { it.first.id in disagreeing } }
                .maxWithOrNull(settle)
                ?.let { return it.first.id }
        }

        /*
         * The opening question is the one that unlocks the most others.
         *
         * Information gain cannot see this. It measures how well a character splits the
         * candidates in front of it, and is blind to the fact that answering "what kind
         * of fungus is it?" makes five further characters askable while "what is the cap
         * surface like?" makes none. With a hundred taxa the cap surface started winning
         * on gain alone, and the key opened by asking about a texture before it knew
         * whether the thing had a cap.
         *
         * Only for the first question. After that there is a real candidate set and
         * information is the better guide.
         */
        if (answers.values.isEmpty()) {
            val unlocks = schema.dependencies.groupingBy { it.requiresCharacter }.eachCount()
            askable.maxWithOrNull(
                compareBy({ unlocks[it.first.id] ?: 0 }, { it.second })
            )?.let { return it.first.id }
        }

        return bestByGainRatio(askable, live)
    }

    /**
     * The best question, by gain ratio among the characters that carry real information.
     *
     * Quinlan's own qualification on the gain ratio, and it is needed for the reason he
     * gives: dividing by how many answers a character has over-corrects, and hands the
     * choice to a character that splits the field in two and barely moves it. "Does it
     * bruise?" removes one bit; "what kind of fungus is it?" removes two and a half. The
     * ratio preferred the bruising question, and it opened the whole key with it.
     *
     * So the ratio only decides among characters that are at least averagely informative
     * to begin with. Below that line a character is not competing on how cleanly it
     * splits the field, it is competing on having had few ways to split it.
     */
    private fun bestByGainRatio(
        options: List<Pair<Character, Double>>,
        live: List<Taxon>,
    ): String? {
        if (options.isEmpty()) return null
        val gains = options.associate { it.first.id to informationGain(it.first.id, live) }
        val average = gains.values.filter { it > 0.0 }.average()
        return options
            .filter { (gains[it.first.id] ?: 0.0) >= average }
            .ifEmpty { options }
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
        if (hazards.isEmpty()) return Settling(emptyMap(), emptySet())

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
            // Counted, not collected. Sixty-one confusions across the pack name enough
            // characters between them that the declared set is most of the schema once
            // every deadly taxon is still live, and a tier holding most of the schema
            // is not a tier. How many live deadly pairs a character would settle is
            // what separates the base of the stem, which the Amanita problem turns on
            // over and over, from a smell that settles exactly one pair.
            val declared = pairs.filter { it.second }
                .groupingBy { it.first }.eachCount()
            Settling(declared, pairs.map { it.first }.toSet() - declared.keys)
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
    private data class Settling(val declared: Map<String, Int>, val disagreeing: Set<String>)

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
    private fun withGatingQuestions(
        settling: Map<String, Int>,
        answers: Answers,
    ): Map<String, Int> {
        if (settling.isEmpty()) return settling
        val out = settling.toMutableMap()
        val queue = ArrayDeque(settling.keys)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (schema.isApplicable(id, answers.values)) continue
            // A gate inherits the weight of what it unlocks, so the door is opened
            // before anything less urgent is asked rather than after.
            val weight = out[id] ?: 1
            schema.dependencies.filter { it.character == id }.forEach { dep ->
                if (dep.requiresCharacter !in answers.values.keys) {
                    val seen = out.put(
                        dep.requiresCharacter,
                        maxOf(out[dep.requiresCharacter] ?: 0, weight),
                    )
                    if (seen == null) queue.addLast(dep.requiresCharacter)
                }
            }
        }
        return out
    }

    private fun withGatingQuestions(settling: Set<String>, answers: Answers): Set<String> =
        withGatingQuestions(settling.associateWith { 1 }, answers).keys

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
     * **A cardinality correction, but not the entropy of the split.** Raw gain rewards a
     * character simply for having many states, because more states shatter the set into
     * smaller groups. Cap colour has sixteen and would be chosen as the opening question
     * - and cap colour is the one character every source warns is unreliable, since
     * rain, age and sun all change it.
     *
     * Quinlan's gain ratio is the usual correction, and it was used here, and it did
     * nothing at all. Quinlan divides by the entropy of the split, and that works
     * because his groups contain *classes* whose entropy is measured separately. Here
     * every taxon is its own class, so the entropy remaining in a group is the log of
     * its size - and with that substitution the gain and the split entropy are the same
     * number, algebraically, for every character there is. The ratio was 1.000000 across
     * the board, every question the key ever asked was ordered by [Character.power]
     * alone, and the symptom the correction was added for went away only because cap
     * colour has a low power. Three paragraphs of comment described an adaptive key that
     * was a hand-written list.
     *
     * What is used instead is the intrinsic value of the split: the log of how many
     * distinct answers there are. A sixteen-state character has to be four bits better
     * than a two-state one to be worth asking first, which is the bias that needed
     * removing, and it does not collapse.
     *
     * **Reliability weighting.** DELTA weights characters by how dependably a person can
     * apply them, and the schema's power field is that judgement. A character that is
     * slightly less informative but far easier to get right is the better question.
     */
    private fun askingValue(character: Character, live: List<Taxon>): Double {
        val gain = informationGain(character.id, live)
        if (gain <= 0.0) return 0.0
        val answers = groupsFor(character.id, live).size
        if (answers < 2) return 0.0
        val reliability = (character.power / 5.0) * (character.power / 5.0)
        return (gain / (ln(answers.toDouble()) / LN2)) * reliability
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
            // Not the ones already looked at and given up on. A shared find listed the
            // spore print under "looked at and could not say" and then again under "not
            // recorded", and advice that tells a reader to go and record something they
            // have just told you they tried is not advice.
            .filter { it.id !in answers.notTested }
            // Measurements are excluded for the same reason [nextQuestion] excludes
            // them: this is advice about which question to ask next time, and size is
            // not a question. It is recorded on the entry screen instead.
            .filter { it.kind != Character.Kind.MEASUREMENT }
            .filter { schema.isApplicable(it.id, answers.values) }
            // Gain ratio weighted by power, exactly as nextQuestion scores. Raw
            // information gain was used here and it put "what colour is the cap?" at
            // the top of the advice, for the same reason it was once the first question
            // the key ever asked: sixteen states, so the raw number is large. It is the
            // one character every source calls unreliable, and telling a reader that is
            // what they should have recorded is worse than saying nothing.
            .map { it.id to askingValue(it, live) }
            .filter { it.second > 0.0 }
            // Not filtered to above-average gain the way [nextQuestion] is. That
            // qualification exists to pick one question and it is wrong for a ranked
            // list: it dropped the spore print, which is the single character this app
            // most wants a reader to go back for.
            .sortedByDescending { it.second }
    }

    private companion object {
        const val FULL_MATCH = 1.0
        const val PARTIAL_MATCH = 0.4

        // Heavy, but finite. One wrong tap must cost a taxon its place at the top
        // without erasing it from the list entirely.
        const val MISMATCH = -2.0

        // Lighter than a state mismatch, and it never counts as one. See the scoring
        // of measurements above for why a wrong number must not be able to rule
        // anything out.
        const val SIZE_MISS = -0.8

        // How much corroboration it takes before a row's own average is trusted to
        // stand in for what it does not say. Two.
        const val IMPUTE_DAMPING = 2.0

        /**
         * Characters an old or weathered specimen cannot be trusted on. A cap that has
         * been rained on for a fortnight is flatter, paler and more ragged than the book
         * says, and its gills have taken the colour of whatever fell on them.
         */
        val TIME_WORN = setOf(
            "cap_shape", "cap_margin", "cap_surface", "hymenophore_colour",
            "veil_remnants", "ring",
        )

        /** The two that fall off entirely, so their absence proves nothing. */
        val FALLS_OFF = setOf("veil_remnants", "ring")

        /** The answers that mean "there is none", which is what time produces. */
        val DISAPPEARS = setOf("none", "absent")

        // Still a penalty, because it is still evidence — a third of the usual one, and
        // it never counts as a mismatch, so it cannot take a deadly candidate off the
        // screen on the strength of a fortnight's weather.
        const val WEATHERED_MISMATCH = -0.7

        const val SEASON_BONUS = 0.15
        const val SEASON_PENALTY = -0.15
        const val UNSCORED = " unscored"
        val LN2 = ln(2.0)
    }
}
