package com.ampersand.model

import java.time.LocalDate

/** Streak milestones and ended-run notices, computed the same on both phones. */
object Milestones {

    /** Run lengths that count as milestones. */
    val Runs = listOf(30, 50, 100, 150, 200, 365, 500, 730, 1000)

    /** Ended runs shorter than this are not mentioned. */
    const val WorthMentioning = 7

    /**
     * The run [date] would make, when that is a milestone.
     * Counted from the end of yesterday, so it holds all day.
     */
    fun on(date: LocalDate, history: List<DayEntry>): Int? =
        (Streaks.before(history, date) + 1).takeIf { it in Runs }

    /**
     * Length of the run that ended with yesterday unanswered, on the first morning
     * after only. One of you answering counts, as for the streak.
     */
    fun ended(date: LocalDate, history: List<DayEntry>): Int? {
        val yesterday = date.minusDays(1)
        if (Streaks.before(history, date) > 0) return null
        return Streaks.before(history, yesterday).takeIf { it >= WorthMentioning }
    }

    /** Headline for a milestone run. Spelled out so it starts with a letter. */
    fun headline(run: Int): String = when (run) {
        30 -> "Thirty days in a row."
        50 -> "Fifty days in a row."
        100 -> "A hundred days in a row."
        150 -> "A hundred and fifty days in a row."
        200 -> "Two hundred days in a row."
        365 -> "One year in a row."
        500 -> "Five hundred days in a row."
        730 -> "Two years in a row."
        1000 -> "A thousand days in a row."
        else -> "$run days in a row."
    }
}
