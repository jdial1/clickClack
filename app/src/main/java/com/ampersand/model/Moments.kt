package com.ampersand.model

/** Something from before, brought back on Today. */
sealed interface Moment {
    val day: DayEntry

    /** A recent day you didn't answer. [partnerAnswered] means answering now opens it. */
    data class Missed(override val day: DayEntry, val partnerAnswered: Boolean) : Moment

    /** An opened day from a month or more ago. */
    data class Recall(override val day: DayEntry) : Moment
}

object Moments {

    /** At most one missed day (last week) and one old day to remember, both deterministic by date. */
    fun of(state: CoupleState): List<Moment> {
        val today = state.today.date
        val missed = state.history
            .firstOrNull { it.date >= today.minusDays(MissedDays) && !it.isRevealed && state.you.uid !in it.submitted }
            ?.let { Moment.Missed(it, state.them.uid in it.submitted) }
        val old = state.history.filter { it.isRevealed && it.date <= today.minusDays(RecallAfterDays) }
        val recall = (old.firstOrNull { it.date.dayOfMonth == today.dayOfMonth }
            ?: old.getOrNull(today.toEpochDay().mod(old.size.coerceAtLeast(1))))
            ?.let { Moment.Recall(it) }
        return listOfNotNull(missed, recall)
    }

    private const val MissedDays = 7L
    private const val RecallAfterDays = 28L
}
