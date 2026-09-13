package com.ampersand.model

/**
 * Weights that tilt the question draw toward what fits the couple.
 * A weight, never a filter: every question stays reachable. [reasons] explains the tilt on screen.
 */
data class Taste(
    /** Category label ("Everyday", "Deep", not enum names) to draw copies. 1 is normal. */
    val themes: Map<String, Int> = emptyMap(),
    /** Tag to draw copies. A question takes the max of its category and tags, not the sum. */
    val tags: Map<String, Int> = emptyMap(),
    /** Types that get one extra copy in the draw. */
    val types: Set<QuestionType> = emptySet(),
    /** Shown on the Us screen. One short sentence each. */
    val reasons: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = themes.isEmpty() && tags.isEmpty() && types.isEmpty()

    /** How many copies of [question] go into the draw, capped at [MaxWeight]. */
    fun weightOf(question: Question): Int {
        val byTheme = themes[question.category] ?: 1
        val byTag = question.tags.maxOfOrNull { tags[it] ?: 1 } ?: 1
        val base = maxOf(byTheme, byTag)
        val bonus = if (question.type in types) 1 else 0
        return (base + bonus).coerceIn(1, MaxWeight)
    }

    companion object {
        const val MaxWeight = 4

        val none = Taste()
    }
}
