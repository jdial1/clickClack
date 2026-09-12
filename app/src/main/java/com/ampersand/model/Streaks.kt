package com.ampersand.model

import java.time.LocalDate

/**
 * Runs of days, counted from what both phones can see.
 *
 * Pulled out of the repository so it can be tested, which is how it was found
 * to be wrong: on the real app past days are loaded without their answers, so
 * the old count only ever kept past days that happened to carry a reaction. A
 * couple who answered every day and never reacted sat on a streak of one.
 */
object Streaks {

    /**
     * Consecutive answered days ending today, or yesterday.
     *
     * A past day counts once it opened -- both of you answered -- which is
     * what the day document's `revealed` flag records. Today counts as soon as
     * either of you has answered, and yesterday is allowed to carry the run,
     * so the streak does not read as broken all morning before you get to it.
     */
    fun run(history: List<DayEntry>, today: DayEntry): Int {
        val answered = history.filter { it.counts() }.mapTo(mutableSetOf()) { it.date }
        if (today.answers.isNotEmpty() || today.isRevealed) answered += today.date
        val recorded = (history + today).associate { it.date to it.recordedRun }

        var day = if (today.date in answered) today.date else today.date.minusDays(1)
        var count = 0
        // The phones only load the last ten weeks, so a walk alone stops at
        // seventy however long the run really is. Each opened day records the
        // run it made, and a day that says so carries everything before it.
        // Taken as the larger of the two, never trusted outright: a day that
        // opened before its phone had loaded history recorded a run that is
        // too short, and the walk overrules it wherever it can see.
        var best = 0
        while (day in answered) {
            recorded[day]?.takeIf { it > 0 }?.let { best = maxOf(best, count + it) }
            count++
            day = day.minusDays(1)
        }
        return maxOf(best, count)
    }

    /**
     * The run as it stood at the end of yesterday, whether or not today is answered.
     *
     * What today *would* make is this plus one, which is the number a milestone
     * is about -- and it must not change the moment one of you answers, or the
     * day would stop being the hundredth halfway through.
     */
    fun before(history: List<DayEntry>, date: LocalDate): Int =
        run(history.filter { it.date < date }, DayEntry(date, Placeholder))

    private val Placeholder = Question("", "", QuestionType.REFLECT, "", Depth(1))

    private fun DayEntry.counts(): Boolean = isRevealed || answers.isNotEmpty()
}
