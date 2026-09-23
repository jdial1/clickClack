package com.ampersand.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Something from before, brought back on Today. Tapping [MISSED] answers it; the rest open the day.
 * [day] is set when the moment can be shown in full, with both answers on the card.
 */
data class Moment(val kind: Kind, val date: LocalDate, val title: String, val text: String, val day: DayEntry? = null) {
    enum class Kind { MISSED, ON_THIS_DAY, LINE_BACK, TALKED, KEPT, RECALL }
}

/** How far back a memory has to be before it is brought forward. */
object Memories {
    /** On this day and old-day recalls: a season at least. A month is not a memory yet. */
    const val MinDays = 90L

    /** Lines back, talked-about days and kept days: a month at least. */
    const val RecentDays = 30L

    /** Months back to fetch today's date from, beyond the loaded history. */
    val MonthsBack = listOf(3L, 6L, 12L, 24L, 36L, 48L)

    /** "A year ago today", "2 years ago today", "6 months ago today". */
    fun ago(date: LocalDate, today: LocalDate): String {
        val months = ChronoUnit.MONTHS.between(date, today)
        return when {
            months == 12L -> "A year ago today"
            months % 12 == 0L -> "${months / 12} years ago today"
            else -> "$months months ago today"
        }
    }
}

object Moments {

    /**
     * A missed day from this week, if any, plus one moment that rotates daily
     * (deterministic by date): a month-day anniversary first, otherwise a line
     * one of you wrote back, a day you talked about, something you kept, or an old day.
     */
    fun of(state: CoupleState): List<Moment> {
        val today = state.today.date
        val them = state.them.name

        val missed = state.history
            .firstOrNull { it.date >= today.minusDays(MissedDays) && !it.isRevealed && state.you.uid !in it.submitted }
            ?.let { day ->
                val weekday = day.date.format(Weekday)
                Moment(
                    Moment.Kind.MISSED, day.date,
                    // Name the pause, not the loss, and never who else answered.
                    "$weekday's question is still open.",
                    day.question.text,
                )
            }

        val old = state.history.filter { it.isRevealed && it.date <= today.minusDays(Memories.MinDays) }
        val onThisDay = state.onThisDay().firstOrNull()?.let { day ->
            Moment(Moment.Kind.ON_THIS_DAY, day.date, Memories.ago(day.date, today), day.question.text, day.takeIf { it.answers.size >= 2 })
        }

        val rotating = buildList {
            state.history.filter { it.isRevealed && it.date <= today.minusDays(Memories.RecentDays) && it.notes.isNotEmpty() }.forEach { day ->
                val (uid, note) = day.notes.entries.first()
                val who = if (uid == state.you.uid) "You" else them
                add(Moment(Moment.Kind.LINE_BACK, day.date, "$who wrote back on ${day.date.format(Short)}", note))
            }
            state.history.filter { it.wasTalkedAbout && it.date <= today.minusDays(Memories.RecentDays) }.forEach { day ->
                add(Moment(Moment.Kind.TALKED, day.date, "You talked about this on ${day.date.format(Short)}", day.question.text))
            }
            state.kept.forEach { (key, label) ->
                Kept.dateOf(key)?.takeIf { it <= today.minusDays(Memories.RecentDays) }?.let { date ->
                    add(Moment(Moment.Kind.KEPT, date, "You kept this from ${date.format(Short)}", label))
                }
            }
            old.forEach { day -> add(Moment(Moment.Kind.RECALL, day.date, "Remember ${day.date.format(Short)}?", day.question.text)) }
        }
        val pick = onThisDay ?: rotating.getOrNull(today.toEpochDay().mod(rotating.size.coerceAtLeast(1)))
        return listOfNotNull(missed, pick)
    }

    private const val MissedDays = 7L
    private val Weekday: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE")
    private val Short: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d")
}
