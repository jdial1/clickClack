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
