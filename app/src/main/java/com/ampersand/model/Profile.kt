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
 * A subject either of you can take out of the daily question.
 *
 * Hidden by either of you means hidden for both: the daily question has to be
 * the same on two phones, and a topic one person would rather not be asked
 * about is not one to ask the other about either. Decks are not filtered --
 * choosing to play one is its own consent.
 */
enum class Topic(val label: String, val detail: String, private val test: (Question) -> Boolean) {
    SPICY("Spicy", "Warm, flirty questions about wanting each other.", { it.category == "Spicy" }),
    ROMANTIC("Romantic", "Dates, love letters and how you fell for each other.", { it.category == "Romantic" }),
    MONEY("Money", "Saving, spending and what you would do with more.", { "money" in it.tags }),
    FAMILY("Family and upbringing", "How you were raised and the families you come from.", { "family" in it.tags }),
    PAST("The past", "Old memories, old choices and how you got here.", { "past" in it.tags }),
    WORK("Work", "Jobs, ambitions and what a working week does to you.", { "work" in it.tags }),
    ;

    fun matches(question: Question): Boolean = test(question)
}

/** A date worth a countdown: a birthday, a first date, the day you moved in. */
data class SpecialDate(
    val id: String,
    val label: String,
    val date: LocalDate,
)

/** One line on the Us screen's dates: what, when it next comes round, and since when. */
data class UpcomingDate(val label: String, val next: LocalDate, val since: LocalDate)

object Dates {

    /** The next time [date]'s month and day come round, today included. */
    fun nextOccurrence(date: LocalDate, today: LocalDate): LocalDate {
        val md = MonthDay.from(date)
        val thisYear = md.atYear(today.year)
        return if (thisYear < today) md.atYear(today.year + 1) else thisYear
    }

    /**
     * Every yearly date the two of you have, soonest first: the day you started,
     * your wedding, both birthdays and anything you added.
     */
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
