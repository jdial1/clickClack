package com.ampersand.model

import java.time.LocalDate
import java.time.YearMonth

/** A question one of you wrote. Limited to the [WRITABLE] types. */
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
     * As a [Question]. The id gets [PREFIX] so the day doc can pin it and the
     * repository can tell it from a pool question. Always depth 2.
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

        /** Question types a wildcard can use. */
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
 * All-time guess hits for each of you.
 *
 * Stored as one doc per month with a map keyed by day, not a counter, so both
 * phones writing the same day is idempotent. Totals are summed here.
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
}

data class KnowingMonth(
    val month: YearMonth,
    /** Days in this month where your guess about them landed. */
    val youHits: Int,
    val theyHits: Int,
    /** Days in this month that could be scored at all. */
    val scored: Int,
)
