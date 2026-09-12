package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * What a day earns, and why the sculpture is worth looking at.
 *
 * The shelf only means anything if a week where something happened is
 * distinguishable from a week of clearing the notification. Width already said
 * how well you did and height said how deep the question was; the two rarest
 * and best days earned the same box as everything else.
 */
class DayRewardTest {

    private val you = Member("u1", "You", 0xFFF54E4F)
    private val them = Member("u2", "Maya", 0xFF108BE2)
    private val date = LocalDate.of(2026, 9, 12)

    private fun question(
        id: String = "q1",
        type: QuestionType = QuestionType.THIS_OR_THAT,
        depth: Int = 1,
    ) = Question(id = id, text = "?", type = type, category = "Everyday", depth = Depth(depth))

    private fun day(
        q: Question = question(),
        mine: String = "a",
        theirs: String = "b",
        myGuess: String? = null,
        talked: Set<String> = emptySet(),
    ) = DayEntry(
        date = date,
        question = q,
        answers = mapOf(
            you.uid to Answer.of(you.uid, mine, myGuess),
            them.uid to Answer.of(them.uid, theirs),
        ),
        talkedAbout = talked,
    )

    private fun shapeOf(d: DayEntry, streak: Int = 1) =
        Rewards.forDay(d, you, them, streak).single().shape

    // ---- the two that used to earn nothing special -------------------------

    /** The only content in the app that came from inside the relationship. */
    @Test
    fun `a wildcard earns the piece that is not a box`() {
        val wild = question(id = "${Wildcard.PREFIX}abc")
        assertNotEquals(Slope.NONE, shapeOf(day(q = wild)).slope)
    }

    /** The conversation carrying on away from the phone is the whole point. */
    @Test
    fun `a day you talked about again earns the piece that is not a box`() {
        assertNotEquals(Slope.NONE, shapeOf(day(talked = setOf(you.uid))).slope)
    }

    /** One person remembering is the event; it need not be you. */
    @Test
    fun `either of you marking it is enough`() {
        assertNotEquals(Slope.NONE, shapeOf(day(talked = setOf(them.uid))).slope)
    }

    @Test
    fun `an ordinary missed day is still a plain stub`() {
        val shape = shapeOf(day())
        assertEquals(Slope.NONE, shape.slope)
        assertEquals(2, shape.w)
    }

    // ---- what was already there, which should not have moved ---------------

    @Test
    fun `calling it still widens the brick`() {
        assertEquals(6, shapeOf(day(myGuess = "b")).w)
        assertEquals(2, shapeOf(day(myGuess = "wrong")).w)
    }

    /** `BrickShape.h` is a row count, not a `BrickHeight`. */
    @Test
    fun `depth still raises it`() {
        assertEquals(BrickHeight.LOW.rows, shapeOf(day(q = question(depth = 1))).h)
        assertEquals(BrickHeight.MEDIUM.rows, shapeOf(day(q = question(depth = 3))).h)
    }

    @Test
    fun `a reflect day is not judged`() {
        val reflect = question(type = QuestionType.REFLECT)
        assertEquals(4, shapeOf(day(q = reflect)).w)
    }

    /** A seventh day outranks everything, including the new cases. */
    @Test
    fun `a streak milestone still wins`() {
        assertEquals(CharacterShape, shapeOf(day(talked = setOf(you.uid)), streak = 7))
    }

    @Test
    fun `a day nobody finished earns nothing at all`() {
        val half = DayEntry(
            date = date,
            question = question(),
            answers = mapOf(you.uid to Answer.of(you.uid, "a")),
        )
        assertTrue(Rewards.forDay(half, you, them, 1).isEmpty())
    }

    // ---- the flag itself ---------------------------------------------------

    @Test
    fun `talked about is nobody until somebody says so`() {
        assertEquals(false, day().wasTalkedAbout)
        assertEquals(true, day(talked = setOf(them.uid)).wasTalkedAbout)
    }
}
