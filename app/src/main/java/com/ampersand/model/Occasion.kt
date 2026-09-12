package com.ampersand.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * A day that is not like the others.
 *
 * Holidays, solstices and the day the two of you started. On these the daily
 * question is drawn from that occasion's own small deck instead of the general
 * pool, because "what did you need this week" on Christmas morning is a wasted
 * question and "what are you dreading about today" is the one worth asking.
 *
 * [id] matches a file in `assets/occasions/`, so adding an occasion is a JSON
 * file and one line here.
 *
 * No icons. There were emoji; no icon set has a Juneteenth or a Presidents' Day
 * mark, and twenty occasions illustrated by whatever happened to be close is
 * worse than twenty occasions named plainly.
 *
 * **Declaration order is precedence**, because occasions collide: Father's Day
 * 2026 falls on the twenty-first of June, which is also the summer solstice.
 * The named days people actually mark come first and the astronomical markers
 * last, so nobody gets asked about the long evenings on Father's Day.
 */
enum class Occasion(val id: String, val label: String) {
    // Named days first.
    NEW_YEAR("new-year", "New Year's Day"),
    MLK_DAY("mlk-day", "Martin Luther King Jr. Day"),
    VALENTINES("valentines", "Valentine's Day"),
    PRESIDENTS_DAY("presidents-day", "Presidents' Day"),
    MOTHERS_DAY("mothers-day", "Mother's Day"),
    MEMORIAL_DAY("memorial-day", "Memorial Day"),
    JUNETEENTH("juneteenth", "Juneteenth"),
    FATHERS_DAY("fathers-day", "Father's Day"),
    INDEPENDENCE_DAY("independence-day", "Independence Day"),
    LABOR_DAY("labor-day", "Labor Day"),
    COLUMBUS_DAY("columbus-day", "Columbus Day"),
    HALLOWEEN("halloween", "Halloween"),
    VETERANS_DAY("veterans-day", "Veterans Day"),
    THANKSGIVING("thanksgiving", "Thanksgiving"),
    CHRISTMAS("christmas", "Christmas Day"),
    NEW_YEARS_EVE("new-years-eve", "New Year's Eve"),

    // Astronomical markers last: they lose every collision.
    SPRING("spring", "First day of spring"),
    SUMMER("summer", "Summer solstice"),
    AUTUMN("autumn", "First day of fall"),
    WINTER("winter", "Winter solstice"),

    /**
     * The day the two of you started, which only exists once someone has told
     * the app when that was.
     */
    ANNIVERSARY("anniversary", "Your anniversary"),
    ;

    companion object {

        /**
         * Which occasion [date] is, if any.
         *
         * The anniversary wins over a public holiday. If you got together on the
         * fourth of July, the fourth of July is your anniversary.
         */
        fun on(date: LocalDate, anniversary: LocalDate? = null): Occasion? {
            if (anniversary != null &&
                date.month == anniversary.month &&
                date.dayOfMonth == anniversary.dayOfMonth &&
                date > anniversary
            ) {
                return ANNIVERSARY
            }
            return entries.firstOrNull { it != ANNIVERSARY && it.falls(date) }
        }

        /** The next occasion at or after [date], for showing what is coming. */
        fun next(date: LocalDate, anniversary: LocalDate? = null): Pair<Occasion, LocalDate>? =
            (0..370L).firstNotNullOfOrNull { offset ->
                val day = date.plusDays(offset)
                on(day, anniversary)?.let { it to day }
            }
    }

    /**
     * Whether this occasion falls on [date].
     *
     * The solstices and equinoxes are fixed dates rather than computed
     * astronomically. The real ones wander by a day either side and depend on
     * where you are standing; an almanac calculation would be a lot of code for
     * a question about whether you like the dark evenings.
     */
    fun falls(date: LocalDate): Boolean = when (this) {
        NEW_YEAR -> date.month == Month.JANUARY && date.dayOfMonth == 1
        MLK_DAY -> date.isNth(3, DayOfWeek.MONDAY, Month.JANUARY)
        VALENTINES -> date.month == Month.FEBRUARY && date.dayOfMonth == 14
        PRESIDENTS_DAY -> date.isNth(3, DayOfWeek.MONDAY, Month.FEBRUARY)
        SPRING -> date.month == Month.MARCH && date.dayOfMonth == 20
        MOTHERS_DAY -> date.isNth(2, DayOfWeek.SUNDAY, Month.MAY)
        MEMORIAL_DAY -> date.isLast(DayOfWeek.MONDAY, Month.MAY)
        SUMMER -> date.month == Month.JUNE && date.dayOfMonth == 21
        FATHERS_DAY -> date.isNth(3, DayOfWeek.SUNDAY, Month.JUNE)
        JUNETEENTH -> date.month == Month.JUNE && date.dayOfMonth == 19
        INDEPENDENCE_DAY -> date.month == Month.JULY && date.dayOfMonth == 4
        LABOR_DAY -> date.isNth(1, DayOfWeek.MONDAY, Month.SEPTEMBER)
        AUTUMN -> date.month == Month.SEPTEMBER && date.dayOfMonth == 22
        COLUMBUS_DAY -> date.isNth(2, DayOfWeek.MONDAY, Month.OCTOBER)
        HALLOWEEN -> date.month == Month.OCTOBER && date.dayOfMonth == 31
        VETERANS_DAY -> date.month == Month.NOVEMBER && date.dayOfMonth == 11
        THANKSGIVING -> date.isNth(4, DayOfWeek.THURSDAY, Month.NOVEMBER)
        WINTER -> date.month == Month.DECEMBER && date.dayOfMonth == 21
        CHRISTMAS -> date.month == Month.DECEMBER && date.dayOfMonth == 25
        NEW_YEARS_EVE -> date.month == Month.DECEMBER && date.dayOfMonth == 31
        // Only ever matched against the couple's own date, in `on`.
        ANNIVERSARY -> false
    }
}

private fun LocalDate.isNth(n: Int, day: DayOfWeek, month: Month): Boolean =
    this.month == month && this == withDayOfMonth(1).with(TemporalAdjusters.dayOfWeekInMonth(n, day))

private fun LocalDate.isLast(day: DayOfWeek, month: Month): Boolean =
    this.month == month && this == with(TemporalAdjusters.lastInMonth(day))

/**
 * How long the two of you have been going, said the way a person would.
 *
 * Years and months, not days. "1,247 days together" is a number from a
 * spreadsheet; nobody has ever said it out loud.
 */
fun timeTogether(since: LocalDate, now: LocalDate): String {
    if (since > now) return "Starting today"
    val period = java.time.Period.between(since, now)
    val years = period.years
    val months = period.months
    val days = period.days

    return when {
        years == 0 && months == 0 && days == 0 -> "Since this morning"
        years == 0 && months == 0 -> "$days ${plural(days, "day")}"
        years == 0 -> "$months ${plural(months, "month")}"
        months == 0 -> "$years ${plural(years, "year")}"
        else -> "$years ${plural(years, "year")}, $months ${plural(months, "month")}"
    }
}

private fun plural(n: Int, word: String) = if (n == 1) word else "${word}s"
