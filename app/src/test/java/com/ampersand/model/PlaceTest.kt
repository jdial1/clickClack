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
    fun `the brick reads the sky from the forecast words`() {
        assertEquals(WeatherBrick.STORMY, DayWeather(84, "Showers And Thunderstorms", precipChance = 70).brick)
        assertEquals(WeatherBrick.SNOWY, DayWeather(30, "Snow Showers", precipChance = 80).brick)
        assertEquals(WeatherBrick.RAINY, DayWeather(60, "Rain", precipChance = 90).brick)
        assertEquals(WeatherBrick.PARTLY_CLOUDY, DayWeather(84, "Partly Sunny then Slight Chance Showers And Thunderstorms", precipChance = 17).brick)
        assertEquals(WeatherBrick.FOGGY, DayWeather(55, "Patchy Fog then Sunny").brick)
        assertEquals(WeatherBrick.HOT, DayWeather(96, "Sunny").brick)
        assertEquals(WeatherBrick.COLD, DayWeather(20, "Clear", daytime = false).brick)
        assertEquals(WeatherBrick.CLEAR_NIGHT, DayWeather(60, "Mostly Clear", daytime = false).brick)
        assertEquals(WeatherBrick.CLOUDY, DayWeather(60, "Mostly Cloudy").brick)
        assertEquals(WeatherBrick.SUNNY, DayWeather(70, "Sunny").brick)
    }

    @Test
    fun `the line reads like a person wrote it`() {
        assertEquals("84°  ·  Partly sunny  ·  Pittsburgh", DayWeather(84, "Partly Sunny", "Pittsburgh").line)
    }

    @Test
    fun `a ZIP knows its time zone, including the states split by a zone line`() {
        assertEquals("America/Chicago", Place("64012", 38.8, -94.5, "MO").timeZone)
        assertEquals("America/New_York", Place("15213", 40.44, -79.95, "PA").timeZone)
        assertEquals("America/Denver", Place("79901", 31.76, -106.48, "TX").timeZone)
        assertEquals("America/Chicago", Place("75201", 32.78, -96.8, "TX").timeZone)
        assertEquals("America/Chicago", Place("32501", 30.42, -87.21, "FL").timeZone)
        assertEquals("America/Phoenix", Place("85001", 33.45, -112.07, "AZ").timeZone)
        assertEquals("America/Los_Angeles", Place("83501", 46.4, -117.0, "ID").timeZone)
    }
}
