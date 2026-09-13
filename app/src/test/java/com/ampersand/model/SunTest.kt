package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

class SunTest {

    private val pittsburgh = ZoneId.of("America/New_York")

    private fun near(expected: LocalTime, actual: LocalTime, minutes: Long = 4) {
        val gap = abs(java.time.Duration.between(expected, actual).toMinutes())
        assertTrue("expected about $expected, got $actual", gap <= minutes)
    }

    @Test
    fun `pittsburgh in mid september`() {
        // About 7:00 and 7:32 pm. The formula is good to a couple of minutes;
        // the tolerance is for the reference times, which are approximate.
        val d = Sun.daylight(LocalDate.of(2026, 9, 13), 40.444, -79.955, pittsburgh)!!
        near(LocalTime.of(7, 1), d.sunrise, minutes = 6)
        near(LocalTime.of(19, 32), d.sunset, minutes = 6)
    }

    @Test
    fun `pittsburgh at the solstices`() {
        val june = Sun.daylight(LocalDate.of(2026, 6, 21), 40.444, -79.955, pittsburgh)!!
        near(LocalTime.of(5, 50), june.sunrise)
        near(LocalTime.of(20, 53), june.sunset)
        val december = Sun.daylight(LocalDate.of(2026, 12, 21), 40.444, -79.955, pittsburgh)!!
        near(LocalTime.of(17, 1), december.sunset)
    }

    @Test
    fun `utqiagvik in december has no sunset to speak of`() {
        assertNull(Sun.daylight(LocalDate.of(2026, 12, 21), 71.29, -156.79, ZoneId.of("America/Anchorage")))
    }

    @Test
    fun `the kind of day the light makes`() {
        val summer = Sun.daylight(LocalDate.of(2026, 6, 21), 40.444, -79.955, pittsburgh)!!
        assertEquals(SunKind.LONGEST, Sun.kind(LocalDate.of(2026, 6, 21), summer))
        val november = LocalDate.of(2026, 11, 20)
        assertEquals(SunKind.EARLY_DARK, Sun.kind(november, Sun.daylight(november, 40.444, -79.955, pittsburgh)!!))
        val july = LocalDate.of(2026, 7, 10)
        assertEquals(SunKind.LATE_LIGHT, Sun.kind(july, Sun.daylight(july, 40.444, -79.955, pittsburgh)!!))
        val september = LocalDate.of(2026, 9, 13)
        assertNull(Sun.kind(september, Sun.daylight(september, 40.444, -79.955, pittsburgh)!!))
    }

    @Test
    fun `said the way a person reads a clock`() {
        val d = Daylight(LocalTime.of(7, 4), LocalTime.of(19, 32))
        assertEquals("7:32 pm", d.sunsetText)
        assertEquals("12 hours 28 minutes", d.lengthText)
    }
}
