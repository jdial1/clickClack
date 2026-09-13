package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MoodTest {

    private val today = LocalDate.of(2026, 9, 13)

    @Test
    fun `a mood is only today's mood on the day it was set`() {
        assertEquals(Mood.KNOCKED_OVER, MoodEntry(Mood.KNOCKED_OVER, today).current(today))
        assertNull(MoodEntry(Mood.KNOCKED_OVER, today.minusDays(1)).current(today))
    }

    @Test
    fun `every mood has a name and a plain line, and none repeat`() {
        assertEquals(10, Mood.entries.size)
        assertEquals(Mood.entries.size, Mood.entries.map { it.label }.toSet().size)
        assertTrue(Mood.entries.all { it.plain.isNotBlank() && '!' !in it.label + it.plain })
    }
}
