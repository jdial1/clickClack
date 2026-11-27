package com.ampersand.ui.theme

import java.time.format.DateTimeFormatter

/**
 * The date formats the app shows. A screen picks one of these by what the date
 * is for, never its own pattern, so the same kind of date reads the same everywhere.
 */
object DateFormats {
    /** "Friday, November 27": the day a question belongs to. */
    val Weekday: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

    /** "November 27, 2026": a date that may be in another year. */
    val Full: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    /** "November 27": a date this year. */
    val MonthDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d")

    /** "Nov 27": a date in a tight row. */
    val Short: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

    /** "Nov": a month in a picker. */
    val MonthShort: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
}
