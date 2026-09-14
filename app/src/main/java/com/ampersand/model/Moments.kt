package com.ampersand.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Something from before, brought back on Today. Tapping [MISSED] answers it; the rest open the day. */
data class Moment(val kind: Kind, val date: LocalDate, val title: String, val text: String) {
    enum class Kind { MISSED, ON_THIS_DAY, LINE_BACK, TALKED, KEPT, RECALL }
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
                    if (state.them.uid in day.submitted) "You missed $weekday's question. $them answered." else "You missed $weekday's question.",
                    day.question.text,
                )
            }

        val old = state.history.filter { it.isRevealed && it.date <= today.minusDays(RecallAfterDays) }
        val onThisDay = old.firstOrNull { it.date.dayOfMonth == today.dayOfMonth }?.let { day ->
            val months = ChronoUnit.MONTHS.between(day.date, today)
            Moment(Moment.Kind.ON_THIS_DAY, day.date, if (months == 1L) "A month ago today" else "$months months ago today", day.question.text)
        }

        val rotating = buildList {
            state.history.filter { it.isRevealed && it.date <= today.minusDays(3) && it.notes.isNotEmpty() }.forEach { day ->
                val (uid, note) = day.notes.entries.first()
                val who = if (uid == state.you.uid) "You" else them
                add(Moment(Moment.Kind.LINE_BACK, day.date, "$who wrote back on ${day.date.format(Short)}", note))
            }
            state.history.filter { it.wasTalkedAbout && it.date <= today.minusDays(7) }.forEach { day ->
                add(Moment(Moment.Kind.TALKED, day.date, "You talked about this on ${day.date.format(Short)}", day.question.text))
            }
            state.kept.forEach { (key, label) ->
                Kept.dateOf(key)?.takeIf { it <= today.minusDays(7) }?.let { date ->
                    add(Moment(Moment.Kind.KEPT, date, "You kept this from ${date.format(Short)}", label))
                }
            }
            old.forEach { day -> add(Moment(Moment.Kind.RECALL, day.date, "Remember ${day.date.format(Short)}?", day.question.text)) }
        }
        val pick = onThisDay ?: rotating.getOrNull(today.toEpochDay().mod(rotating.size.coerceAtLeast(1)))
        return listOfNotNull(missed, pick)
    }

    private const val MissedDays = 7L
    private const val RecallAfterDays = 28L
    private val Weekday: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE")
    private val Short: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d")
}
