package com.ampersand.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The Us screen's "You two" numbers, counted for the pair and never per person:
 * a per-person tally invites "I show up more than you". See [Knowing.landed].
 */
data class Tally(val daysTogether: Int, val decksTogether: Int, val questionsWritten: Int)

object SideBySide {

    fun of(state: CoupleState): Tally {
        val days = (state.history + state.today).count { day ->
            DayPips.didSomething(state.you.uid, day.date, day, emptyList()) &&
                DayPips.didSomething(state.them.uid, day.date, day, emptyList())
        }
        val finished = state.quizRuns.filter { it.completedAtMillis != null }
        val decks = finished.groupBy { it.quizId }.count { (_, runs) -> runs.map { it.uid }.toSet().size >= 2 }
        return Tally(daysTogether = days, decksTogether = decks, questionsWritten = state.written.values.sum())
    }
}

private const val DaysPerMonth = 30.44

/** Relative time to a date: days under two weeks, then weeks, then months. */
fun until(from: LocalDate, to: LocalDate): String {
    val days = ChronoUnit.DAYS.between(from, to)
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Tomorrow"
        days < 14L -> "In $days days"
        days < 60L -> "In ${days / 7} weeks"
        // Rounded to the nearest month, not truncated.
        else -> Math.round(days / DaysPerMonth).coerceAtLeast(2).let { "In $it months" }
    }
}
