package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamsTest {

    private fun at(lat: Double, lon: Double) = Place("00000", lat, lon, "PA")

    @Test
    fun `thirty baseball teams and thirty-two football teams`() {
        assertEquals(30, Teams.all.count { it.league == League.MLB })
        assertEquals(32, Teams.all.count { it.league == League.NFL })
    }

    @Test
    fun `the nearest ballpark and stadium are the local teams`() {
        val pittsburgh = at(40.444, -79.955)
        assertEquals("Pirates", Teams.nearest(pittsburgh, League.MLB).nickname)
        assertEquals("Steelers", Teams.nearest(pittsburgh, League.NFL).nickname)
        val greenBay = at(44.513, -88.013)
        assertEquals("Packers", Teams.nearest(greenBay, League.NFL).nickname)
        assertEquals("Brewers", Teams.nearest(greenBay, League.MLB).nickname)
    }

    @Test
    fun `haversine is right to within a mile`() {
        // Pittsburgh to Philadelphia is about 257 miles as the crow flies.
        val miles = Teams.milesBetween(40.447, -80.006, 39.906, -75.166)
        assertTrue("$miles", miles in 255.0..262.0)
    }

    @Test
    fun `a team question is filled in for the place, and left alone without one`() {
        val q = Question("t", "When {nfl} are on, I am:", QuestionType.THIS_OR_THAT, "Playful", Depth(1),
            options = listOf("Watching {nfl}", "Elsewhere"))
        val filled = q.forPlace(at(40.444, -79.955))
        assertEquals("When the Steelers are on, I am:", filled.text)
        assertEquals(listOf("Watching the Steelers", "Elsewhere"), filled.options)
        assertEquals(q, q.forPlace(null))
        assertTrue(q.needsPlace)
    }
}
