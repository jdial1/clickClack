package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceTest {

    private val table = ZipTable.parse(
        sequenceOf("15213,40.444,-79.955,PA", "15217,40.431,-79.921,PA", "96813,21.317,-157.857,HI", "bad,row"),
    )

    @Test
    fun `a known zip maps to its state and region`() {
        val place = table.lookup("15213")!!
        assertEquals("PA", place.state)
        assertEquals(UsRegion.MID_ATLANTIC, place.region)
        assertEquals(UsRegion.PACIFIC, table.lookup("96813")!!.region)
    }

    @Test
    fun `an unmapped zip falls back to its sorting area, and nonsense to nothing`() {
        assertEquals("PA", table.lookup("15299")!!.state)
        assertNull(table.lookup("99999"))
        assertNull(table.lookup("12ab5"))
        assertNull(table.lookup("123"))
    }

    @Test
    fun `every state and DC has a region`() {
        val states = listOf("AL","AK","AZ","AR","CA","CO","CT","DE","DC","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY")
        states.forEach { assertEquals(it, true, UsRegion.of(it) != null) }
    }

    @Test
    fun `the day's weather is snow, rain, hot or cold, in that order`() {
        assertEquals(WeatherKind.SNOW, DayWeather(95, "Snow Showers", precipChance = 80).kind)
        assertEquals(WeatherKind.RAIN, DayWeather(95, "Showers And Thunderstorms", precipChance = 60).kind)
        assertEquals(WeatherKind.HOT, DayWeather(95, "Slight Chance Showers", precipChance = 17).kind)
        assertEquals(WeatherKind.COLD, DayWeather(28, "Sunny", precipChance = 0).kind)
        assertNull(DayWeather(84, "Partly Sunny then Slight Chance Showers And Thunderstorms", precipChance = 17).kind)
    }

    @Test
    fun `the line reads like a person wrote it`() {
        assertEquals("84°  ·  Partly sunny  ·  Pittsburgh", DayWeather(84, "Partly Sunny", "Pittsburgh").line)
    }
}
