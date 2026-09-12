package com.ampersand.data

import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Depth
import com.ampersand.model.Taste
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tilt on the pool, checked as arithmetic.
 *
 * Two properties matter more than any particular weighting, and both are the
 * kind of thing that only shows up months in: nothing may become unreachable,
 * and a favoured question must not come round twice in a fortnight. The first
 * is the difference between a tilt and a filter; the second is what makes a
 * naive `List(n) { q }` expansion wrong.
 */
class TasteTest {

    private fun q(id: String, category: String, type: QuestionType = QuestionType.REFLECT) =
        Question(id = id, text = id, type = type, category = category, depth = Depth(1))

    private val pool = listOf(
        q("a", "Everyday"), q("b", "Everyday"), q("c", "Deep"),
        q("d", "Spicy"), q("e", "Future"), q("f", "Playful", QuestionType.GUESS_ME),
    )

    // ---- weights -----------------------------------------------------------

    @Test
    fun `an empty taste weights everything the same`() {
        assertTrue(Taste.none.isEmpty)
        pool.forEach { assertEquals(1, Taste.none.weightOf(it)) }
    }

    @Test
    fun `a favoured category counts more, and a type adds on top`() {
        val taste = Taste(
            themes = mapOf("Everyday" to 3),
            types = setOf(QuestionType.GUESS_ME),
        )
        assertEquals(3, taste.weightOf(q("x", "Everyday")))
        assertEquals(1, taste.weightOf(q("x", "Deep")))
        assertEquals(2, taste.weightOf(q("x", "Deep", QuestionType.GUESS_ME)))
        assertEquals(4, taste.weightOf(q("x", "Everyday", QuestionType.GUESS_ME)))
    }

    /** Two signals agreeing is worth something; agreeing four times is not. */
    @Test
    fun `weight is capped`() {
        val taste = Taste(
            themes = mapOf("Everyday" to 99),
            types = setOf(QuestionType.GUESS_ME),
        )
        assertEquals(Taste.MaxWeight, taste.weightOf(q("x", "Everyday", QuestionType.GUESS_ME)))
    }

    @Test
    fun `weight never drops below one`() {
        val taste = Taste(themes = mapOf("Deep" to 0, "Spicy" to -4))
        assertEquals(1, taste.weightOf(q("x", "Deep")))
        assertEquals(1, taste.weightOf(q("x", "Spicy")))
    }

    // ---- the draw ----------------------------------------------------------

    private fun draw(taste: Taste): List<Question> {
        // Mirrors QuestionPool.tilted: tier one is everything, tier two is
        // everything weighted two or more, and so on.
        val top = pool.maxOf { taste.weightOf(it) }
        return (1..top).flatMap { tier -> pool.filter { taste.weightOf(it) >= tier } }
    }

    /**
     * The property that makes this a tilt rather than a filter.
     *
     * A couple whose profile comes out strongly one way must not find a third
     * of the app quietly unavailable to them.
     */
    @Test
    fun `every question survives however strong the tilt`() {
        val extreme = Taste(
            themes = mapOf("Everyday" to 4, "Spicy" to 4),
            types = setOf(QuestionType.GUESS_ME),
        )
        assertEquals(pool.map { it.id }.toSet(), draw(extreme).map { it.id }.toSet())
    }

    @Test
    fun `a favoured question appears as often as its weight says`() {
        val taste = Taste(themes = mapOf("Everyday" to 3, "Deep" to 2))
        val counts = draw(taste).groupingBy { it.id }.eachCount()
        assertEquals(3, counts["a"])
        assertEquals(3, counts["b"])
        assertEquals(2, counts["c"])
        assertEquals(1, counts["d"])
        assertEquals(1, counts["e"])
    }

    /**
     * Copies sit a whole pool apart, not next to each other.
     *
     * `pool.flatMap { List(weight) { it } }` is the obvious expansion and it
     * puts a favoured question's copies adjacent, so the stride walks onto the
     * same question twice within a couple of weeks. Tiers keep them apart.
     */
    @Test
    fun `copies of a favoured question are never adjacent`() {
        val taste = Taste(themes = mapOf("Everyday" to 3))
        val list = draw(taste).map { it.id }
        list.zipWithNext().forEach { (a, b) ->
            assertTrue("$a repeated back to back in $list", a != b)
        }
    }

    @Test
    fun `the draw grows by exactly the sum of the weights`() {
        val taste = Taste(themes = mapOf("Everyday" to 3, "Deep" to 2))
        assertEquals(pool.sumOf { taste.weightOf(it) }, draw(taste).size)
    }

    @Test
    fun `an empty taste leaves the pool exactly as it was`() {
        assertEquals(pool.map { it.id }, draw(Taste.none).map { it.id })
    }

    // ---- the same on both phones ------------------------------------------

    /**
     * Two devices computing the same taste must compute the same draw.
     *
     * This is the requirement the whole daily question rests on: both phones
     * have to land on the same question before either has written anything.
     */
    @Test
    fun `the draw is a pure function of the taste`() {
        val a = Taste(themes = mapOf("Everyday" to 3), types = setOf(QuestionType.GUESS_ME))
        val b = Taste(themes = mapOf("Everyday" to 3), types = setOf(QuestionType.GUESS_ME))
        assertEquals(draw(a).map { it.id }, draw(b).map { it.id })
        assertFalse(a.isEmpty)
    }
}
