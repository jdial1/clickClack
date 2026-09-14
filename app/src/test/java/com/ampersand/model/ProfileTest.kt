package com.ampersand.model

import com.ampersand.data.QuestionPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfileTest {

    private val today = LocalDate.of(2026, 9, 13)
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))

    @Test
    fun `a date comes round again this year or next`() {
        assertEquals(LocalDate.of(2027, 5, 11), Dates.nextOccurrence(LocalDate.of(2012, 5, 11), today))
        assertEquals(LocalDate.of(2026, 12, 1), Dates.nextOccurrence(LocalDate.of(1990, 12, 1), today))
        assertEquals(today, Dates.nextOccurrence(LocalDate.of(1988, 9, 13), today))
    }

    @Test
    fun `upcoming dates are soonest first and named for whose they are`() {
        val state = CoupleState(
            you = Member("a", "Justin", BrickColor.RED.argb, birthday = LocalDate.of(1990, 1, 2)),
            them = Member("b", "Tasia", BrickColor.BLUE.argb, birthday = LocalDate.of(1991, 10, 1)),
            today = DayEntry(today, q),
            plate = BuildPlate(id = "p", startedOn = today, periodDays = 7, gridW = 12, gridH = 28),
            anniversary = LocalDate.of(2012, 5, 11),
            specialDates = listOf(SpecialDate("s", "First date", LocalDate.of(2012, 4, 20))),
        )
        assertEquals(
            listOf("Tasia's birthday", "Your birthday", "Tasia's half-birthday", "First date", "Your anniversary", "Your half-birthday"),
            Dates.upcoming(state).map { it.label },
        )
    }

    @Test
    fun `topics match by theme or by tag`() {
        assertTrue(Topic.SPICY.matches(q.copy(category = "Spicy")))
        assertTrue(Topic.MONEY.matches(q.copy(tags = setOf("money", "planning"))))
        assertFalse(Topic.MONEY.matches(q))
    }

    @Test
    fun `a hidden topic never reaches the daily question`() {
        val pool = QuestionPool(
            decks = listOf(
                Quiz("d", "d", "", QuizTheme.EVERYDAY,
                    (1..6).map { Question("m$it", "m", QuestionType.REFLECT, "Everyday", Depth(1), tags = setOf("money")) } +
                        listOf(Question("plain", "p", QuestionType.REFLECT, "Everyday", Depth(1)))),
            ),
        )
        (0L..30L).forEach { n ->
            assertEquals("plain", pool.daily(today.plusDays(n), hidden = setOf(Topic.MONEY)).id)
        }
    }

    @Test
    fun `a date that falls today brings its own question and names it`() {
        val today = java.time.LocalDate.of(2026, 9, 13)
        val first = SpecialDate("a", "First date", java.time.LocalDate.of(2019, 9, 13), DateKind.FIRST_DATE)
        val kinds = Dates.kindsOn(today, listOf(first), listOf(null), null)
        assertEquals(listOf(DateKind.FIRST_DATE), kinds)
        val special = Question("dfd1", "?", QuestionType.REFLECT, "Romantic", Depth(1))
        val pool = com.ampersand.data.QuestionPool(
            decks = listOf(Quiz("d", "d", "", QuizTheme.EVERYDAY, listOf(Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))))),
            dates = mapOf(DateKind.FIRST_DATE to listOf(special)),
        )
        val q = pool.daily(today, dateKinds = kinds)
        assertEquals("dfd1", q.id)
        assertEquals("First date", pool.why(q, today, null, null, kinds, emptySet()))
        assertEquals(emptyList<DateKind>(), Dates.kindsOn(today.plusDays(1), listOf(first), listOf(null), null))
    }
}
