package com.ampersand.data

import com.ampersand.model.CoupleState
import com.ampersand.model.Taste

/**
 * Weights for the daily draw: topics the couple asked for, plus categories they
 * marked as talked about in the last two weeks.
 * Pure function of shared state, so both phones pick the same question.
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
            reasons += "More " + talked.joinToString(" and ") { it.lowercase() } + ", since you talked about it."
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

    /** Lookback window in days for talked-about days. */
    private const val Fortnight = 14L
}
