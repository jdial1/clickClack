package com.ampersand.model

/** "1 day", "3 days": a count and its noun, for nouns whose plural just adds an s. */
fun counted(n: Int, noun: String): String = "$n ${if (n == 1) noun else "${noun}s"}"

/** "a year ago", "3 months ago", "a few weeks ago": how long before [now] [then] was. */
fun ago(then: java.time.LocalDate, now: java.time.LocalDate): String {
    val months = java.time.temporal.ChronoUnit.MONTHS.between(then, now)
    return when {
        months >= 24 -> "${months / 12} years ago"
        months >= 12 -> "a year ago"
        months >= 2 -> "$months months ago"
        months == 1L -> "a month ago"
        else -> "a few weeks ago"
    }
}
