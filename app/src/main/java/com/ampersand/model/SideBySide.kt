package com.ampersand.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One person's row in the Us screen's side-by-side table. */
data class Tally(val daysShowedUp: Int, val guessesLanded: Int, val decksFinished: Int)

object SideBySide {

    fun of(state: CoupleState, uid: String): Tally {
        val days = (state.history + state.today).count { day ->
            DayPips.didSomething(uid, day.date, day, emptyList())
        }
        val decks = state.quizRuns.count { it.uid == uid && it.completedAtMillis != null }
        val guesses = if (uid == state.you.uid) state.knowing.youHits else state.knowing.theyHits
        return Tally(daysShowedUp = days, guessesLanded = guesses, decksFinished = decks)
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
