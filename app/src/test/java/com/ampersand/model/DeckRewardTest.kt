package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * What a finished deck earns once what you wrote counts too.
 *
 * The same one-step, capped rule as a day, measured across the deck, and the
 * same ceiling: nothing a deck earns may outgrow the roof.
 */
class DeckRewardTest {

    private val you = Member("u1", "You", 0xFFF54E4F)
    private val them = Member("u2", "Maya", 0xFF108BE2)
    private val on = LocalDate.of(2026, 9, 12)

    private fun words(n: Int) = List(n) { "word" }.joinToString(" ")

    private fun q(id: String, type: QuestionType) =
        Question(id = id, text = "?", type = type, category = "Everyday", depth = Depth(1))

    private fun row(
        type: QuestionType,
        yours: String = "a",
        theirs: String = "b",
        yourGuess: String? = null,
        theirGuess: String? = null,
        id: String = "q${type.name}",
    ) = CrossAnswer(q(id, type), listOf(yours), listOf(theirs), yourGuess?.let(::listOf), theirGuess?.let(::listOf))

    private fun result(rows: List<CrossAnswer>) =
        QuizResult(Quiz("deck", "Deck", "", QuizTheme.EVERYDAY, rows.map { it.question }), rows)

    // ---- effort -----------------------------------------------------------

    @Test
    fun `writing properly on half the text questions is enough`() {
        val rows = listOf(
            row(QuestionType.REFLECT, yours = words(20), id = "a"),
            row(QuestionType.REFLECT, yours = "fine", id = "b"),
        )
        assertEquals(1, Rewards.deckEffort(rows, yours = true))
    }

    @Test
    fun `less than half is not`() {
        val rows = listOf(
            row(QuestionType.REFLECT, yours = words(20), id = "a"),
            row(QuestionType.REFLECT, yours = "fine", id = "b"),
            row(QuestionType.REFLECT, yours = "ok", id = "c"),
        )
        assertEquals(0, Rewards.deckEffort(rows, yours = true))
    }

    @Test
    fun `their writing does not count toward yours`() {
        val rows = listOf(row(QuestionType.REFLECT, yours = "fine", theirs = words(200)))
        assertEquals(0, Rewards.deckEffort(rows, yours = true))
        assertEquals(1, Rewards.deckEffort(rows, yours = false))
    }

    @Test
    fun `a deck with no text questions has nothing to measure`() {
        val rows = listOf(row(QuestionType.PICK_ONE, yours = words(40)))
        assertEquals(0, Rewards.deckEffort(rows, yours = true))
    }

    @Test
    fun `a guess counts as your writing`() {
        val rows = listOf(row(QuestionType.GUESS_ME, yours = words(8), yourGuess = words(8)))
        assertEquals(1, Rewards.deckEffort(rows, yours = true))
    }

    // ---- the brick --------------------------------------------------------

    private fun scoredRows(hits: Int, of: Int) = List(of) { i ->
        row(QuestionType.THIS_OR_THAT, theirs = "b", yourGuess = if (i < hits) "b" else "x", id = "s$i")
    }

    @Test
    fun `effort raises a middling deck to the top`() {
        val r = result(scoredRows(hits = 2, of = 5))
        val plain = Rewards.forQuiz(r, you, them, 2, on).single().shape
        val written = Rewards.forQuiz(r, you, them, 2, on, effort = 1).single().shape
        assertEquals(BrickHeight.MEDIUM.rows, plain.h)
        assertEquals(BrickHeight.HIGH.rows, written.h)
        assertEquals(plain.w, written.w)
    }

    /** Growing it would make a twenty-four cell slab. */
    @Test
    fun `the widest deck brick does not grow`() {
        val r = result(scoredRows(hits = 4, of = 5))
        val shape = Rewards.forQuiz(r, you, them, 4, on, effort = 1).single().shape
        assertEquals(6, shape.w)
        assertEquals(BrickHeight.MEDIUM.rows, shape.h)
    }

    @Test
    fun `nothing a deck earns outgrows the roof`() {
        for (of in 1..6) for (hits in 0..of) for (effort in 0..1) {
            val r = result(scoredRows(hits, of))
            val shape = Rewards.forQuiz(r, you, them, hits, on, effort = effort).single().shape
            assertTrue("$hits/$of effort $effort gave ${shape.w}x${shape.h}", shape.w * shape.h <= Rewards.RoofArea)
        }
        val unscored = result(listOf(row(QuestionType.REFLECT, yours = words(30))))
        val shape = Rewards.forQuiz(unscored, you, them, 0, on, effort = 1).single().shape
        assertTrue(shape.w * shape.h <= Rewards.RoofArea)
    }
}
