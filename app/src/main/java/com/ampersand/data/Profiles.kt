package com.ampersand.data

import com.ampersand.model.CoupleState
import com.ampersand.model.Taste

/**
 * The lean on the daily draw, from two things you did on purpose: topics you
 * asked for more of, and days you said came up again in the last fortnight.
 *
 * A pure function of shared state, so both phones land on the same question.
 */
object Profiles {

    fun of(state: CoupleState): Taste {
        val themes = mutableMapOf<String, Int>()
        val tags = mutableMapOf<String, Int>()
        val reasons = mutableListOf<String>()

        val since = state.today.date.minusDays(Fortnight)
        val talked = state.history
            .filter { it.date >= since && it.date < state.today.date && it.wasTalkedAbout }
            .map { it.question.category }
            .distinct()
        if (talked.isNotEmpty()) {
            talked.forEach { themes[it] = 3 }
            reasons += "More " + talked.joinToString(" and ") { it.lowercase() } + ", because it came up again."
        }

        if (state.interests.isNotEmpty()) {
            state.interests.forEach { topic ->
                topic.theme?.let { themes[it] = 3 }
                topic.tag?.let { tags[it] = 3 }
            }
            reasons += "More " + state.interests.joinToString(" and ") { it.label.lowercase() } + ", because you asked."
        }

        return Taste(themes = themes, tags = tags, reasons = reasons)
    }

    /** How far back "came up again" reaches. */
    private const val Fortnight = 14L
}
