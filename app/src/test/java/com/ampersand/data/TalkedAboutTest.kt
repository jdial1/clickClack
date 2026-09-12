package com.ampersand.data

import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.Depth
import com.ampersand.model.Member
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Saying yesterday came up again, end to end through the in-memory repository.
 *
 * The case that shaped the design: yesterday belonged to a week that sealed
 * overnight, so its brick is already on a finished sculpture. The new piece
 * has to land on the week you are in, and only once however often you tap.
 */
class TalkedAboutTest {

    private val you = Member("u1", "You", 0xFFF54E4F)
    private val them = Member("u2", "Maya", 0xFF108BE2)
    private val today = LocalDate.of(2026, 9, 14)
    private val yesterday = today.minusDays(1)
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))

    private fun plate(id: String, started: LocalDate, sealed: LocalDate? = null) =
        BuildPlate(id, started, 7, 12, 28, completedOn = sealed)

    private fun repo() = FakeCoupleRepository(
        CoupleState(
            you = you,
            them = them,
            today = DayEntry(today, q),
            // This week started today; last week sealed overnight.
            plate = plate("this-week", today),
            shelf = listOf(plate("last-week", today.minusDays(7), sealed = today)),
            // Past days load the way the real app loads them: a flag, no answers.
            history = listOf(DayEntry(yesterday, q, opened = true)),
        ),
    )

    @Test
    fun `yesterday's piece lands on this week, not the sealed one`() {
        val r = repo()
        r.markTalkedAbout(yesterday)
        val s = r.state.value

        assertEquals(2, s.plate.blocks.size)
        assertTrue(s.plate.blocks.all { it.earnedOn == today })
        assertTrue("the sealed week is untouched", s.shelf.single().blocks.isEmpty())
        assertTrue(s.history.single().wasTalkedAbout)
    }

    @Test
    fun `tapping twice lays it once`() {
        val r = repo()
        r.markTalkedAbout(yesterday)
        r.markTalkedAbout(yesterday)
        assertEquals(2, r.state.value.plate.blocks.size)
    }

    @Test
    fun `a day that never opened cannot be marked and earns nothing`() {
        val r = FakeCoupleRepository(
            CoupleState(
                you = you,
                them = them,
                today = DayEntry(today, q),
                plate = plate("this-week", today),
                history = listOf(DayEntry(yesterday, q, opened = false)),
            ),
        )
        r.markTalkedAbout(yesterday)
        assertTrue(r.state.value.plate.blocks.isEmpty())
    }
}
