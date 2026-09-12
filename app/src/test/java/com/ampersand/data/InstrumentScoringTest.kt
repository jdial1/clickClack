package com.ampersand.data

import com.ampersand.model.InstrumentRun
import com.ampersand.model.Member
import com.ampersand.model.Owner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The scoring keys, checked against the numbers their own papers print.
 *
 * These equations were transcribed from a PDF by hand, and a sign error in one
 * of them produces a wrong letter that still looks exactly like a letter. There
 * is no screen on which that failure is visible, which is the whole reason this
 * file exists.
 */
class InstrumentScoringTest {

    private val you = Member("u1", "You", 0xFFF54E4F)
    private val them = Member("u2", "Sam", 0xFF108BE2)

    // ---- Jorgenson's Open Extended Jungian Type Scales --------------------

    /**
     * Every axis must reach 8 at one extreme and 40 at the other.
     *
     * The published sheet states that range for all four, and it only holds if
     * the constant and the signs agree: get one sign wrong and the floor moves
     * to 6 or 10 without anything else looking amiss.
     */
    @Test
    fun `every jungian axis spans 8 to 40 about a midpoint of 24`() {
        Instruments.JUNGIAN.subscales.forEach { scale ->
            assertEquals("${scale.id} floor", 8, scale.range.first)
            assertEquals("${scale.id} ceiling", 40, scale.range.last)
            assertEquals("${scale.id} cut", 24, scale.cut)
            assertEquals("${scale.id} item count", 8, scale.terms.size)
        }
    }

    /** Thirty-two items, and each one used by exactly one axis. */
    @Test
    fun `the jungian axes partition the item list`() {
        val kit = Instruments.JUNGIAN
        assertEquals(32, kit.items.size)
        val used = kit.subscales.flatMap { it.terms.keys }
        assertEquals("no item scored twice", used.size, used.toSet().size)
        assertEquals("every item scored once", kit.items.indices.toSet(), used.toSet())
    }

    /**
     * Straight down the middle lands exactly on the cut, on every axis.
     *
     * All 3s is the one input whose answer can be derived by hand from the
     * printed sheet -- each equation's constant plus three times the sum of its
     * signs -- so it is the check that catches a mistyped constant.
     */
    @Test
    fun `all threes lands on the midpoint`() {
        val middling = List(32) { 3 }
        Instruments.JUNGIAN.readings(middling).forEach { r ->
            assertEquals("${r.subscale.id} on all 3s", 24, r.value)
            // At the cut, not above it, so every axis resolves to the low letter.
            assertEquals(r.subscale.low, r.letter)
            assertEquals(0f, r.margin, 0.001f)
        }
    }

    /**
     * One end of every scale, checked against the published cut.
     *
     * Answering 1 to everything is not "all I" -- the axes do not all run the
     * same way round, which is exactly the property a transcription error
     * destroys. IE and FT carry a constant of 30 and mostly negative terms, so
     * all 1s drives them *up*; SN and JP carry 12 and 18 and mostly positive
     * terms, so all 1s drives them down.
     */
    @Test
    fun `the axes do not all run the same way round`() {
        val byId = Instruments.JUNGIAN.readings(List(32) { 1 }).associateBy { it.subscale.id }

        // 30 - Q3 - Q7 - Q11 + Q15 - Q19 + Q23 + Q27 - Q31: five down, three up.
        assertEquals(28, byId.getValue("IE").value)
        // 12 + six up - two down.
        assertEquals(16, byId.getValue("SN").value)
        // 30 + three up - five down.
        assertEquals(28, byId.getValue("FT").value)
        // 18 + five up - three down.
        assertEquals(20, byId.getValue("JP").value)

        assertEquals("E", byId.getValue("IE").letter)
        assertEquals("S", byId.getValue("SN").letter)
        assertEquals("T", byId.getValue("FT").letter)
        assertEquals("J", byId.getValue("JP").letter)
    }

    /**
     * A letter won by one point and a letter won by sixteen must not read alike.
     *
     * Three points on a scale: dead on the cut, part way, and pinned. The third
     * needs a response vector built to max one axis -- SN's two negative terms
     * answered 1 and its six positive terms answered 5 -- because no uniform
     * answer reaches an extreme on an axis whose signs are mixed.
     */
    @Test
    fun `margin separates a coin toss from a landslide`() {
        val onTheCut = Instruments.JUNGIAN.readings(List(32) { 3 }).first()
        assertEquals(0f, onTheCut.margin, 0.001f)

        val partWay = sn(Instruments.JUNGIAN.readings(List(32) { 1 }))
        assertEquals(16, partWay.value)
        assertEquals(0.5f, partWay.margin, 0.001f)

        val axis = Instruments.JUNGIAN.subscales.first { it.id == "SN" }
        val down = axis.terms.filterValues { it < 0 }.keys
        val pinned = sn(Instruments.JUNGIAN.readings(List(32) { i -> if (i in down) 1 else 5 }))
        assertEquals(40, pinned.value)
        assertEquals("N", pinned.letter)
        assertEquals(1f, pinned.margin, 0.001f)
    }

    private fun sn(readings: List<com.ampersand.model.Reading>) =
        readings.first { it.subscale.id == "SN" }

    // ---- love styles ------------------------------------------------------

    /**
     * Four subscales of seven, and neither of the two that were cut.
     *
     * Ludus and Mania are not hidden here, they are absent. Hiding a subscale
     * from the result still puts its items in front of somebody, and Mania's
     * thirtieth item asks about suicidal thinking after a breakup -- which a
     * daily couples game has no business asking and no way to answer.
     */
    @Test
    fun `love styles ships four subscales and asks only those items`() {
        val kit = Instruments.LOVE_ATTITUDES
        assertEquals(28, kit.items.size)
        assertEquals(4, kit.subscales.size)
        assertEquals(1, kit.passCount)
        assertTrue("nothing is merely hidden", kit.hidden.isEmpty())

        kit.subscales.forEach {
            assertEquals("${it.id} is seven items", 7, it.terms.size)
            assertEquals("${it.id} spans 7..35", 7..35, it.range)
        }
        val used = kit.subscales.flatMap { it.terms.keys }
        assertEquals("every item scored once", kit.items.indices.toSet(), used.toSet())
        assertEquals("no item scored twice", used.size, used.toSet().size)

        // Every statement carries its own text; a bare pole pair here would be
        // an item with nothing to agree or disagree with.
        assertTrue("all statements", kit.items.all { !it.isPolePair })
    }

    /** The leading style is the highest, and the note names the runner-up. */
    @Test
    fun `a love style badge leads with one and names two`() {
        val kit = Instruments.LOVE_ATTITUDES
        // Agape is the last seven. Max those, middle everything else.
        val agape = kit.subscales.first { it.id == "agape" }.terms.keys
        val responses = List(28) { i -> if (i in agape) 5 else 2 }

        val run = InstrumentRun(
            instrumentId = kit.id,
            takenOn = LocalDate.of(2026, 9, 12),
            answers = mapOf(you.uid to responses, them.uid to responses),
        )
        val badge = BadgeWall.groups(listOf(run), you, them)
            .first { it.instrumentId == kit.id }
            .rows.single()
            .badges.first { it.owner == Owner.YOU }

        assertEquals("Giving way", badge.result)
        assertEquals(4, badge.detail.size)
        val note = requireNotNull(badge.note)
        assertTrue("names the runner-up too: $note", note.contains(","))
        assertTrue("says a person is not one thing: $note", note.contains("only one"))
    }

    // ---- love languages ---------------------------------------------------

    @Test
    fun `love languages is five subscales of four, asked twice`() {
        val kit = Instruments.LOVE_LANGUAGES
        assertEquals(20, kit.items.size)
        assertEquals(2, kit.passCount)
        assertEquals("forty responses per person", 40, kit.length)
        kit.subscales.forEach { assertEquals(4, it.terms.size) }
        kit.subscales.forEach { assertEquals(4..20, it.range) }
    }

    /** The second pass must be scored from the second block, not the first. */
    @Test
    fun `the two passes score independently`() {
        val kit = Instruments.LOVE_LANGUAGES
        val felt = List(20) { 5 }
        val given = List(20) { 1 }

        assertTrue(kit.readings(felt + given, Instruments.FELT).all { it.value == 20 })
        assertTrue(kit.readings(felt + given, Instruments.GIVEN).all { it.value == 4 })
    }

    // ---- the wall ---------------------------------------------------------

    /** One person in is not a result. The seal is the whole mechanic. */
    @Test
    fun `a badge stays unearned until both have finished`() {
        val half = InstrumentRun(
            instrumentId = Instruments.JUNGIAN.id,
            takenOn = LocalDate.of(2026, 9, 12),
            answers = mapOf(you.uid to List(32) { 3 }),
        )
        val badge = BadgeWall.groups(listOf(half), you, them)
            .first { it.instrumentId == Instruments.JUNGIAN.id }
            .rows.single()
            .badges.first { it.owner == Owner.YOU }

        assertNull(badge.result)
        assertTrue("your own submission is acknowledged", badge.waitingOnThem)
    }

    /**
     * The gap is reported against the person actually going short.
     *
     * Both halves of this used to be one absolute difference, which meant that
     * giving somebody far more of a thing than they wanted was reported as
     * neglecting them. Here you lavish words nobody asked for and Sam quietly
     * wants time -- the sentence has to name time.
     */
    @Test
    fun `over-giving is not a shortfall`() {
        //                  words time gifts acts touch
        val yourFelt = listOf(3, 3, 3, 5, 4)
        val yourGiven = listOf(5, 3, 3, 3, 4)
        val theirFelt = listOf(3, 5, 3, 3, 4)
        val theirGiven = listOf(3, 3, 3, 3, 4)

        val run = InstrumentRun(
            instrumentId = Instruments.LOVE_LANGUAGES.id,
            takenOn = LocalDate.of(2026, 9, 12),
            answers = mapOf(
                you.uid to expand(yourFelt, yourGiven),
                them.uid to expand(theirFelt, theirGiven),
            ),
        )

        val fit = fitBadge(run)

        assertNotNull(fit.result)
        assertEquals(5, fit.detail.size)

        val note = requireNotNull(fit.note) { "a measured gap must say something" }
        assertTrue("should name the shortfall, not the surplus: $note", note.contains("time"))
        assertTrue("Sam is the one going short: $note", note.startsWith("Sam"))
    }

    @Test
    fun `a couple who match are told so`() {
        val same = listOf(4, 4, 4, 4, 4)
        val run = InstrumentRun(
            instrumentId = Instruments.LOVE_LANGUAGES.id,
            takenOn = LocalDate.of(2026, 9, 12),
            answers = mapOf(
                you.uid to expand(same, same),
                them.uid to expand(same, same),
            ),
        )
        val fit = fitBadge(run)

        assertEquals("Lines up on 5 of 5", fit.result)
        assertEquals(100, BadgeWall.fitPercent(run, you, them))
    }

    /**
     * Every course a test produces sits under that test's own heading.
     *
     * Love languages yields three, and they were siblings of the type row until
     * it became clear that three rows called "Love language something" read as
     * three separate tests rather than as one sitting.
     */
    @Test
    fun `each test owns its own courses`() {
        val groups = BadgeWall.groups(emptyList(), you, them)
        assertEquals(3, groups.size)

        val type = groups.first { it.instrumentId == Instruments.JUNGIAN.id }
        assertEquals(1, type.rows.size)
        assertEquals("a lone course needs no sub-heading", "", type.rows.single().label)

        val language = groups.first { it.instrumentId == Instruments.LOVE_LANGUAGES.id }
        assertEquals("wanted, given, and the gap", 3, language.rows.size)
        assertTrue("every course is labelled", language.rows.all { it.label.isNotBlank() })

        // The heading has to carry the explanation: a badge on its own is a
        // result with no question attached to it.
        groups.forEach {
            assertTrue("${it.title} explains itself", it.about.length > 60)
            assertTrue("${it.title} says how long", it.minutes > 0)
        }
    }

    private fun fitBadge(run: InstrumentRun) =
        BadgeWall.groups(listOf(run), you, them)
            .first { it.instrumentId == Instruments.LOVE_LANGUAGES.id }
            .rows.last()
            .badges.single()

    /** Five per-language levels to the four items each one covers, twice. */
    private fun expand(felt: List<Int>, given: List<Int>): List<Int> =
        (felt + given).flatMap { level -> List(4) { level } }
}
