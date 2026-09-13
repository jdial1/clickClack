package com.ampersand.model

import com.ampersand.data.QuestionPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** State holidays, the leagues near home and in season, and the light. */
class LocalDaysTest {

    private val pittsburgh = Place("15213", 40.444, -79.955, "PA")
    private val albuquerque = Place("87102", 35.084, -106.650, "NM")

    @Test
    fun `easter and mardi gras`() {
        assertEquals(LocalDate.of(2026, 4, 5), Occasion.easter(2026))
        assertEquals(LocalDate.of(2027, 3, 28), Occasion.easter(2027))
        assertEquals(Occasion.MARDI_GRAS, Occasion.on(LocalDate.of(2026, 2, 17), state = "LA"))
    }

    @Test
    fun `a state's day is only a day there`() {
        val derby = LocalDate.of(2026, 5, 2)
        assertEquals(Occasion.KENTUCKY_DERBY, Occasion.on(derby, state = "KY"))
        assertNull(Occasion.on(derby, state = "OH"))
        assertNull(Occasion.on(derby))
        assertEquals(Occasion.PATRIOTS_DAY, Occasion.on(LocalDate.of(2026, 4, 20), state = "MA"))
        // National days still win everywhere.
        assertEquals(Occasion.CHRISTMAS, Occasion.on(LocalDate.of(2026, 12, 25), state = "HI"))
    }

    @Test
    fun `every league is the right size`() {
        assertEquals(30, Teams.all.count { it.league == League.NBA })
        assertEquals(32, Teams.all.count { it.league == League.NHL })
        assertEquals(30, Teams.all.count { it.league == League.MLS })
        assertEquals(68, Teams.all.count { it.league == League.COLLEGE })
    }

    @Test
    fun `only leagues near home and in season`() {
        val october = LocalDate.of(2026, 10, 10)
        val pa = Teams.activeLeagues(pittsburgh, october)
        assertTrue(League.NHL in pa && League.NFL in pa && League.COLLEGE in pa)
        val july = LocalDate.of(2026, 7, 10)
        assertFalse(League.NHL in Teams.activeLeagues(pittsburgh, july))
        // The nearest hockey arena to Albuquerque is over 300 miles away.
        assertNull(Teams.local(albuquerque, League.NHL))
    }

    @Test
    fun `team names read naturally in every league`() {
        val q = Question("x", "{nhl}, {mls} and {college} football", QuestionType.REFLECT, "Playful", Depth(1))
        assertEquals("The Penguins, Columbus Crew and Pitt football", q.forPlace(pittsburgh).text)
    }

    private fun q(id: String, text: String = id) = Question(id, text, QuestionType.REFLECT, "Everyday", Depth(1))

    @Test
    fun `a team question waits for its league`() {
        val pool = QuestionPool(
            decks = listOf(Quiz("d", "d", "", QuizTheme.EVERYDAY, (1..5).map { q("q$it") })),
            localTeams = listOf(q("hockey", "{nhl} tonight?")),
        )
        val days = (0L..60L).map { LocalDate.of(2026, 11, 2).plusDays(it) }
        assertTrue(days.none { pool.daily(it, region = UsRegion.MID_ATLANTIC).id == "hockey" })
        assertTrue(days.any { pool.daily(it, region = UsRegion.MID_ATLANTIC, leagues = setOf(League.NHL)).id == "hockey" })
    }
}
