package com.ampersand.model

/**
 * What the app has learned about the two of you, as a tilt on the pool.
 *
 * Deliberately a *weight* and never a filter. Every question stays reachable at
 * least once however the profile comes out -- a couple who scored high on one
 * thing should not find a third of the app quietly unavailable to them, and a
 * recommendation engine that narrows is how an app ends up asking about the
 * same four subjects until it is deleted.
 *
 * [reasons] exists so this is never mysterious. An app that silently steers
 * what it asks based on a test you took in March is doing something people are
 * right to dislike; one that says "Maya wants time more than you tend to give
 * it, so there is more about that this fortnight" is doing the same arithmetic
 * out loud, which is the whole difference.
 */
data class Taste(
    /**
     * Question category to how many times it enters the draw. 1 is normal.
     *
     * Keyed by the label a question actually carries -- categories default to
     * their deck's theme label, so these are "Everyday", "Deep" and so on
     * rather than enum names.
     */
    val themes: Map<String, Int> = emptyMap(),
    /**
     * Tag to how many times a question carrying it enters the draw.
     *
     * The finer of the two keys. A question gets the strongest of its category
     * and any of its tags -- not the sum, so a question tagged three ways that
     * all match does not crowd out the pool by itself.
     */
    val tags: Map<String, Int> = emptyMap(),
    /** Types that get one extra copy in the draw. */
    val types: Set<QuestionType> = emptySet(),
    /** Said out loud on the Us screen. One short sentence each. */
    val reasons: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = themes.isEmpty() && tags.isEmpty() && types.isEmpty()

    /**
     * How many copies of [question] go into the draw.
     *
     * Capped at [MaxWeight]. Two signals agreeing is worth more than one, but a
     * question that matched every rule at once would otherwise crowd out a
     * quarter of the pool on its own.
     */
    fun weightOf(question: Question): Int {
        val byTheme = themes[question.category] ?: 1
        val byTag = question.tags.maxOfOrNull { tags[it] ?: 1 } ?: 1
        val base = maxOf(byTheme, byTag)
        val bonus = if (question.type in types) 1 else 0
        return (base + bonus).coerceIn(1, MaxWeight)
    }

    companion object {
        /** Four copies against one is already a strong tilt. */
        const val MaxWeight = 4

        val none = Taste()
    }
}
