package com.ampersand.model

/** US region of the couple's home, for region-specific questions. */
enum class UsRegion(val label: String, val states: Set<String>) {
    NEW_ENGLAND("New England", setOf("ME", "NH", "VT", "MA", "RI", "CT")),
    MID_ATLANTIC("Mid-Atlantic", setOf("NY", "NJ", "PA", "DE", "MD", "DC")),
    SOUTH("The South", setOf("VA", "WV", "KY", "TN", "NC", "SC", "GA", "FL", "AL", "MS", "AR", "LA", "OK", "TX", "PR")),
    MIDWEST("The Midwest", setOf("OH", "IN", "IL", "MI", "WI", "MN", "IA", "MO", "KS", "NE", "SD", "ND")),
    MOUNTAIN("The Mountain West", setOf("MT", "ID", "WY", "CO", "UT", "NV", "AZ", "NM")),
    PACIFIC("The West Coast", setOf("WA", "OR", "CA", "AK", "HI")),
    ;

    companion object {
        fun of(state: String): UsRegion? = entries.firstOrNull { state in it.states }
    }
}

/** Where a ZIP is: the middle of it, and its state. */
data class Place(val zip: String, val lat: Double, val lon: Double, val state: String) {
    val region: UsRegion? get() = UsRegion.of(state)
}

/**
 * Bundled ZIP to location table, so forecasts need no location permission.
 *
 * Built from the 2023 ZCTA gazetteer (centroids) and the 2020 ZCTA-to-county
 * relationship file (state), both public domain. PO-box-only ZIPs have no ZCTA,
 * so unknown ZIPs fall back to the first entry sharing the 3-digit prefix.
 */
class ZipTable(private val places: Map<String, Place>) {

    fun lookup(zip: String): Place? {
        val clean = zip.trim().take(5)
        if (clean.length != 5 || !clean.all(Char::isDigit)) return null
        places[clean]?.let { return it }
        val prefix = clean.take(3)
        return places.values.firstOrNull { it.zip.startsWith(prefix) }
    }

    companion object {
        val empty = ZipTable(emptyMap())

        /** Lines of `zip,lat,lon,state`. */
        fun parse(lines: Sequence<String>): ZipTable = ZipTable(
            lines.mapNotNull { line ->
                val p = line.split(',')
                if (p.size != 4) return@mapNotNull null
                val lat = p[1].toDoubleOrNull() ?: return@mapNotNull null
                val lon = p[2].toDoubleOrNull() ?: return@mapNotNull null
                p[0] to Place(p[0], lat, lon, p[3])
            }.toMap(),
        )
    }
}

/** The weather brick shown on Today. [WeatherKind] is the coarser one used to pick questions. */
enum class WeatherBrick { SUNNY, PARTLY_CLOUDY, CLOUDY, RAINY, STORMY, SNOWY, WINDY, FOGGY, HOT, COLD, CLEAR_NIGHT }

/** Weather that gets its own questions. */
enum class WeatherKind(val label: String) { SNOW("Snow"), RAIN("Rain"), HOT("Hot"), COLD("Cold") }

/**
 * The day's forecast, saved on the day so both phones read the same one.
 * [kind] checks snow, then rain, then temperature.
 */
data class DayWeather(
    val tempF: Int,
    val summary: String,
    val city: String? = null,
    val precipChance: Int? = null,
    /** False when the first forecast period is the night, as it is after dark. */
    val daytime: Boolean? = null,
) {
    val kind: WeatherKind?
        get() {
            val words = summary.lowercase()
            val likely = (precipChance ?: 100) >= LikelyPrecip && "slight chance" !in words
            return when {
                listOf("snow", "flurr", "sleet", "blizzard").any { it in words } && likely -> WeatherKind.SNOW
                listOf("rain", "shower", "thunder", "drizzle").any { it in words } && likely -> WeatherKind.RAIN
                tempF >= HotF -> WeatherKind.HOT
                tempF <= ColdF -> WeatherKind.COLD
                else -> null
            }
        }

    /** Priority: storms, snow, rain, fog, wind, temperature, then sky cover. */
    val brick: WeatherBrick?
        get() {
            val words = summary.lowercase()
            val likely = (precipChance ?: 100) >= LikelyPrecip && "slight chance" !in words
            fun has(vararg w: String) = w.any { it in words }
            return when {
                has("thunder", "storm") && likely -> WeatherBrick.STORMY
                has("snow", "flurr", "sleet", "blizzard", "ice", "freezing") && likely -> WeatherBrick.SNOWY
                has("rain", "shower", "drizzle") && likely -> WeatherBrick.RAINY
                has("fog", "haze", "smoke", "mist") -> WeatherBrick.FOGGY
                has("wind", "breezy", "blustery", "gust") -> WeatherBrick.WINDY
                tempF >= HotF -> WeatherBrick.HOT
                tempF <= ColdF -> WeatherBrick.COLD
                daytime == false && has("clear") -> WeatherBrick.CLEAR_NIGHT
                has("mostly cloudy", "overcast") -> WeatherBrick.CLOUDY
                has("partly", "mostly sunny", "mostly clear") -> WeatherBrick.PARTLY_CLOUDY
                has("cloudy") -> WeatherBrick.CLOUDY
                has("sunny", "clear", "fair") -> WeatherBrick.SUNNY
                // e.g. "Chance showers" below the likely threshold.
                else -> WeatherBrick.PARTLY_CLOUDY
            }
        }

    /** One line for Today: "84°  ·  Partly sunny  ·  Pittsburgh". */
    val line: String
        get() = listOfNotNull(
            "$tempF°",
            summary.lowercase().replaceFirstChar { it.uppercase() },
            city,
        ).joinToString("  ·  ")

    companion object {
        const val HotF = 90
        const val ColdF = 32
        const val LikelyPrecip = 40
    }
}
