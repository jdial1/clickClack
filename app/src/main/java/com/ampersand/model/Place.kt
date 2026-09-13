package com.ampersand.model

/**
 * The part of the country a couple lives in, for questions that only make sense
 * there: which team, which storm, which way you say soda.
 *
 * Six rather than the Census's four. "The South" is one region to the Census
 * and several to anyone who lives in it, but six is as far as the questions
 * can honestly go before they start pretending to know your town.
 */
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
 * Every ZIP the Census Bureau maps, bundled, so turning a ZIP into a forecast
 * needs no location permission and no second service.
 *
 * Built from the 2023 ZCTA gazetteer (centroids) and the 2020 ZCTA-to-county
 * relationship file (state), both public domain. A ZCTA is not quite a postal
 * ZIP -- PO-box-only ZIPs have none -- so an unknown ZIP falls back to the
 * first mapped one sharing its first three digits, which is the same sorting
 * area and close enough for weather.
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

/** The four kinds of day worth a question of their own. */
enum class WeatherKind(val label: String) { SNOW("Snow"), RAIN("Rain"), HOT("Hot"), COLD("Cold") }

/**
 * The day's forecast, as saved on the day so both phones read the same one.
 *
 * [kind] is worked out from the words and the temperature, in that order:
 * snow outranks rain, and either outranks a number, because a hot rainy day is
 * a rainy day to anyone deciding whether to go out.
 */
data class DayWeather(
    val tempF: Int,
    val summary: String,
    val city: String? = null,
    val precipChance: Int? = null,
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
