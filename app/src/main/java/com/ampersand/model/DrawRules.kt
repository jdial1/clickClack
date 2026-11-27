package com.ampersand.model

import java.time.LocalDate

/*
 * Rules the daily draw reads from shared history. Both phones compute them from
 * the same state, so they land on the same question before either has written.
 */

/**
 * Which of the first week's questions is next: the count of days you have both opened,
 * or null after seven, or once a sculpture has sealed (history is a window, so a couple
 * back from a long gap must not start over).
 */
fun firstWeekDay(history: List<DayEntry>, sealedSculpture: Boolean): Int? {
    if (sealedSculpture) return null
    return history.count { it.isRevealed }.takeIf { it < 7 }
}

/**
 * The Bad-Month Test as a rule: after [QuietDays] days that neither of you
 * answered, the next question is a light one. Only for couples who have
 * answered before.
 */
fun gentleReturn(today: LocalDate, history: List<DayEntry>): Boolean {
    if (history.none { it.submitted.isNotEmpty() || it.isRevealed }) return false
    return (1L..QuietDays).all { back ->
        history.none { it.date == today.minusDays(back) && (it.submitted.isNotEmpty() || it.isRevealed) }
    }
}

const val QuietDays = 3L
