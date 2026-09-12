package com.ampersand.model

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

        var day = if (today.date in answered) today.date else today.date.minusDays(1)
        var count = 0
        while (day in answered) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    private fun DayEntry.counts(): Boolean = isRevealed || answers.isNotEmpty()
}
