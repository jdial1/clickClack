package com.ampersand.data

import com.ampersand.model.CoupleState
import com.ampersand.model.Instrument
import com.ampersand.model.InstrumentRun
import com.ampersand.model.QuestionType
import com.ampersand.model.Reaction
import com.ampersand.model.Taste
import kotlin.math.abs

/**
 * Turning what the tests found into what the app asks.
 *
 * Until now the profile instruments were a display case: three sittings, a
 * hundred answers each, and the daily question computed from the calendar and
 * nothing else. This is the part that makes taking them worth the six minutes.
 *
 * A handful of honest rules rather than a model. Each one is a sentence somebody could
 * have said out loud -- "Maya wants time more than you tend to give it, so
 * there is more about that this fortnight" -- and each one carries that
 * sentence with it, because an app that quietly steers what it asks based on a
 * questionnaire is doing something people are right to be wary of, and the only
 * difference between that and a good feature is saying so.
 *
 * Everything here is a pure function of shared state, which it has to be: both
 * phones must land on the same question before either has written anything.
 * Once somebody answers, the day pins its question and the tilt stops mattering
 * until tomorrow.
 */
object Profiles {

    /** Which tags each love style leans toward. */
    private val styleTags = mapOf(
        "eros" to listOf("romance", "touch"),
        "storge" to listOf("friends", "past"),
        "pragma" to listOf("planning", "money"),
        "agape" to listOf("acts"),
    )

    fun of(state: CoupleState): Taste {
        val themes = mutableMapOf<String, Int>()
        val tags = mutableMapOf<String, Int>()
        val types = mutableSetOf<QuestionType>()
        val reasons = mutableListOf<String>()

        fun favour(names: List<String>, by: Int = 1) {
            names.forEach { themes[it] = (themes[it] ?: 1) + by }
        }

        fun favourTags(names: List<String>, by: Int = 1) {
            names.forEach { tags[it] = (tags[it] ?: 1) + by }
        }

        // The week first, then the questionnaires. These say what is alive
        // right now, where the profile tests are taken once a year -- so they
        // weigh more, and they come first in what the Us page reads out.
        lately(state).let { (talked, curious) ->
            if (talked.isNotEmpty()) {
                favour(talked, by = 2)
                reasons += "You kept talking about the " +
                    talked.joinToString(" and ") { it.lowercase() } +
                    " ones lately, so there are more of those."
            }
            if (curious) {
                favour(listOf("Deep"))
                reasons += "One of you asked to say more tonight, so the questions go a " +
                    "little further in."
            }
        }

        // Asked for out loud, so said back first after the week.
        if (state.interests.isNotEmpty()) {
            state.interests.forEach { topic ->
                topic.theme?.let { favour(listOf(it), by = 2) }
                topic.tag?.let { favourTags(listOf(it), by = 2) }
            }
            reasons += "You asked for more about " +
                state.interests.joinToString(" and ") { it.label.lowercase() } + ", so there is more of that."
        }

        shortfall(state)?.let { (language, who, label) ->
            // Each love language's id is also a question tag, so the lean
            // reaches every question about it, wherever the question sits.
            favourTags(listOf(language), by = 2)
            reasons += "$who wants ${label.lowercase()} more than the other of you tends to " +
                "give it, so there is more about that in the pool."
        }

        leadingStyle(state)?.let { (id, label) ->
            favourTags(styleTags[id].orEmpty())
            reasons += "You both lead with ${label.lowercase()}."
        }

        planningGap(state)?.let { reason ->
            favourTags(listOf("planning"))
            reasons += reason
        }

        // How you have been doing at calling each other, which is the one
        // signal that moves week to week rather than once a year.
        val knowing = state.knowing
        if (knowing.scored >= EnoughDays) {
            val share = (knowing.youHits + knowing.theyHits).toFloat() / (knowing.scored * 2)
            when {
                // Either-or, not guess-me. Two options is the easiest call there
                // is; more open guess-me days were more chances to miss, which is
                // the opposite of helping a couple who keep missing.
                share < Struggling -> {
                    types += QuestionType.THIS_OR_THAT
                    reasons += "You have been missing each other lately, so there are more " +
                        "quick either-or days, which are easier to call."
                }
                share > Sharp -> {
                    favour(listOf("Deep"))
                    reasons += "You have been reading each other well, so the questions go " +
                        "a little further in."
                }
            }
        }

        return Taste(themes = themes, tags = tags, types = types, reasons = reasons)
    }

    /**
     * What the last fortnight says, from the two signals a past day carries.
     *
     * The categories of days either of you said came up again, and whether
     * anybody reached for "Say more tonight". How much you wrote would be the
     * third signal, and on the real app it is not available: past days are
     * loaded without their answers, and a read per day to count words is not
     * worth it for a lean.
     */
    private fun lately(state: CoupleState): Pair<List<String>, Boolean> {
        val since = state.today.date.minusDays(Fortnight)
        val recent = state.history.filter { it.date >= since && it.date < state.today.date }
        val talked = recent.filter { it.wasTalkedAbout }
            .map { it.question.category }
            .distinct()
        val curious = recent.any { day -> Reaction.CURIOUS in day.reactions.values }
        return talked to curious
    }

    /**
     * The biggest gap between what one of you wants and what the other gives.
     *
     * Signed, not absolute: being given far more of something than you asked
     * for is not a shortfall, and reporting it as one is a bug this repo has
     * already had once.
     */
    private fun shortfall(state: CoupleState): Triple<String, String, String>? {
        val kit = Instruments.LOVE_LANGUAGES
        val run = state.instrumentRuns.lastOrNull { it.instrumentId == kit.id } ?: return null
        if (!run.isOpen) return null

        val yourFelt = readings(run, kit, state.you.uid, Instruments.FELT) ?: return null
        val yourGiven = readings(run, kit, state.you.uid, Instruments.GIVEN) ?: return null
        val theirFelt = readings(run, kit, state.them.uid, Instruments.FELT) ?: return null
        val theirGiven = readings(run, kit, state.them.uid, Instruments.GIVEN) ?: return null

        var best: Triple<String, String, String>? = null
        var worst = Gap
        kit.subscales.forEachIndexed { i, scale ->
            val youShort = yourFelt[i] - theirGiven[i]
            val theyShort = theirFelt[i] - yourGiven[i]
            val short = maxOf(youShort, theyShort)
            if (short > worst) {
                worst = short
                val who = if (theyShort >= youShort) state.them.name else "You"
                best = Triple(scale.id, who, scale.label)
            }
        }
        return best
    }

    /** The style you *both* lead with, if you happen to lead with the same one. */
    private fun leadingStyle(state: CoupleState): Pair<String, String>? {
        val kit = Instruments.LOVE_ATTITUDES
        val run = state.instrumentRuns.lastOrNull { it.instrumentId == kit.id } ?: return null
        if (!run.isOpen) return null

        val yours = top(run, kit, state.you.uid) ?: return null
        val theirs = top(run, kit, state.them.uid) ?: return null
        return if (yours.first == theirs.first) yours else null
    }

    /**
     * One of you plans and the other improvises, by a wide margin.
     *
     * The JP axis is the only one of the four whose gap has an obvious thing to
     * ask about. A big split on thinking versus feeling is interesting and does
     * not map onto a deck, so it is left alone rather than forced.
     */
    private fun planningGap(state: CoupleState): String? {
        val kit = Instruments.JUNGIAN
        val run = state.instrumentRuns.lastOrNull { it.instrumentId == kit.id } ?: return null
        if (!run.isOpen) return null

        val yours = axis(run, kit, state.you.uid, "JP") ?: return null
        val theirs = axis(run, kit, state.them.uid, "JP") ?: return null
        if (abs(yours - theirs) < AxisGap) return null

        val planner = if (yours < theirs) "You" else state.them.name
        val other = if (yours < theirs) state.them.name else "you"
        return "$planner plan ahead and $other keep options open, so there is more about " +
            "what happens next."
    }

    private fun readings(
        run: InstrumentRun,
        kit: Instrument,
        uid: String,
        pass: Int,
    ): List<Int>? = run.responsesFor(uid)?.let { kit.readings(it, pass).map { r -> r.value } }

    private fun top(run: InstrumentRun, kit: Instrument, uid: String): Pair<String, String>? =
        run.responsesFor(uid)
            ?.let { kit.readings(it) }
            ?.maxByOrNull { it.value }
            ?.let { it.subscale.id to it.subscale.label }

    private fun axis(run: InstrumentRun, kit: Instrument, uid: String, id: String): Int? =
        run.responsesFor(uid)
            ?.let { kit.readings(it) }
            ?.firstOrNull { it.subscale.id == id }
            ?.value

    /** A gap of five on a scale of four to twenty is worth acting on. */
    private const val Gap = 4

    /** Out of 8..40, a sixteen-point split is two people who genuinely differ. */
    private const val AxisGap = 16

    /** How far back "lately" reaches. */
    private const val Fortnight = 14L

    /** Enough scored days that a hit rate means something. */
    private const val EnoughDays = 10

    private const val Struggling = 0.4f
    private const val Sharp = 0.75f
}
