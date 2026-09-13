package com.ampersand.model

import java.time.LocalDate

/**
 * Who picks tomorrow's theme once today is open.
 * Alternates daily, computed from the date and both uids so both phones agree with nothing stored.
 */
object Turns {

    /** The uid who picks tomorrow's theme on [date]. */
    fun chooser(date: LocalDate, a: String, b: String): String {
        val (first, second) = if (a <= b) a to b else b to a
        return if (date.toEpochDay().mod(2L) == 0L) first else second
    }

    /**
     * The theme picked yesterday for [today], if any.
     * Rules cannot check whose turn it was, so picks by the wrong person are ignored here.
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
