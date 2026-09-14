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

    @Test
    fun `love spectrum scores the AQ-10 key, neutral counts for nothing`() {
        val kit = Instruments.LOVE_SPECTRUM
        assertEquals(10, kit.items.size)
        // Every item answered on the autistic side: agree on 1, 7, 8, 10; disagree on the rest.
        val all = listOf(5, 1, 1, 1, 2, 1, 4, 5, 1, 5)
        assertEquals(10, Instruments.autismTraits(all))
        assertEquals(0, Instruments.autismTraits(List(10) { 3 }))
        assertEquals(0, Instruments.autismTraits(listOf(1, 5, 5, 5, 4, 5, 2, 1, 5, 1)))
        assertEquals("Many autistic traits", BadgeWall.spectrumTitle(6))
        assertEquals("Few autistic traits", BadgeWall.spectrumTitle(2))
    }

    @Test
    fun `ways of loving names a style, or a blend when two are close`() {
        val kit = Instruments.LOVE_ATTITUDES
        val romantic = List(28) { if (it < 7) 5 else 2 }
        assertEquals("The Romantic", BadgeWall.loveStyle(kit.readings(romantic)))
        val blend = List(28) { when (it) { in 7..13 -> 5; in 21..27 -> if (it == 27) 4 else 5; else -> 1 } }
        assertEquals("The Best Friend & The Giver", BadgeWall.loveStyle(kit.readings(blend)))
    }
}
