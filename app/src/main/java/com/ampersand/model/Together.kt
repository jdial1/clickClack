package com.ampersand.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A relationship milestone on a given day: what to call it and which question set it draws from. */
data class Together(val title: String, val detail: String, val tier: Tier) {

    enum class Tier(val id: String) {
        /** The early ones: 100 days, six months, 500 and 1,000 days. */
        EARLY("milestone-early"),

        /** Named anniversaries: 1, 5, 10, 15... years. */
        BIG("milestone-big"),
    }

    companion object {
        /** Traditional anniversary names for the years worth a bigger celebration. */
        val Named = mapOf(
            1 to "paper", 5 to "wood", 10 to "tin", 15 to "crystal", 20 to "china", 25 to "silver",
            30 to "pearl", 35 to "coral", 40 to "ruby", 45 to "sapphire", 50 to "gold", 60 to "diamond",
        )

        private val DayMarks = listOf(100L, 500L, 1000L, 2000L, 5000L, 10000L)

        /** The milestone on [today], if any. A named wedding year wins over a named year together. */
        fun on(today: LocalDate, anniversary: LocalDate?, marriedOn: LocalDate?): Together? {
            fun named(since: LocalDate?, what: String): Together? {
                since ?: return null
                val years = ChronoUnit.YEARS.between(since, today).toInt()
                if (since.plusYears(years.toLong()) != today) return null
                val name = Named[years] ?: return null
                return Together("$years ${if (years == 1) "year" else "years"} $what", "The $name anniversary", Tier.BIG)
            }
            named(marriedOn, "married")?.let { return it }
            named(anniversary, "together")?.let { return it }

            anniversary ?: return null
            if (anniversary.plusMonths(6) == today) return Together("Six months together", "Half a year of you two", Tier.EARLY)
            val days = ChronoUnit.DAYS.between(anniversary, today)
            if (days in DayMarks) return Together("${"%,d".format(days)} days together", "Every one of them counted", Tier.EARLY)
            return null
        }
    }
}
