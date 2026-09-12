package com.ampersand.data

import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.Knowing
import com.ampersand.model.KnowingMonth
import com.ampersand.model.Depth
import com.ampersand.model.Member
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Reaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * The last fortnight leaning on the pool, with no questionnaire taken at all.
 *
 * Past days are built the way the real app loads them: a flag, reactions and
 * who said it came up again, and no answers.
 */
class ProfilesLatelyTest {

    private val you = Member("u1", "You", 0xFFF54E4F)
    private val them = Member("u2", "Maya", 0xFF108BE2)
    private val today = LocalDate.of(2026, 9, 14)

    private fun q(category: String) = Question("q$category", "?", QuestionType.REFLECT, category, Depth(1))

    private fun past(
        daysAgo: Long,
        category: String = "Everyday",
        talked: Boolean = false,
        reaction: Reaction? = null,
    ) = DayEntry(
        date = today.minusDays(daysAgo),
        question = q(category),
        opened = true,
        talkedAbout = if (talked) setOf(you.uid) else emptySet(),
        reactions = reaction?.let { mapOf(them.uid to it) }.orEmpty(),
    )

    private fun state(history: List<DayEntry>) = CoupleState(
        you = you,
        them = them,
        today = DayEntry(today, q("Everyday")),
        plate = BuildPlate("p", today, 7, 12, 28),
        history = history,
    )

    @Test
    fun `a quiet fortnight leans on nothing`() {
        val taste = Profiles.of(state(listOf(past(1), past(2))))
        assertTrue(taste.isEmpty)
        assertTrue(taste.reasons.isEmpty())
    }

    @Test
    fun `categories you talked about again come up more`() {
        val taste = Profiles.of(state(listOf(past(2, "Food", talked = true))))
        assertEquals(3, taste.themes["Food"])
        assertTrue(taste.reasons.single().contains("food"))
    }

    /** A fortnight of what is alive should outweigh a questionnaire from March. */
    @Test
    fun `the week weighs more than a single profile rule`() {
        val taste = Profiles.of(state(listOf(past(1, "Travel", talked = true))))
        assertTrue((taste.themes["Travel"] ?: 1) > 2)
    }

    @Test
    fun `say more tonight goes a little deeper`() {
        val taste = Profiles.of(state(listOf(past(3, reaction = Reaction.CURIOUS))))
        assertEquals(2, taste.themes["Deep"])
        assertTrue(taste.reasons.single().contains("say more tonight"))
    }

    @Test
    fun `other reactions do not count as asking for more`() {
        val taste = Profiles.of(state(listOf(past(3, reaction = Reaction.LOVE))))
        assertFalse(taste.themes.containsKey("Deep"))
    }

    @Test
    fun `lately means the last fortnight`() {
        val taste = Profiles.of(state(listOf(past(20, "Food", talked = true, reaction = Reaction.CURIOUS))))
        assertTrue(taste.isEmpty)
    }

    /** Keep missing each other and the pool offers easier calls, not more misses. */
    @Test
    fun `missing each other leans toward either-or days`() {
        val base = state(emptyList())
        val struggling = base.copy(
            knowing = Knowing(listOf(KnowingMonth(YearMonth.from(today), youHits = 2, theyHits = 2, scored = 12))),
        )
        val taste = Profiles.of(struggling)
        assertEquals(setOf(QuestionType.THIS_OR_THAT), taste.types)
        assertFalse(QuestionType.GUESS_ME in taste.types)
    }

    @Test
    fun `two categories are named together`() {
        val taste = Profiles.of(
            state(listOf(past(1, "Food", talked = true), past(2, "Travel", talked = true))),
        )
        assertEquals(1, taste.reasons.size)
        assertTrue(taste.reasons.single().contains("food and travel"))
    }
}
