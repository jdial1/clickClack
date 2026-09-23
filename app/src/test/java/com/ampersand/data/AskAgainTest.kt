package com.ampersand.data

import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.Depth
import com.ampersand.model.Member
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * "Ask us this again in a year" is a gesture: either of you can ask, only
 * your own ask can be taken back, and a sealed day can't be asked for.
 */
class AskAgainTest {

    private val you = Member("u1", "You", 0xFFF54E4F)
    private val them = Member("u2", "Maya", 0xFF108BE2)
    private val today = LocalDate.of(2026, 9, 14)
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))

    private fun repo(opened: Boolean = true, asked: Set<String> = emptySet()) = FakeCoupleRepository(
        CoupleState(
            you = you,
            them = them,
            today = DayEntry(today, q),
            plate = BuildPlate("p", today, 7, 12, 28),
            history = listOf(DayEntry(today.minusDays(1), q, opened = opened, askAgain = asked)),
        ),
    )

    private fun FakeCoupleRepository.asked() = state.value.history.single().askAgain

    @Test
    fun `asking marks the day, and taking it back unmarks it`() {
        val r = repo()
        r.askAgain(today.minusDays(1), true)
        assertEquals(setOf("u1"), r.asked())
        r.askAgain(today.minusDays(1), false)
        assertEquals(emptySet<String>(), r.asked())
    }

    @Test
    fun `taking yours back leaves theirs`() {
        val r = repo(asked = setOf("u2"))
        r.askAgain(today.minusDays(1), true)
        r.askAgain(today.minusDays(1), false)
        assertEquals(setOf("u2"), r.asked())
    }

    @Test
    fun `a sealed day cannot be asked for`() {
        val r = repo(opened = false)
        r.askAgain(today.minusDays(1), true)
        assertEquals(emptySet<String>(), r.asked())
    }
}
