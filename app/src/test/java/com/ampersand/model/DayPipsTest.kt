package com.ampersand.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DayPipsTest {

    private val day = LocalDate.of(2026, 9, 10)
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))
    private val utc = ZoneOffset.UTC

    private fun entry(opened: Boolean = false, submitted: Set<String> = emptySet()) =
        DayEntry(day, q, opened = opened, submitted = submitted)

    @Test
    fun `an opened day lights both`() {
        assertTrue(DayPips.didSomething("a", day, entry(opened = true), emptyList(), utc))
        assertTrue(DayPips.didSomething("b", day, entry(opened = true), emptyList(), utc))
    }

    @Test
    fun `one answer lights only that person`() {
        val e = entry(submitted = setOf("a"))
        assertTrue(DayPips.didSomething("a", day, e, emptyList(), utc))
        assertFalse(DayPips.didSomething("b", day, e, emptyList(), utc))
    }

    @Test
    fun `finishing a deck that day counts`() {
        val millis = day.atTime(20, 0).toInstant(utc).toEpochMilli()
        val run = QuizRun("deck", "b", answers = emptyMap(), completedAtMillis = millis)
        assertTrue(DayPips.didSomething("b", day, null, listOf(run), utc))
        assertFalse(DayPips.didSomething("b", day.plusDays(1), null, listOf(run), utc))
        assertFalse(DayPips.didSomething("a", day, null, listOf(run), utc))
    }

    @Test
    fun `a day with nothing lights nobody`() {
        assertFalse(DayPips.didSomething("a", day, null, emptyList(), utc))
    }
}
