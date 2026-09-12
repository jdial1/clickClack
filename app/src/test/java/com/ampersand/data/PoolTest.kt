package com.ampersand.data

import com.ampersand.model.Depth
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Quiz
import com.ampersand.model.QuizTheme
import com.ampersand.model.Taste
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Which question a day gets, once the pool is wider and remembers the last six weeks.
 */
class PoolTest {

    private fun q(id: String, depth: Int = 1, category: String = "Everyday") =
        Question(id, id, QuestionType.REFLECT, category, Depth(depth))

    private fun deck(id: String, questions: List<Question>) =
        Quiz(id, id, "", QuizTheme.EVERYDAY, questions)

    // An ordinary day with no occasion attached. Occasions override everything
    // and are not what these tests are about.
    private val day = LocalDate.of(2026, 3, 10)

    @Test
    fun `daily-only questions reach the day but never the decks`() {
        val pool = QuestionPool(
            decks = listOf(deck("d", listOf(q("deck1")))),
            dailyOnly = listOf(deck("daily", listOf(q("daily1")))),
        )
        assertEquals(listOf("deck1"), pool.decks.flatMap { it.questions }.map { it.id })
        assertTrue(pool.all.any { it.id == "daily1" })
        assertEquals("daily1", pool.question("daily1")?.id)
    }

    @Test
    fun `a question asked lately is passed over`() {
        val pool = QuestionPool(decks = listOf(deck("d", (1..9).map { q("q$it") })))
        val natural = pool.daily(day)
        val skipped = pool.daily(day, recent = setOf(natural.id))
        assertNotEquals(natural.id, skipped.id)
    }

    @Test
    fun `when everything is recent the day still gets a question`() {
        val pool = QuestionPool(decks = listOf(deck("d", (1..5).map { q("q$it") })))
        val all = pool.all.map { it.id }.toSet()
        assertEquals(pool.daily(day).id, pool.daily(day, recent = all).id)
    }

    /** Two phones with the same state must land on the same question. */
    @Test
    fun `skipping is a pure function of what both phones share`() {
        val pool = QuestionPool(decks = listOf(deck("d", (1..12).map { q("q$it") })))
        val recent = setOf("q1", "q4", "q7")
        assertEquals(pool.daily(day, recent = recent), pool.daily(day, recent = recent))
    }

    /**
     * The bugs the old stride had. A pool whose length was a multiple of seven
     * only ever reached a seventh of itself, and a weekend list stepped by
     * calendar days could never reach some of its own questions.
     */
    @Test
    fun `every question is reachable whatever the pool's length`() {
        for (size in listOf(14, 21, 70, 140, 76, 99, 163)) {
            // Depth 3 only: the weekend list is all of it, and weekdays fall
            // back to the whole pool too, so both kinds of day draw from it.
            val pool = QuestionPool(decks = listOf(deck("d", (1..size).map { q("q$it", depth = 3) })))
            val seen = mutableSetOf<String>()
            var d = LocalDate.of(2026, 1, 1)
            repeat(size * 3) {
                seen += pool.daily(d).id
                d = d.plusDays(1)
            }
            assertEquals("pool of $size", size, seen.size)
        }
    }

    /** Only weekends reach the deep end, so weekends alone must cover it. */
    @Test
    fun `weekends alone reach every deep question`() {
        val deep = (1..21).map { q("deep$it", depth = 3) }
        val light = (1..30).map { q("light$it", depth = 1) }
        val pool = QuestionPool(decks = listOf(deck("d", light + deep)))
        val seen = mutableSetOf<String>()
        var d = LocalDate.of(2026, 1, 2) // a Friday
        repeat(51 * 3 * 3) {
            if (d.dayOfWeek.value >= 5) seen += pool.daily(d).id
            d = d.plusDays(1)
        }
        assertTrue("every deep question came up at a weekend", deep.all { it.id in seen })
    }

    /** The weighting changes the draw's length; the stride has to keep fitting. */
    @Test
    fun `a weighted draw still reaches every question`() {
        val questions = (1..20).map { q("q$it", category = if (it <= 8) "Food" else "Everyday") }
        val pool = QuestionPool(decks = listOf(deck("d", questions)))
        val taste = Taste(themes = mapOf("Food" to 2)) // 20 + 8 = 28, a multiple of seven
        val seen = mutableSetOf<String>()
        var d = LocalDate.of(2026, 1, 5) // a Monday; depth 1 is reachable every day
        repeat(28 * 7) {
            seen += pool.daily(d, taste = taste).id
            d = d.plusDays(1)
        }
        assertEquals(20, seen.size)
    }
}
