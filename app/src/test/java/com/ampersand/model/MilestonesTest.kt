package com.ampersand.model

import com.ampersand.data.QuestionPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The hundredth day, the morning after a run ends, and runs longer than the loaded history. */
class MilestonesTest {

    private val today = LocalDate.of(2026, 9, 15) // a Tuesday
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))

    private fun past(daysAgo: Long, run: Int = 0) =
        DayEntry(today.minusDays(daysAgo), q, opened = true, recordedRun = run)

    private fun run(days: LongRange) = days.map { past(it) }

    @Test
    fun `the day that would make thirty is a milestone`() {
        assertEquals(30, Milestones.on(today, run(1L..29L)))
        assertNull(Milestones.on(today, run(1L..28L)))
    }

    @Test
    fun `answering does not move the milestone off today`() {
        val answered = DayEntry(today, q, answers = mapOf("u" to Answer.of("u", "yes")))
        assertEquals(30, Milestones.on(today, run(1L..29L)))
        // The repository hands history without today; today's answers are not in it.
        assertEquals(30, Streaks.run(run(1L..29L), answered))
    }

    /** The phones load seventy days. The hundredth still has to be seen. */
    @Test
    fun `a recorded run carries past the loaded window`() {
        // 70 loaded days; the oldest one recorded that it made a run of 30.
        val history = (1L..69L).map { past(it) } + past(70, run = 30)
        assertEquals(99, Streaks.before(history, today))
        assertEquals(100, Milestones.on(today, history))
    }

    /** A day opened before its phone loaded history wrote a run that is too short. */
    @Test
    fun `a recorded run that is too short is overruled by what can be seen`() {
        val history = (1L..40L).map { if (it == 5L) past(it, run = 1) else past(it) }
        assertEquals(40, Streaks.before(history, today))
    }

    @Test
    fun `the morning after a long run ends, it says how long it was`() {
        val history = (2L..24L).map { past(it) } // yesterday missed
        assertEquals(23, Milestones.ended(today, history))
    }

    @Test
    fun `only the first morning after`() {
        val history = (3L..24L).map { past(it) } // yesterday and the day before missed
        assertNull(Milestones.ended(today, history))
    }

    @Test
    fun `a short run ending is not news, and a live run is not ended`() {
        assertNull(Milestones.ended(today, (2L..4L).map { past(it) }))
        assertNull(Milestones.ended(today, run(1L..30L)))
    }

    @Test
    fun `headlines never fall through to a bare number`() {
        Milestones.Runs.forEach { assertTrue(Milestones.headline(it).first().isLetter()) }
    }

    // ---- the draw ----------------------------------------------------------

    private fun question(id: String, depth: Int, category: String = "Everyday") =
        Question(id, id, QuestionType.REFLECT, category, Depth(depth))

    private val pool = QuestionPool(
        decks = listOf(
            Quiz(
                "e", "e", "", QuizTheme.EVERYDAY,
                (1..12).map { question("light$it", 1) } + (1..3).map { question("deep$it", 3) },
            ),
        ),
    )

}
