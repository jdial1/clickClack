package com.ampersand.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * A question one of you wrote.
 *
 * The pool is 182 questions, which is about six months at one a day, and every
 * one of them was written by a stranger. A wildcard is the only content in the
 * app that came from inside the relationship -- and the only one that can ask
 * about the thing that actually happened this week.
 *
 * Deliberately three types and no more. A wildcard is written on a phone, in a
 * minute, usually about something specific, and a form offering eight controls
 * is a form nobody finishes: say something, guess me, or pick a side.
 */
data class Wildcard(
    val id: String,
    val text: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val byUid: String,
    val byName: String = "",
    val createdAtMillis: Long = 0L,
    /** The day it was asked, once it has been. Null while it is still waiting. */
    val usedOn: LocalDate? = null,
) {
    /**
     * As a question the rest of the app can ask.
     *
     * The id carries the [PREFIX] so the day document can pin it the same way
     * it pins a pool question, and the repository can tell the two apart when
     * it reads the day back.
     *
     * Depth 2 always: a wildcard is by definition about the two of you, which
     * is never small talk and is not the app's business to call heavy.
     */
    fun asQuestion(): Question = Question(
        id = "$PREFIX$id",
        text = text,
        type = type,
        category = if (byName.isBlank()) "Yours" else "$byName asked",
        depth = Depth(2),
        options = options,
    )

    companion object {
        const val PREFIX = "wc_"

        /** The types someone can actually write on a phone in under a minute. */
        val WRITABLE = listOf(
            QuestionType.REFLECT,
            QuestionType.GUESS_ME,
            QuestionType.THIS_OR_THAT,
        )

        fun isWildcard(questionId: String?): Boolean = questionId?.startsWith(PREFIX) == true

        fun idOf(questionId: String): String = questionId.removePrefix(PREFIX)
    }
}

/**
 * How often each of you has called the other, for as long as you have played.
 *
 * The per-day score has always existed and has always evaporated: you saw
 * "nailed it" on Tuesday and by Friday it was gone. This is the same fact kept.
 *
 * Stored as one document per month rather than a running counter, because a
 * counter has to be incremented exactly once and two phones both notice a day
 * opening at the same moment. A month document holds a map keyed by day, so
 * writing the same day twice writes the same value twice and the total is
 * unchanged -- the arithmetic is done here, on what was read, rather than
 * trusted to a sequence of increments nobody can replay.
 */
data class Knowing(
    val months: List<KnowingMonth> = emptyList(),
) {
    val youHits: Int get() = months.sumOf { it.youHits }
    val theyHits: Int get() = months.sumOf { it.theyHits }
    val scored: Int get() = months.sumOf { it.scored }

    val isEmpty: Boolean get() = scored == 0

    /** Newest first, and only months with something in them. */
    val recent: List<KnowingMonth>
        get() = months.filter { it.scored > 0 }.sortedByDescending { it.month }

    fun youShare(): Float = if (scored == 0) 0f else youHits.toFloat() / scored
    fun theyShare(): Float = if (scored == 0) 0f else theyHits.toFloat() / scored
}

data class KnowingMonth(
    val month: YearMonth,
    /** Days in this month where your guess about them landed. */
    val youHits: Int,
    val theyHits: Int,
    /** Days in this month that could be scored at all. */
    val scored: Int,
)
