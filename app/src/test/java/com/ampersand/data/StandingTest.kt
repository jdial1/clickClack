package com.ampersand.data

import com.ampersand.model.InstrumentRun
import com.ampersand.model.Member
import com.ampersand.model.Standing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where each of you has got to, and what the wall says about it.
 *
 * The state this exists for is the one that used to be invisible: both of you
 * have finished round one of love languages and neither has started round two.
 * That looked exactly like neither of you having opened it, so two people sat
 * waiting for each other to go first on something they had both half done.
 */
class StandingTest {

    private val you = Member("u1", "You", 0xFFF54E4F)
    private val them = Member("u2", "Maya", 0xFF108BE2)
    private val kit = Instruments.LOVE_LANGUAGES
    private val solo = Instruments.JUNGIAN

    private fun run(vararg progress: Pair<String, Int>) = InstrumentRun(
        instrumentId = kit.id,
        progress = progress.toMap(),
    )

    // ---- the arithmetic ---------------------------------------------------

    @Test
    fun `a round is the one you are in, not the one you finished`() {
        // Twenty items a round, forty in all.
        assertEquals(0, kit.standingOf(0).pass)
        assertEquals(0, kit.standingOf(19).pass)
        // Twenty answered is round one complete, so you are now in round two.
        assertEquals(1, kit.standingOf(20).pass)
        assertEquals(1, kit.standingOf(39).pass)
        // Finished, and never past the last round.
        assertEquals(1, kit.standingOf(40).pass)
    }

    @Test
    fun `a single-round instrument never reports rounds`() {
        assertFalse(solo.standingOf(0).hasRounds)
        assertFalse(solo.standingOf(31).hasRounds)
        assertTrue(kit.standingOf(0).hasRounds)
        // And its pass never leaves zero however far you get.
        assertEquals(0, solo.standingOf(32).pass)
    }

    @Test
    fun `standing is clamped to the instrument`() {
        assertEquals(0, kit.standingOf(-5).answered)
        assertEquals(40, kit.standingOf(999).answered)
        assertTrue(kit.standingOf(999).finished)
        assertFalse(kit.standingOf(39).finished)
        assertTrue(kit.standingOf(1).inProgress)
        assertFalse(kit.standingOf(0).inProgress)
        assertFalse(kit.standingOf(40).inProgress)
    }

    /** A sealed run outranks a stale count from a phone that died mid-write. */
    @Test
    fun `a finished run beats a lagging progress number`() {
        val sealed = InstrumentRun(
            instrumentId = kit.id,
            answers = mapOf(you.uid to List(40) { 3 }),
            progress = mapOf(you.uid to 12),
        )
        assertEquals(40, sealed.answeredBy(you.uid, 40))
        assertEquals(0, sealed.answeredBy(them.uid, 40))
    }

    // ---- what the brick says ----------------------------------------------

    private fun caption(yours: Int, theirs: Int): Pair<String, String> =
        BadgeWall.caption(kit.standingOf(yours), kit.standingOf(theirs), them.name)

    @Test
    fun `neither of you has started`() {
        assertEquals("" to "Take it", caption(0, 0))
    }

    /** The case that prompted all of this. */
    @Test
    fun `both have finished round one and neither has started round two`() {
        val (note, action) = caption(20, 20)
        assertEquals("Carry on", action)
        assertEquals("Both of you are on round 2 of 2", note)
    }

    /** Same state, said once. The template used to say it twice. */
    @Test
    fun `being in the same place is not reported twice`() {
        val (note, _) = caption(20, 20)
        assertEquals("round said once", 1, Regex("round 2 of 2").findAll(note).count())
    }

    @Test
    fun `different rounds name who is where`() {
        val (note, action) = caption(5, 25)
        assertEquals("Carry on", action)
        assertEquals("You are on round 1 of 2, Maya is on round 2 of 2", note)
    }

    @Test
    fun `you are part way and they have not begun`() {
        val (note, action) = caption(8, 0)
        assertEquals("Carry on", action)
        assertEquals("You are on round 1 of 2", note)
    }

    @Test
    fun `you are done and they are mid-way`() {
        val (note, action) = caption(40, 25)
        assertEquals("Waiting on Maya", action)
        assertEquals("Maya is on round 2 of 2", note)
    }

    @Test
    fun `you are done and they never started`() {
        assertEquals("You are done" to "Waiting on Maya", caption(40, 0))
    }

    @Test
    fun `they finished first, so it is your turn`() {
        assertEquals("Maya has finished" to "Take it", caption(0, 40))
    }

    @Test
    fun `they are part way and you have not begun`() {
        val (note, action) = caption(0, 5)
        assertEquals("Take it", action)
        assertEquals("Maya is on round 1 of 2", note)
    }

    /** A one-round test counts instead, because "round 1 of 1" says nothing. */
    @Test
    fun `a single-round test reports a count`() {
        val (note, action) =
            BadgeWall.caption(solo.standingOf(12), solo.standingOf(0), them.name)
        assertEquals("Carry on", action)
        assertEquals("You are 12 of 32", note)
    }

    // ---- through the wall -------------------------------------------------

    @Test
    fun `the wall reads standing from the run`() {
        val runs = listOf(run(you.uid to 20, them.uid to 20))
        val mine = BadgeWall.standing(runs, kit, you.uid)
        val theirs = BadgeWall.standing(runs, kit, them.uid)

        assertEquals(20, mine.answered)
        assertEquals(1, mine.pass)
        assertEquals(1, theirs.pass)
        assertTrue(mine.inProgress)
    }

    @Test
    fun `no run at all is a standing of nothing`() {
        val none = BadgeWall.standing(emptyList(), kit, you.uid)
        assertEquals(Standing.none(40, 2), none)
        assertFalse(none.started)
    }
}
