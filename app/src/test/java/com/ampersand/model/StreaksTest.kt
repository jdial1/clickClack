package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The streak, as the real app sees it: past days without their answers.
 *
 * Every past day in these tests is built the way the repository builds one --
 * question, reactions, the `revealed` flag, and no answers -- because that is
 * the shape the old count got wrong.
 */
class StreaksTest {

    private val today = LocalDate.of(2026, 9, 12)
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))
    private val you = "u1"

    private fun past(daysAgo: Long, opened: Boolean = true, reacted: Boolean = false) = DayEntry(
        date = today.minusDays(daysAgo),
        question = q,
        opened = opened,
        reactions = if (reacted) mapOf(you to Reaction.LOVE) else emptyMap(),
    )

    private fun todayEntry(answered: Boolean) = DayEntry(
        date = today,
        question = q,
        answers = if (answered) mapOf(you to Answer.of(you, "yes")) else emptyMap(),
    )

    /** The bug: answering every day and never reacting read as a streak of one. */
    @Test
    fun `opened past days count without their answers or any reaction`() {
        val history = (1L..6L).map { past(it) }
        assertEquals(7, Streaks.run(history, todayEntry(answered = true)))
    }

    @Test
    fun `yesterday carries the run before you have answered today`() {
        val history = (1L..4L).map { past(it) }
        assertEquals(4, Streaks.run(history, todayEntry(answered = false)))
    }

    @Test
    fun `a day that never opened breaks the run`() {
        val history = listOf(past(1), past(2), past(3, opened = false), past(4), past(5))
        assertEquals(3, Streaks.run(history, todayEntry(answered = true)))
    }

    @Test
    fun `a reaction alone no longer stands in for answering`() {
        val history = listOf(past(1, opened = false, reacted = true))
        assertEquals(1, Streaks.run(history, todayEntry(answered = true)))
    }

    @Test
    fun `nothing answered anywhere is no streak`() {
        assertEquals(0, Streaks.run(emptyList(), todayEntry(answered = false)))
    }

    @Test
    fun `a past day reads as revealed from its flag alone`() {
        assertTrue(past(1).isRevealed)
        assertFalse(past(1, opened = false).isRevealed)
        // And the flag does not invent answers.
        assertTrue(past(1).answers.isEmpty())
    }
}
