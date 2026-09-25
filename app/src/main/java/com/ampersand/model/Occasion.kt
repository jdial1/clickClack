package com.ampersand.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * A holiday, solstice or anniversary whose daily question comes from its own deck.
 *
 * [id] matches a file in `assets/occasions/`.
 * Declaration order is precedence when occasions share a date (e.g. Father's Day
 * and the summer solstice), so named days come before astronomical ones.
 */
enum class Occasion(
    val id: String,
    val label: String,
    /** States where this occasion applies. Empty means everywhere. */
    val states: Set<String> = emptySet(),
) {
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
    LEAP_DAY("leap-day", "Leap day"),

    // State-specific days, only for couples whose home ZIP is there. After national ones.
    MARDI_GRAS("mardi-gras", "Mardi Gras", setOf("LA", "AL", "MS")),
    TEXAS_INDEPENDENCE("texas-independence", "Texas Independence Day", setOf("TX")),
    CESAR_CHAVEZ("cesar-chavez", "Cesar Chavez Day", setOf("CA", "CO", "TX", "AZ", "NM")),
    PATRIOTS_DAY("patriots-day", "Patriots' Day", setOf("MA", "ME")),
    LEI_DAY("lei-day", "Lei Day", setOf("HI")),
    KENTUCKY_DERBY("kentucky-derby", "Kentucky Derby Day", setOf("KY")),
    KAMEHAMEHA_DAY("kamehameha-day", "King Kamehameha Day", setOf("HI")),
    PIONEER_DAY("pioneer-day", "Pioneer Day", setOf("UT")),
    ALASKA_DAY("alaska-day", "Alaska Day", setOf("AK")),

    // Astronomical markers last: they lose every collision.
    SPRING("spring", "First day of spring"),
    SUMMER("summer", "Summer solstice"),
    AUTUMN("autumn", "First day of fall"),
    WINTER("winter", "Winter solstice"),

    /** The day the two of you started, once it is set. */
    ANNIVERSARY("anniversary", "Your anniversary"),
    ;

    companion object {

        /** Which occasion [date] is, if any. The anniversary wins over a holiday. */
        fun on(date: LocalDate, anniversary: LocalDate? = null, state: String? = null): Occasion? {
            if (anniversary != null &&
                date.month == anniversary.month &&
                date.dayOfMonth == anniversary.dayOfMonth &&
                date > anniversary
            ) {
                return ANNIVERSARY
            }
            return entries.firstOrNull {
                it != ANNIVERSARY && (it.states.isEmpty() || state in it.states) && it.falls(date)
            }
        }

        /** The next occasion at or after [date], for showing what is coming. */
        fun next(date: LocalDate, anniversary: LocalDate? = null, state: String? = null): Pair<Occasion, LocalDate>? =
            (0..370L).firstNotNullOfOrNull { offset ->
                val day = date.plusDays(offset)
                on(day, anniversary, state)?.let { it to day }
            }

        /** Easter Sunday, by the anonymous Gregorian algorithm. */
        fun easter(year: Int): LocalDate {
            val a = year % 19
            val b = year / 100
            val c = year % 100
            val d = b / 4
            val e = b % 4
            val f = (b + 8) / 25
            val g = (b - f + 1) / 3
            val h = (19 * a + b - d - g + 15) % 30
            val i = c / 4
            val k = c % 4
            val l = (32 + 2 * e + 2 * i - h - k) % 7
            val m = (a + 11 * h + 22 * l) / 451
            val month = (h + l - 7 * m + 114) / 31
            val day = ((h + l - 7 * m + 114) % 31) + 1
            return LocalDate.of(year, month, day)
        }
    }

    /** Whether this occasion falls on [date]. Solstices and equinoxes use fixed dates. */
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
        LEAP_DAY -> date.month == Month.FEBRUARY && date.dayOfMonth == 29
        // Fat Tuesday: 47 days before Easter.
        MARDI_GRAS -> date == easter(date.year).minusDays(47)
        TEXAS_INDEPENDENCE -> date.month == Month.MARCH && date.dayOfMonth == 2
        CESAR_CHAVEZ -> date.month == Month.MARCH && date.dayOfMonth == 31
        PATRIOTS_DAY -> date.isNth(3, DayOfWeek.MONDAY, Month.APRIL)
        LEI_DAY -> date.month == Month.MAY && date.dayOfMonth == 1
        KENTUCKY_DERBY -> date.isNth(1, DayOfWeek.SATURDAY, Month.MAY)
        KAMEHAMEHA_DAY -> date.month == Month.JUNE && date.dayOfMonth == 11
        PIONEER_DAY -> date.month == Month.JULY && date.dayOfMonth == 24
        ALASKA_DAY -> date.month == Month.OCTOBER && date.dayOfMonth == 18
        // Only ever matched against the couple's own date, in `on`.
        ANNIVERSARY -> false
    }
}

private fun LocalDate.isNth(n: Int, day: DayOfWeek, month: Month): Boolean =
    this.month == month && this == withDayOfMonth(1).with(TemporalAdjusters.dayOfWeekInMonth(n, day))

private fun LocalDate.isLast(day: DayOfWeek, month: Month): Boolean =
    this.month == month && this == with(TemporalAdjusters.lastInMonth(day))

/** Time together in years and months (days only under a month). */
fun timeTogether(since: LocalDate, now: LocalDate): String {
    if (since > now) return "Starting today"
    val period = java.time.Period.between(since, now)
    val years = period.years
    val months = period.months
    val days = period.days

    return when {
        years == 0 && months == 0 && days == 0 -> "Starting today"
        years == 0 && months == 0 -> "$days ${plural(days, "day")}"
        years == 0 -> "$months ${plural(months, "month")}"
        months == 0 -> "$years ${plural(years, "year")}"
        else -> "$years ${plural(years, "year")}, $months ${plural(months, "month")}"
    }
}

private fun plural(n: Int, word: String) = if (n == 1) word else "${word}s"
