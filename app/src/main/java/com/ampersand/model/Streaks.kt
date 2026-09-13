package com.ampersand.model

import java.time.LocalDate

/** Streak counting. Past days load without answers, so they count via `revealed`/`submitted`. */
object Streaks {

    /**
     * Consecutive answered days ending today, or yesterday if today is not answered yet.
     * Today counts once either of you has answered.
     */
    fun run(history: List<DayEntry>, today: DayEntry): Int {
        val answered = history.filter { it.counts() }.mapTo(mutableSetOf()) { it.date }
        if (today.answers.isNotEmpty() || today.isRevealed) answered += today.date
        val recorded = (history + today).associate { it.date to it.recordedRun }

        var day = if (today.date in answered) today.date else today.date.minusDays(1)
        var count = 0
        // Only ~10 weeks of history load, so combine the walk with each day's recordedRun.
        // Take the max: a recordedRun written before history loaded can be too short.
        var best = 0
        while (day in answered) {
            recorded[day]?.takeIf { it > 0 }?.let { best = maxOf(best, count + it) }
            count++
            day = day.minusDays(1)
        }
        return maxOf(best, count)
    }

    /** The run as of the end of the day before [date], unaffected by answering today. */
    fun before(history: List<DayEntry>, date: LocalDate): Int =
        run(history.filter { it.date < date }, DayEntry(date, Placeholder))

    private val Placeholder = Question("", "", QuestionType.REFLECT, "", Depth(1))

    private fun DayEntry.counts(): Boolean = isRevealed || answers.isNotEmpty()
}
