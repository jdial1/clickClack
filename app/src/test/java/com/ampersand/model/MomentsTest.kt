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
    private val q = Question("q", "What now?", QuestionType.REFLECT, "Everyday", Depth(1))

    private fun state(history: List<DayEntry>, kept: Map<String, String> = emptyMap()) = CoupleState(
        you = you, them = them, today = DayEntry(today, q),
        plate = BuildPlate("p", today, 7, 12, 28), history = history, kept = kept,
    )

    @Test
    fun `a missed day comes first, and a month-day anniversary wins the rotating slot`() {
        val missed = DayEntry(today.minusDays(2), q, submitted = setOf("u2"))
        val twoMonths = DayEntry(LocalDate.of(2026, 7, 13), q, opened = true)
        val talked = DayEntry(today.minusDays(40), q, opened = true, talkedAbout = setOf("u1"))
        val moments = Moments.of(state(listOf(missed, talked, twoMonths)))
        assertEquals(Moment.Kind.MISSED, moments[0].kind)
        assertEquals("You missed Friday's question. Tasia answered.", moments[0].title)
        assertEquals(Moment.Kind.ON_THIS_DAY, moments[1].kind)
        assertEquals("2 months ago today", moments[1].title)
    }

    @Test
    fun `the rotating slot draws from lines back, talked-about days and kept days`() {
        val note = DayEntry(today.minusDays(5), q, opened = true, notes = mapOf("u2" to "Still thinking about it"))
        val kinds = (0L..30L).map { offset ->
            val s = state(listOf(note), kept = mapOf(Kept.day(today.minusDays(20)) to "Our trip"))
                .let { it.copy(today = it.today.copy(date = today.plusDays(offset))) }
            Moments.of(s).last().kind
        }.toSet()
        assertTrue(kinds.containsAll(listOf(Moment.Kind.LINE_BACK, Moment.Kind.KEPT)))
    }

    @Test
    fun `nothing to bring back on a quiet history`() {
        val answered = DayEntry(today.minusDays(1), q, submitted = setOf("u1"))
        assertTrue(Moments.of(state(listOf(answered))).isEmpty())
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
