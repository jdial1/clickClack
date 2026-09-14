package com.ampersand.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NewTestsScoringTest {

    private fun attachment(vararg r: Int) = BadgeWall.attachmentStyle(Instruments.ATTACHMENT.readings(r.toList()))

    @Test
    fun `attachment lands in the right quadrant`() {
        // Leans on partner (5s on 1-4), opens up (1s on 5-6), not worried (1s on 7-9).
        assertEquals("Secure", attachment(5, 5, 5, 5, 1, 1, 1, 1, 1))
        assertEquals("Anxious", attachment(5, 5, 5, 5, 1, 1, 5, 5, 5))
        assertEquals("Avoidant", attachment(1, 1, 1, 1, 5, 5, 1, 1, 1))
        assertEquals("Fearful", attachment(1, 1, 1, 1, 5, 5, 5, 5, 5))
        assertEquals(6..30, Instruments.ATTACHMENT.subscales[0].range)
        assertEquals(3..15, Instruments.ATTACHMENT.subscales[1].range)
    }

    @Test
    fun `enneagram types take every ninth item`() {
        val kit = Instruments.ENNEAGRAM
        assertEquals(36, kit.items.size)
        assertEquals(mapOf(3 to 1, 12 to 1, 21 to 1, 30 to 1), kit.subscales[3].terms)
        val responses = List(36) { if (it % 9 == 4) 5 else 1 }
        assertEquals("Investigator", kit.readings(responses).maxBy { it.value }.subscale.label)
    }

    @Test
    fun `DISC names the top trait or a close blend`() {
        val kit = Instruments.DISC
        assertEquals(16, kit.items.size)
        val steady = List(16) { if (it % 4 == 2) 5 else 1 }
        assertEquals("Steady", BadgeWall.discStyle(kit.readings(steady)))
        val blend = List(16) { when (it % 4) { 2 -> 5; 3 -> if (it == 15) 4 else 5; else -> 1 } }
        assertEquals("Steady-Conscientious", BadgeWall.discStyle(kit.readings(blend)))
    }
}
