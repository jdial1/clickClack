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
 * distinguishable from a week of clearing the notification, without the plate
 * ever becoming a scoreboard. Width says how the question was answered, height
 * says how deep it was and how much you put in, and whether a guess landed
 * changes nothing.
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

    /**
     * The day's own brick cannot know it was talked about: that is only said
     * the morning after, long after this brick is laid. The piece for it comes
     * from [Rewards.forTalk] instead.
     */
    @Test
    fun `the day brick ignores talked about, which arrives too late for it`() {
        assertEquals(shapeOf(day()), shapeOf(day(talked = setOf(you.uid))))
    }

    // ---- the morning-after piece ------------------------------------------

    private val tomorrow = date.plusDays(1)

    @Test
    fun `a day that came up again earns a small slope the morning after`() {
        val piece = Rewards.forTalk(day(talked = setOf(you.uid)), you, them, tomorrow).single()
        assertNotEquals(Slope.NONE, piece.shape.slope)
        assertEquals(2, piece.shape.w)
        assertEquals(BrickHeight.MEDIUM.rows, piece.shape.h)
        assertEquals("dated the morning it was said", tomorrow, piece.earnedOn)
    }

    /** The conversation was had by two people, whichever of them tapped. */
    @Test
    fun `both of you earn one whoever tapped`() {
        val d = day(talked = setOf(them.uid))
        assertEquals(you.uid, Rewards.forTalk(d, you, them, tomorrow).single().ownerUid)
        assertEquals(them.uid, Rewards.forTalk(d, them, you, tomorrow).single().ownerUid)
    }

    @Test
    fun `the two pieces face each other`() {
        val d = day(talked = setOf(you.uid))
        val mine = Rewards.forTalk(d, you, them, tomorrow).single().shape.slope
        val theirs = Rewards.forTalk(d, them, you, tomorrow).single().shape.slope
        assertNotEquals(mine, theirs)
    }

    @Test
    fun `no piece until somebody says it came up`() {
        assertTrue(Rewards.forTalk(day(), you, them, tomorrow).isEmpty())
    }

    @Test
    fun `no piece for a day that never opened`() {
        val sealed = DayEntry(date = date, question = question(), talkedAbout = setOf(you.uid))
        assertTrue(Rewards.forTalk(sealed, you, them, tomorrow).isEmpty())
    }

    /** Past days load without answers; the flag is what says they opened. */
    @Test
    fun `a past day opened only by its flag still earns the piece`() {
        val past = DayEntry(date = date, question = question(), opened = true, talkedAbout = setOf(you.uid))
        assertEquals(1, Rewards.forTalk(past, you, them, tomorrow).size)
    }

    /** Never the same id as the day's own brick, or one would hide the other. */
    @Test
    fun `the piece has its own id`() {
        val d = day(talked = setOf(you.uid))
        val dayBrick = Rewards.forDay(d, you, them, 1).single().id
        val talkPiece = Rewards.forTalk(d, you, them, tomorrow).single().id
        assertNotEquals(dayBrick, talkPiece)
    }

    @Test
    fun `an ordinary pick is a plain stub`() {
        val shape = shapeOf(day())
        assertEquals(Slope.NONE, shape.slope)
        assertEquals(2, shape.w)
    }

    // ---- flat: being right about them earns nothing extra -------------------

    /** A wrong guess is the best reveal of the day. It must not cost a brick. */
    @Test
    fun `calling it changes nothing`() {
        assertEquals(shapeOf(day(myGuess = "b")), shapeOf(day(myGuess = "wrong")))
        assertEquals(shapeOf(day(myGuess = "b")), shapeOf(day()))
    }

    @Test
    fun `calling a deep one is not a roof`() {
        val deep = question(type = QuestionType.GUESS_ME, depth = 3)
        val called = shapeOf(day(q = deep, theirs = "b", myGuess = "b"))
        val missed = shapeOf(day(q = deep, theirs = "b", myGuess = "x"))
        assertEquals(Slope.NONE, called.slope)
        assertEquals(missed, called)
    }

    @Test
    fun `width comes from how the question is answered`() {
        assertEquals(2, shapeOf(day(q = question(type = QuestionType.PICK_ONE))).w)
        assertEquals(4, shapeOf(day(q = question(type = QuestionType.GUESS_ME))).w)
        assertEquals(6, shapeOf(day(q = question(type = QuestionType.RANK))).w)
    }

    // ---- what was already there, which should not have moved ---------------

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
     * wrote. Letting length climb freely would make a six-stud list a
     * twenty-four cell slab.
     */
    @Test
    fun `nothing outgrows the roof however much is written`() {
        val roofArea = 4 * BrickHeight.HIGH.rows
        val types = listOf(
            QuestionType.REFLECT, QuestionType.GUESS_ME,
            QuestionType.THIS_OR_THAT, QuestionType.PICK_ONE,
            QuestionType.MULTI_SELECT, QuestionType.RANK,
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
