package com.ampersand.model

import java.time.LocalDate

/**
 * Days that are not like the others, worked out the way the question is.
 *
 * Both phones can agree today is the hundredth day in a row as easily as they
 * agree on the question, so the app may as well notice. A milestone day asks
 * one of the deep ones whatever day of the week it falls on, and says why.
 *
 * And the other side of a run: the morning after one ends, the app says so once
 * and plainly. Pretending a sixty-day run never happened is the thing that makes
 * people stop; a guilt screen is the other thing.
 */
object Milestones {

    /** Runs worth marking. Sparse on purpose: a milestone every week is wallpaper. */
    val Runs = listOf(30, 50, 100, 150, 200, 365, 500, 730, 1000)

    /** Runs shorter than this ending is not news. */
    const val WorthMentioning = 7

    /**
     * The run [date] would make, when that is a milestone.
     *
     * Counted from the end of yesterday, so it holds all day: answering does
     * not turn the hundredth day into the hundred-and-first.
     */
    fun on(date: LocalDate, history: List<DayEntry>): Int? =
        (Streaks.before(history, date) + 1).takeIf { it in Runs }

    /**
     * How long the run was that ended with yesterday going unanswered.
     *
     * Only on the first morning after, and only when yesterday really was
     * missed: one of you answering it counts, the same as for the streak.
     */
    fun ended(date: LocalDate, history: List<DayEntry>): Int? {
        val yesterday = date.minusDays(1)
        if (Streaks.before(history, date) > 0) return null
        return Streaks.before(history, yesterday).takeIf { it >= WorthMentioning }
    }

    /** The run said the way somebody would say it out loud. */
    fun headline(run: Int): String = when (run) {
        30 -> "Thirty days in a row."
        50 -> "Fifty days in a row."
        100 -> "A hundred days in a row."
        150 -> "A hundred and fifty days in a row."
        200 -> "Two hundred days in a row."
        365 -> "A whole year, every day."
        500 -> "Five hundred days in a row."
        730 -> "Two whole years, every day."
        1000 -> "A thousand days in a row."
        else -> "$run days in a row."
    }
}
