package com.ampersand.data

import com.ampersand.model.DayEntry
import com.ampersand.model.Depth
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Quiz
import com.ampersand.model.QuizTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** The first seven days you both answer are chosen, not drawn. */
class FirstWeekTest {

    private val today = LocalDate.of(2026, 11, 3)
    private fun q(id: String, category: String = "Everyday") = Question(id, "?", QuestionType.REFLECT, category, Depth(1))
    private val pool = QuestionPool(
        decks = emptyList(),
        dailyOnly = listOf(Quiz("d", "D", "", QuizTheme.PLAYFUL, listOf(q("pool"), q("themed", "Food")))),
        firstWeek = listOf(q("fw1"), q("fw2"), q("fw3")),
    )
    private fun revealed(daysAgo: Long) = DayEntry(
        today.minusDays(daysAgo), q("x$daysAgo"), opened = true, submitted = setOf("a", "b"),
    )

    @Test
    fun `a couple with nothing opened starts at the first question`() {
        assertEquals(0, firstWeekDay(emptyList(), sealedSculpture = false))
        assertEquals("fw1", pool.daily(today, firstWeekDay = 0).id)
    }

    @Test
    fun `each opened day moves on one`() {
        assertEquals(2, firstWeekDay(listOf(revealed(1), revealed(2)), sealedSculpture = false))
        assertEquals("fw3", pool.daily(today, firstWeekDay = 2).id)
    }

    @Test
    fun `a day nobody finished does not use up a question`() {
        val unfinished = DayEntry(today.minusDays(1), q("fw1"), submitted = setOf("a"))
        assertEquals(0, firstWeekDay(listOf(unfinished), sealedSculpture = false))
    }

    @Test
    fun `seven opened days or a sealed sculpture end it`() {
        assertNull(firstWeekDay((1L..7L).map(::revealed), sealedSculpture = false))
        assertNull(firstWeekDay(emptyList(), sealedSculpture = true))
    }

    @Test
    fun `the theme you picked last night still wins`() {
        assertEquals("themed", pool.daily(today, theme = "Food", firstWeekDay = 1).id)
    }

    @Test
    fun `a first week question asked lately is walked past`() {
        assertEquals("fw2", pool.daily(today, recent = setOf("fw1"), firstWeekDay = 0).id)
    }

    @Test
    fun `past the end it falls through to the pool`() {
        assertEquals("pool", pool.daily(today, recent = setOf("themed"), firstWeekDay = 3).id)
    }
}
