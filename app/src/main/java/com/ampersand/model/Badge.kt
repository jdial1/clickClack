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
 * A result, kept.
 *
 * A number that appears once and scrolls away is not worth a twelve-minute
 * sitting, so everything an instrument computes lands here and stays on the Us
 * page. The badge draws as a brick, because the app has exactly one visual
 * language and this is the obvious thing to say in it.
 *
 * [result] null means the slot is unclaimed and the brick draws as an outline.
 * That is a state rather than an absence: an outlined brick carrying the test's
 * name and its length is the whole acquisition funnel, and an empty spot is an
 * invitation this app already knows how to make.
 *
 * [detail] is the shape behind the headline, and is never optional in spirit
 * even though it is in the type. A four-letter type that hides its margins is
 * claiming a precision the instrument does not have, and a love language shown
 * as one word tells somebody they *are* a category the evidence does not
 * support. The brick says the headline; one tap says the truth.
 */
data class Badge(
    val slotId: String,
    val owner: Owner,
    /** Small line above the result: a name, or "Together". */
    val who: String,
    /** Null until both of you have finished. */
    val result: String? = null,
    val detail: List<BadgeBar> = emptyList(),
    /** The sentence worth acting on, shown under the shape. */
    val note: String? = null,
    val instrumentId: String,
    val minutes: Int,
    /** You are done and they are not. Changes the outline's caption, not its state. */
    val waitingOnThem: Boolean = false,
) {
    val isEarned: Boolean get() = result != null
}

/** One line of the wall: a label and the bricks under it. */
data class BadgeRow(
    val label: String,
    val badges: List<Badge>,
) {
    val isEarned: Boolean get() = badges.all { it.isEarned }
}
