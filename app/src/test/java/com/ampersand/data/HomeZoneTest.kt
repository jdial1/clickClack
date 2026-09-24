package com.ampersand.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** The day is the couple's, not the phone's: one instant, one date at home. */
class HomeZoneTest {

    // 04:30 UTC on 23 September: still the 22nd on the US west coast.
    private val clock = Clock.fixed(Instant.parse("2026-09-23T04:30:00Z"), ZoneOffset.UTC)

    @Test
    fun `the date is the home zone's, wherever the phone is`() {
        assertEquals(LocalDate.of(2026, 9, 22), HomeZone.today("America/Los_Angeles", clock))
        assertEquals(LocalDate.of(2026, 9, 23), HomeZone.today("Europe/London", clock))
    }

    @Test
    fun `no zone yet, or a bad one, falls back to the phone's own`() {
        assertEquals(ZoneId.systemDefault(), HomeZone.of(null))
        assertEquals(ZoneId.systemDefault(), HomeZone.of("Not/AZone"))
    }
}
