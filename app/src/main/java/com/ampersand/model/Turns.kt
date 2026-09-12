package com.ampersand.model

import java.time.LocalDate

/**
 * The part of a day that belongs to one of you.
 *
 * Everything else in the app is symmetrical -- the same question, the same seal,
 * the same reveal -- which is fair and also means neither of you ever has
 * anything to do that is yours. This is the one thing that is: once today is
 * open, one person picks what tomorrow is about, and the other finds out in the
 * morning along with the question.
 *
 * Whose turn it is comes from the date and the two uids, nothing stored, so
 * both phones agree without asking each other. It alternates day to day, so
 * nobody gets two in a row and nobody is left out for a week.
 */
object Turns {

    /** The uid who picks tomorrow's theme on [date]. */
    fun chooser(date: LocalDate, a: String, b: String): String {
        val (first, second) = if (a <= b) a to b else b to a
        return if (date.toEpochDay().mod(2L) == 0L) first else second
    }

    /**
     * The theme somebody picked yesterday for [today], if they did.
     *
     * Only a pick made by that day's chooser counts. Rules cannot check whose
     * turn it was, so the phones do: a pick written by the wrong person is left
     * where it is and ignored.
     */
    fun themeFor(today: LocalDate, history: List<DayEntry>, a: String, b: String): String? {
        val yesterday = today.minusDays(1)
        val day = history.firstOrNull { it.date == yesterday } ?: return null
        val theme = day.nextTheme ?: return null
        return theme.takeIf { day.nextThemeBy == chooser(yesterday, a, b) }
    }

    /** Whether [uid] can pick tomorrow right now: their turn, today open, nothing picked yet. */
    fun canPick(today: DayEntry, uid: String, partner: String): Boolean =
        today.isRevealed && today.nextTheme == null && chooser(today.date, uid, partner) == uid
}
