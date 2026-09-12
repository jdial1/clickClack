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

    // ---- what you wrote --------------------------------------------------

    private fun words(n: Int) = List(n) { "word" }.joinToString(" ")

    private val reflect = question(type = QuestionType.REFLECT)
    private val guessMe = question(type = QuestionType.GUESS_ME)

    @Test
    fun `a real answer is one step taller than a word or two`() {
        assertEquals(BrickHeight.LOW.rows, shapeOf(day(q = reflect, mine = "fine")).h)
        assertEquals(BrickHeight.MEDIUM.rows, shapeOf(day(q = reflect, mine = words(15))).h)
    }

    @Test
    fun `just under the line does not count`() {
        assertEquals(BrickHeight.LOW.rows, shapeOf(day(q = reflect, mine = words(14))).h)
    }

    /** The cap. An essay is not a better answer than two true sentences. */
    @Test
    fun `fifteen hundred words earn exactly what fifteen do`() {
        val sentence = shapeOf(day(q = reflect, mine = words(15)))
        val essay = shapeOf(day(q = reflect, mine = words(1500)))
        assertEquals(sentence, essay)
    }

    @Test
    fun `only a deep question answered properly reaches the top`() {
        val deep = question(type = QuestionType.REFLECT, depth = 3)
        assertEquals(BrickHeight.MEDIUM.rows, shapeOf(day(q = deep, mine = "fine")).h)
        assertEquals(BrickHeight.HIGH.rows, shapeOf(day(q = deep, mine = words(40))).h)
    }

    /** Your answer and your guess are both things you wrote. */
    @Test
    fun `a guess counts toward what you put in`() {
        val shape = shapeOf(day(q = guessMe, mine = words(8), theirs = "x", myGuess = words(8)))
        assertEquals(BrickHeight.MEDIUM.rows, shape.h)
    }

    /** Your brick is yours. Their essay does not grow it. */
    @Test
    fun `your partner writing a lot does not change your brick`() {
        val terse = shapeOf(day(q = reflect, mine = "fine", theirs = "short"))
        val theyWroteAnEssay = shapeOf(day(q = reflect, mine = "fine", theirs = words(900)))
        assertEquals(terse, theyWroteAnEssay)
    }

    /** A choice between labels somebody else wrote measures nothing about you. */
    @Test
    fun `length is ignored on questions that are not a text box`() {
        val picked = question(type = QuestionType.PICK_ONE)
        val longLabel = words(40)
        assertEquals(0, Rewards.effortOf(Answer.of(you.uid, longLabel), picked))
        assertEquals(BrickHeight.LOW.rows, shapeOf(day(q = picked, mine = longLabel)).h)
    }

    @Test
    fun `whitespace is not words`() {
        val padded = "   fine   \n\n\t   ok   "
        assertEquals(0, Rewards.effortOf(Answer.of(you.uid, padded), reflect))
        assertEquals(1, Rewards.effortOf(Answer.of(you.uid, words(15)), reflect))
    }

    /**
     * No ordinary day brick outgrows the rarest piece in the app.
     *
     * The whole point of the cap, checked across every combination rather than
     * a hand-picked few: depth, type, whether you called it, how much you
     * wrote. Letting length climb freely would make a six-stud "called it" a
     * twenty-four cell slab.
     */
    @Test
    fun `nothing outgrows the roof however much is written`() {
        val roofArea = 4 * BrickHeight.HIGH.rows
        val types = listOf(
            QuestionType.REFLECT, QuestionType.GUESS_ME,
            QuestionType.THIS_OR_THAT, QuestionType.PICK_ONE,
        )
        for (depth in 1..3) for (type in types) for (n in listOf(0, 14, 15, 1500)) {
            for (called in listOf(true, false)) {
                val q = question(type = type, depth = depth)
                val d = day(
                    q = q,
                    mine = words(maxOf(n, 1)),
                    theirs = "b",
                    myGuess = if (called) "b" else words(maxOf(n, 1)),
                )
                val shape = shapeOf(d)
                assertTrue(
                    "depth $depth $type words $n called $called gave ${shape.w}x${shape.h}",
                    shape.w * shape.h <= roofArea,
                )
            }
        }
    }

    // ---- the flag itself ---------------------------------------------------

    @Test
    fun `talked about is nobody until somebody says so`() {
        assertEquals(false, day().wasTalkedAbout)
        assertEquals(true, day(talked = setOf(them.uid)).wasTalkedAbout)
    }
}
