package com.ampersand.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Whether one person answered the day's question or finished a deck on a date,
 * for the Shelf's per-person day pips.
 *
 * Past days carry no answers, so `submitted` says who answered and `opened`
 * means both did.
 */
object DayPips {

    /**
     * Whole weeks from the first day either of you did anything to today, capped
     * at [max]. A couple two days in sees one week, not ten mostly empty ones.
     */
    fun weeksActive(state: CoupleState, max: Int = 10, zone: ZoneId = ZoneId.systemDefault()): Int {
        val today = state.today.date
        val days = state.history.filter { it.opened || it.submitted.isNotEmpty() || it.answers.isNotEmpty() }.map { it.date } +
            state.quizRuns.mapNotNull { run -> run.completedAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } }
        val first = days.filter { it <= today }.minOrNull() ?: today
        val span = java.time.temporal.ChronoUnit.DAYS.between(first, today).toInt() + 1
        return ((span + 6) / 7).coerceIn(1, max)
    }

    fun didSomething(
        uid: String,
        date: LocalDate,
        day: DayEntry?,
        quizRuns: List<QuizRun>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (day != null && (day.opened || uid in day.submitted || day.answers.containsKey(uid))) return true
        return quizRuns.any { run ->
            run.uid == uid && run.completedAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } == date
        }
    }
}
