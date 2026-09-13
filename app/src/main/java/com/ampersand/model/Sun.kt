package com.ampersand.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/** Sunrise, sunset and how long the light lasts, for one day in one place. */
data class Daylight(val sunrise: LocalTime, val sunset: LocalTime) {
    val length: Duration get() = Duration.between(sunrise, sunset)

    /** "7:42 pm" -- the way a person reads a clock, US style. */
    val sunsetText: String get() = sunset.format(Clock).lowercase()
    val sunriseText: String get() = sunrise.format(Clock).lowercase()

    /** "13 hours 40 minutes". */
    val lengthText: String
        get() = "${length.toHours()} hours ${length.toMinutesPart()} minutes"

    private companion object {
        val Clock: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    }
}

/**
 * The kinds of day the light makes worth a question: the longest and shortest,
 * and the stretches where it is dark before dinner or light well past it.
 */
enum class SunKind { LONGEST, SHORTEST, EARLY_DARK, LATE_LIGHT }

/**
 * Sunrise and sunset from latitude and longitude, on the phone.
 *
 * The sunrise equation as the NOAA solar calculator uses it, to within a minute
 * or two -- plenty for "the sun goes down at 7:42". Worked out in UTC and shown
 * in [zone], the phone's own, which is the couple's for anyone at home.
 */
object Sun {

    fun daylight(date: LocalDate, lat: Double, lon: Double, zone: ZoneId = ZoneId.systemDefault()): Daylight? {
        // Whole days since the J2000 epoch, counted at noon (JD 2451545 is 1 Jan 2000, 12:00 UTC).
        val n = (date.toEpochDay() - 10957L) + 0.0008
        val meanNoon = n - lon / 360.0
        val anomaly = (357.5291 + 0.98560028 * meanNoon).mod(360.0)
        val m = Math.toRadians(anomaly)
        val center = 1.9148 * sin(m) + 0.0200 * sin(2 * m) + 0.0003 * sin(3 * m)
        val longitude = Math.toRadians((anomaly + center + 180 + 102.9372).mod(360.0))
        val transit = 2451545.0 + meanNoon + 0.0053 * sin(m) - 0.0069 * sin(2 * longitude)
        val declination = asin(sin(longitude) * sin(Math.toRadians(23.4397)))
        val phi = Math.toRadians(lat)
        val cosHour = (sin(Math.toRadians(-0.833)) - sin(phi) * sin(declination)) / (cos(phi) * cos(declination))
        if (cosHour < -1 || cosHour > 1) return null // midnight sun or polar night
        val hour = Math.toDegrees(acos(cosHour)) / 360.0
        return Daylight(
            sunrise = julianToLocal(transit - hour, zone),
            sunset = julianToLocal(transit + hour, zone),
        )
    }

    /** What kind of day [daylight] makes, if it is one worth a question. */
    fun kind(date: LocalDate, daylight: Daylight): SunKind? = when {
        date.monthValue == 6 && date.dayOfMonth in 20..22 -> SunKind.LONGEST
        date.monthValue == 12 && date.dayOfMonth in 20..22 -> SunKind.SHORTEST
        daylight.sunset < LocalTime.of(17, 15) -> SunKind.EARLY_DARK
        daylight.sunset >= LocalTime.of(20, 30) -> SunKind.LATE_LIGHT
        else -> null
    }

    private fun julianToLocal(julian: Double, zone: ZoneId): LocalTime {
        val millis = ((julian - 2440587.5) * 86_400_000.0).toLong()
        return ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone).toLocalTime()
            .withSecond(0).withNano(0)
    }
}
