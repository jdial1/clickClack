package com.ampersand.model

import java.time.LocalDate
import java.time.MonthDay

/** What a person may say about themselves. Optional, and never used to pick questions. */
val GenderChoices = listOf("Woman", "Man", "Non-binary", "Prefer to self-describe", "Prefer not to say")

enum class RelationshipStatus(val label: String) {
    DATING("Dating"),
    LIVING_TOGETHER("Living together"),
    ENGAGED("Engaged"),
    MARRIED("Married"),
    PARTNERS("Partners"),
}

/**
 * A subject either of you can hide from the daily question.
 * Hidden by either means hidden for both, so both phones draw the same question. Decks are not filtered.
 */
enum class Topic(val label: String, val detail: String, val theme: String? = null, val tag: String? = null) {
    SPICY("Spicy", "Warm, flirty questions about wanting each other.", theme = "Spicy"),
    ROMANTIC("Romantic", "Dates, love letters and how you fell for each other.", theme = "Romantic"),
    SPORTS("Sports", "Teams, games and trips to see them.", tag = "sports"),
    HOROSCOPES("Horoscopes", "Your signs, from both birthdays, and whether any of it is true.", tag = "astrology"),
    MONEY("Money", "Saving, spending and big plans.", tag = "money"),
    FAMILY("Family and upbringing", "How you were raised and the families you come from.", tag = "family"),
    PAST("The past", "Old memories, old choices and how you got here.", tag = "past"),
    WORK("Work", "Jobs, goals and work life.", tag = "work"),
    ;

    fun matches(question: Question): Boolean =
        (theme != null && question.category == theme) || (tag != null && tag in question.tags)
}

/** What kind of day a special date is. Each has its own few questions for the day itself. */
enum class DateKind(val id: String, val label: String) {
    FIRST_DATE("first-date", "First date"),
    ENGAGEMENT("engagement", "Engagement"),
    WEDDING("wedding", "Wedding"),
    MOVED_IN("moved-in", "Moved in"),
    BIRTHDAY("birthday", "Birthday"),
    PET("pet", "Pet"),
    TRIP("trip", "Trip"),
    REMEMBERING("remembering", "Remembering someone"),
    OTHER("other", "Something else"),
}

/** A date worth a countdown: a birthday, a first date, the day you moved in. */
data class SpecialDate(
    val id: String,
    val label: String,
    val date: LocalDate,
    val kind: DateKind = DateKind.OTHER,
)

/** One row in the Us screen's dates: label, next occurrence, original date. */
data class UpcomingDate(val label: String, val next: LocalDate, val since: LocalDate)

object Dates {

    /** The next time [date]'s month and day come around, today included. */
    fun nextOccurrence(date: LocalDate, today: LocalDate): LocalDate {
        val md = MonthDay.from(date)
        val thisYear = md.atYear(today.year)
        return if (thisYear < today) md.atYear(today.year + 1) else thisYear
    }

    /**
     * The kinds of date that fall on [today]: added dates, both birthdays and
     * the wedding. The first one gets the day's question.
     */
    fun kindsOn(
        today: LocalDate,
        added: List<SpecialDate>,
        birthdays: List<LocalDate?>,
        marriedOn: LocalDate?,
    ): List<DateKind> {
        fun on(d: LocalDate?) = d != null && MonthDay.from(d) == MonthDay.from(today)
        return added.filter { on(it.date) }.map { it.kind } +
            listOfNotNull(DateKind.WEDDING.takeIf { on(marriedOn) }) +
            birthdays.filter(::on).map { DateKind.BIRTHDAY }
    }

    /** Every yearly date (anniversary, wedding, birthdays, added dates), soonest first. */
    fun upcoming(state: CoupleState): List<UpcomingDate> {
        val today = state.today.date
        val entries = buildList {
            state.anniversary?.let { add("Your anniversary" to it) }
            state.marriedOn?.let { add("Your wedding anniversary" to it) }
            state.you.birthday?.let { add("Your birthday" to it) }
            state.them.birthday?.let { add("${state.them.name}'s birthday" to it) }
            state.specialDates.forEach { add(it.label to it.date) }
        }
        return entries
            .map { (label, date) -> UpcomingDate(label, nextOccurrence(date, today), date) }
            .sortedBy { it.next }
    }
}
