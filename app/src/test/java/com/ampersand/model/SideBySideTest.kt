package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SideBySideTest {

    private val today = LocalDate.of(2026, 9, 13)
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))
    private val you = Member("a", "Justin", BrickColor.RED.argb)
    private val them = Member("b", "Tasia", BrickColor.BLUE.argb)

    @Test
    fun `counts days, guesses and decks per person`() {
        val state = CoupleState(
            you = you,
            them = them,
            today = DayEntry(today, q, answers = mapOf("a" to Answer.of("a", "x"))),
            plate = BuildPlate(id = "p", startedOn = today, periodDays = 7, gridW = 12, gridH = 28),
            history = listOf(
                DayEntry(today.minusDays(1), q, opened = true),
                DayEntry(today.minusDays(2), q, submitted = setOf("b")),
            ),
            quizRuns = listOf(QuizRun("d", "b", emptyMap(), completedAtMillis = 1L), QuizRun("e", "a", emptyMap())),
            knowing = Knowing(months = listOf(KnowingMonth(java.time.YearMonth.from(today), youHits = 3, theyHits = 5, scored = 8))),
        )
        assertEquals(Tally(daysShowedUp = 2, guessesLanded = 3, decksFinished = 0), SideBySide.of(state, "a"))
        assertEquals(Tally(daysShowedUp = 2, guessesLanded = 5, decksFinished = 1), SideBySide.of(state, "b"))
    }

    @Test
    fun `dates are said the way a person would`() {
        assertEquals("Today", until(today, today))
        assertEquals("Tomorrow", until(today, today.plusDays(1)))
        assertEquals("In 5 days", until(today, today.plusDays(5)))
        assertEquals("In 3 weeks", until(today, today.plusDays(21)))
        assertEquals("In 8 months", until(today, LocalDate.of(2027, 5, 11)))
    }
}
