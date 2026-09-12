package com.ampersand.data

import android.content.Context
import com.ampersand.model.Occasion
import com.ampersand.model.Question
import com.ampersand.model.Quiz
import com.ampersand.model.Taste
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Every question the app knows about, loaded from the bundled decks.
 *
 * The decks are both things at once: a deck is something you sit down and play,
 * and its questions are also the pool the daily question is drawn from. Keeping
 * one source means a question cannot exist in the app and be missing from the
 * pool, which is what happens the moment there are two lists.
 */
class QuestionPool(
    val decks: List<Quiz>,
    /**
     * One small deck per occasion, keyed by [Occasion.id].
     *
     * Kept out of [decks] on purpose: these are the source for a particular
     * day, not something to sit down and play. A Christmas deck listed on the
     * eleventh of March is clutter.
     */
    val occasions: Map<String, Quiz> = emptyMap(),
) {

    val all: List<Question> = decks.flatMap { it.questions }

    private val byId: Map<String, Question> =
        (all + occasions.values.flatMap { it.questions }).associateBy { it.id }

    fun question(id: String?): Question? = id?.let { byId[it] }

    fun deck(id: String?): Quiz? = decks.firstOrNull { it.id == id }

    /**
     * Today's question, chosen by date.
     *
     * Deterministic, and that is the whole requirement: both phones have to land
     * on the same question before either has written anything. Anything drawn at
     * random, or stored by whoever opens the app first, gives the second person
     * a different question.
     *
     * Weekdays draw from the shallow end and Friday to Sunday from the whole
     * pool. Depth exists so the app can keep a tired Tuesday light and put the
     * ones worth a real evening where there is an evening to give them -- and a
     * pool that ignores it will eventually ask what you are most afraid of at
     * 7am on a Monday.
     */
    fun daily(
        date: LocalDate,
        anniversary: LocalDate? = null,
        taste: Taste = Taste.none,
    ): Question {
        require(all.isNotEmpty()) { "no questions loaded" }

        // A day that is not like the others gets its own question. Rotated by
        // year, so the same holiday does not ask the same thing every time.
        Occasion.on(date, anniversary)?.let { occasion ->
            occasions[occasion.id]?.questions?.takeIf { it.isNotEmpty() }?.let { pool ->
                return pool[date.year.mod(pool.size)]
            }
        }

        val weekend = date.dayOfWeek in setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val candidates = all.filter { weekend || it.depth.level <= 2 }.ifEmpty { all }

        // A prime stride walks the list rather than stepping through it in
        // order, so consecutive days are not neighbours in the same deck.
        val draw = if (taste.isEmpty) candidates else tilted(candidates, taste)
        val index = (date.toEpochDay() * 7).mod(draw.size)
        return draw[index]
    }

    /**
     * The pool, with what the tests found leaning on it.
     *
     * Laid out in tiers rather than by repeating each question in place: tier
     * one is every candidate, tier two is every candidate the profile weights
     * at two or more, and so on. Grouping a question's copies together would
     * put them within a few strides of each other and the same question would
     * come round twice in a fortnight; a tier is a whole pool apart.
     *
     * Every question survives tier one whatever the profile says. The tilt
     * changes the odds and never the reachable set -- an app that quietly
     * retires a third of its questions because of a test somebody took in March
     * is a worse app than one that never asked.
     */
    private fun tilted(candidates: List<Question>, taste: Taste): List<Question> {
        val top = candidates.maxOf { taste.weightOf(it) }
        return (1..top).flatMap { tier -> candidates.filter { taste.weightOf(it) >= tier } }
    }

    companion object {
        fun load(context: Context): QuestionPool = QuestionPool(
            decks = QuizJson.loadBundled(context),
            occasions = QuizJson.loadFolder(context, "occasions").associateBy { it.id },
        )
    }
}
