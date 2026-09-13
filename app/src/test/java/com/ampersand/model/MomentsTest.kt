package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class MomentsTest {

    private val today = LocalDate.of(2026, 9, 13)
    private val you = Member("u1", "You", 0xFFF54E4F)
    private val them = Member("u2", "Tasia", 0xFF108BE2)
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))

    private fun state(history: List<DayEntry>) = CoupleState(
        you = you, them = them, today = DayEntry(today, q),
        plate = BuildPlate("p", today, 7, 12, 28), history = history,
    )

    @Test
    fun `a day you missed this week comes back, and an old opened day is recalled`() {
        val missed = DayEntry(today.minusDays(2), q, submitted = setOf("u2"))
        val old = DayEntry(today.minusDays(40), q, opened = true)
        val moments = Moments.of(state(listOf(missed, old)))
        assertEquals(Moment.Missed(missed, partnerAnswered = true), moments[0])
        assertEquals(Moment.Recall(old), moments[1])
    }

    @Test
    fun `nothing from yesterday you answered, and nothing too recent to recall`() {
        val answered = DayEntry(today.minusDays(1), q, submitted = setOf("u1"))
        val recent = DayEntry(today.minusDays(5), q, opened = true)
        assertTrue(Moments.of(state(listOf(answered, recent))).isEmpty())
    }

    @Test
    fun `the day's mood brick is the mood held longest`() {
        val day = LocalDate.of(2026, 9, 12)
        val eightPm = day.atTime(20, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val entry = MoodEntry(Mood.SNAPPED_IN, day, atMillis = eightPm, spans = mapOf(Mood.HANDLE_WITH_CARE to 10 * 3_600_000L))
        assertEquals(Mood.HANDLE_WITH_CARE, entry.most(ZoneOffset.UTC))
        val block = entry.block(you)
        assertEquals("mood_2026-09-12_u1", block.id)
        assertEquals("mood_spiky_red", block.asset)
        assertEquals(1, block.shape.w)
    }
}
