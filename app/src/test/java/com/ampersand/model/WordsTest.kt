package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WordsTest {
    private val now = LocalDate.of(2026, 11, 27)

    @Test
    fun `counts take an s except for one`() {
        assertEquals("1 day", counted(1, "day"))
        assertEquals("3 days", counted(3, "day"))
    }

    @Test
    fun `ago reads in months, then years, never days`() {
        assertEquals("a few weeks ago", ago(now.minusDays(20), now))
        assertEquals("a month ago", ago(now.minusMonths(1), now))
        assertEquals("3 months ago", ago(now.minusMonths(3), now))
        assertEquals("a year ago", ago(now.minusMonths(13), now))
        assertEquals("2 years ago", ago(now.minusYears(2), now))
    }
}
