package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TogetherTest {

    private val start = LocalDate.of(2012, 5, 11)

    @Test
    fun `named years, early marks, and plain days`() {
        assertEquals("100 days together", Together.on(start.plusDays(100), start, null)?.title)
        assertEquals("Six months together", Together.on(start.plusMonths(6), start, null)?.title)
        assertEquals("1,000 days together", Together.on(start.plusDays(1000), start, null)?.title)
        assertEquals("10 years together", Together.on(start.plusYears(10), start, null)?.title)
        assertEquals("The tin anniversary", Together.on(start.plusYears(10), start, null)?.detail)
        assertNull("3 years is a plain anniversary", Together.on(start.plusYears(3), start, null))
        assertNull(Together.on(start.plusDays(101), start, null))
    }

    @Test
    fun `a wedding milestone wins on the same day`() {
        val wedding = LocalDate.of(2021, 5, 11)
        assertEquals("5 years married", Together.on(LocalDate.of(2026, 5, 11), start, wedding)?.title)
    }
}
