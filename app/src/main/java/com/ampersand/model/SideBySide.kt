package com.ampersand.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The Us screen's table: the same three facts for each of you, in two rows.
 *
 * Counts of things you did, never a score of each other. Guesses landed is the
 * one that compares, and it is already on the screen as a sentence that
 * refuses to declare a winner.
 */
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

/**
 * How far off a date is, the way a person would say it.
 *
 * Never a bare day count past a fortnight: "in 243 days" is arithmetic, "in 8
 * months" is a date.
 */
fun until(from: LocalDate, to: LocalDate): String {
    val days = ChronoUnit.DAYS.between(from, to)
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Tomorrow"
        days < 14L -> "In $days days"
        days < 60L -> "In ${days / 7} weeks"
        // Rounded to the nearest month: eleven May from the thirteenth of
        // September is "in 8 months", not the seven whole months it strictly is.
        else -> Math.round(days / DaysPerMonth).coerceAtLeast(2).let { "In $it months" }
    }
}
