package com.ampersand.model

/** Whose result a badge is. Couple results span both columns. */
enum class Owner { YOU, THEM, BOTH }

/** One bar in the shape behind a badge. */
data class BadgeBar(
    val label: String,
    /** 0f..1f within the subscale's own range. */
    val fraction: Float,
    /** What to print: "31 of 40", "INTJ won 25-24". */
    val value: String,
)

/**
 * A test result, drawn as a brick on the Us page.
 *
 * [result] null means the slot is unclaimed and draws as an outline.
 * [detail] holds the subscale bars shown when the badge is tapped.
 */
data class Badge(
    val slotId: String,
    val owner: Owner,
    /** Small line above the result: a name, or "Together". */
    val who: String,
    /** Null until both of you have finished. */
    val result: String? = null,
    val detail: List<BadgeBar> = emptyList(),
    /** Note shown under the bars. */
    val note: String? = null,
    val instrumentId: String,
    val minutes: Int,
    /** You are done and they are not. Changes the outline's caption, not its state. */
    val waitingOnThem: Boolean = false,
) {
    val isEarned: Boolean get() = result != null
}

/**
 * One row of badges under an optional sub-label.
 *
 * [label] is empty when a test produces a single row.
 */
data class BadgeRow(
    val label: String,
    val badges: List<Badge>,
) {
    val isEarned: Boolean get() = badges.all { it.isEarned }
}

/** Everything one test produced, under one heading. [about] says what the test asks. */
data class BadgeGroup(
    val instrumentId: String,
    val title: String,
    val about: String,
    val minutes: Int,
    /** Partner's name, for "Maya has not finished". */
    val partnerName: String,
    /** Progress line shown above the action. */
    val standing: String,
    /** Action label: "Start", "Continue", "Maya has not finished". */
    val action: String,
    /** False once you have sealed your half. */
    val yourTurn: Boolean,
    val rows: List<BadgeRow>,
    /** Both results side by side and one question about them, once you've both finished. */
    val together: String? = null,
) {
    val isEarned: Boolean get() = rows.all { it.isEarned }
}
