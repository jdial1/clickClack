package com.ampersand.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Whether one person turned up on a given day: answered the day's question, or
 * finished a deck.
 *
 * The Shelf's ten weeks draw one pip per person per day, so a day only one of
 * you managed looks different from a day you both did. A past day carries no
 * answers, so the day's `submitted` list says who answered, and a day that
 * opened means both did.
 *
 * A partner's deck only counts once that deck has opened: their run is
 * unreadable until then, which is the seal doing its job.
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
