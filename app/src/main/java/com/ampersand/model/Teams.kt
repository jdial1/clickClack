package com.ampersand.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class League { MLB, NFL }

/** A team and where its home field is. [nickname] is what a question says: "the Pirates". */
data class Team(val nickname: String, val city: String, val league: League, val lat: Double, val lon: Double)

/**
 * The local team, by distance to the ballpark.
 *
 * Not the broadcast territories: MLB's blackout map is MLB's own data and is
 * not published as a dataset anywhere we could use. Nearest stadium is the
 * honest approximation -- right for almost everyone, and wrong mostly in the
 * places where people already argue about it (a Queens couple gets the Mets,
 * the Bronx gets the Yankees).
 *
 * Coordinates are each home field's, as of the 2026 season.
 */
object Teams {

    val all: List<Team> = listOf(
        Team("Diamondbacks", "Arizona", League.MLB, 33.445, -112.067),
        Team("Braves", "Atlanta", League.MLB, 33.891, -84.468),
        Team("Orioles", "Baltimore", League.MLB, 39.284, -76.622),
        Team("Red Sox", "Boston", League.MLB, 42.346, -71.097),
        Team("Cubs", "Chicago", League.MLB, 41.948, -87.656),
        Team("White Sox", "Chicago", League.MLB, 41.830, -87.634),
        Team("Reds", "Cincinnati", League.MLB, 39.097, -84.507),
        Team("Guardians", "Cleveland", League.MLB, 41.496, -81.685),
        Team("Rockies", "Colorado", League.MLB, 39.756, -104.994),
        Team("Tigers", "Detroit", League.MLB, 42.339, -83.049),
        Team("Astros", "Houston", League.MLB, 29.757, -95.355),
        Team("Royals", "Kansas City", League.MLB, 39.051, -94.480),
        Team("Angels", "Los Angeles", League.MLB, 33.800, -117.883),
        Team("Dodgers", "Los Angeles", League.MLB, 34.074, -118.240),
        Team("Marlins", "Miami", League.MLB, 25.778, -80.220),
        Team("Brewers", "Milwaukee", League.MLB, 43.028, -87.971),
        Team("Twins", "Minnesota", League.MLB, 44.982, -93.278),
        Team("Mets", "New York", League.MLB, 40.757, -73.846),
        Team("Yankees", "New York", League.MLB, 40.829, -73.926),
        Team("Athletics", "Sacramento", League.MLB, 38.580, -121.513),
        Team("Phillies", "Philadelphia", League.MLB, 39.906, -75.166),
        Team("Pirates", "Pittsburgh", League.MLB, 40.447, -80.006),
        Team("Padres", "San Diego", League.MLB, 32.707, -117.157),
        Team("Giants", "San Francisco", League.MLB, 37.778, -122.389),
        Team("Mariners", "Seattle", League.MLB, 47.591, -122.332),
        Team("Cardinals", "St. Louis", League.MLB, 38.623, -90.193),
        Team("Rays", "Tampa Bay", League.MLB, 27.768, -82.653),
        Team("Rangers", "Texas", League.MLB, 32.747, -97.084),
        Team("Blue Jays", "Toronto", League.MLB, 43.641, -79.389),
        Team("Nationals", "Washington", League.MLB, 38.873, -77.007),

        Team("Cardinals", "Arizona", League.NFL, 33.528, -112.263),
        Team("Falcons", "Atlanta", League.NFL, 33.755, -84.401),
        Team("Ravens", "Baltimore", League.NFL, 39.278, -76.623),
        Team("Bills", "Buffalo", League.NFL, 42.774, -78.787),
        Team("Panthers", "Carolina", League.NFL, 35.226, -80.853),
        Team("Bears", "Chicago", League.NFL, 41.862, -87.617),
        Team("Bengals", "Cincinnati", League.NFL, 39.095, -84.516),
        Team("Browns", "Cleveland", League.NFL, 41.506, -81.700),
        Team("Cowboys", "Dallas", League.NFL, 32.748, -97.093),
        Team("Broncos", "Denver", League.NFL, 39.744, -105.020),
        Team("Lions", "Detroit", League.NFL, 42.340, -83.046),
        Team("Packers", "Green Bay", League.NFL, 44.501, -88.062),
        Team("Texans", "Houston", League.NFL, 29.685, -95.411),
        Team("Colts", "Indianapolis", League.NFL, 39.760, -86.164),
        Team("Jaguars", "Jacksonville", League.NFL, 30.324, -81.637),
        Team("Chiefs", "Kansas City", League.NFL, 39.049, -94.484),
        Team("Raiders", "Las Vegas", League.NFL, 36.091, -115.184),
        Team("Chargers", "Los Angeles", League.NFL, 33.953, -118.339),
        Team("Rams", "Los Angeles", League.NFL, 33.954, -118.338),
        Team("Dolphins", "Miami", League.NFL, 25.958, -80.239),
        Team("Vikings", "Minnesota", League.NFL, 44.974, -93.258),
        Team("Patriots", "New England", League.NFL, 42.091, -71.264),
        Team("Saints", "New Orleans", League.NFL, 29.951, -90.081),
        Team("Giants", "New York", League.NFL, 40.813, -74.074),
        Team("Jets", "New York", League.NFL, 40.814, -74.073),
        Team("Eagles", "Philadelphia", League.NFL, 39.901, -75.168),
        Team("Steelers", "Pittsburgh", League.NFL, 40.447, -80.016),
        Team("49ers", "San Francisco", League.NFL, 37.403, -121.970),
        Team("Seahawks", "Seattle", League.NFL, 47.595, -122.332),
        Team("Buccaneers", "Tampa Bay", League.NFL, 27.976, -82.503),
        Team("Titans", "Tennessee", League.NFL, 36.166, -86.771),
        Team("Commanders", "Washington", League.NFL, 38.908, -76.864),
    )

    /** The nearest team in [league] to [place]. Two teams sharing a stadium resolve by list order. */
    fun nearest(place: Place, league: League): Team =
        all.filter { it.league == league }.minBy { milesBetween(place.lat, place.lon, it.lat, it.lon) }

    /** Great-circle distance, in miles. */
    fun milesBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}

/**
 * A question that names the local teams, filled in for where you live.
 *
 * `{mlb}` and `{nfl}` become "the Pirates" and "the Steelers". Both phones
 * share the home ZIP, so both fill in the same names.
 */
fun Question.forPlace(place: Place?): Question {
    if ("{mlb}" !in text && "{nfl}" !in text && options.none { "{mlb}" in it || "{nfl}" in it }) return this
    if (place == null) return this
    val mlb = "the " + Teams.nearest(place, League.MLB).nickname
    val nfl = "the " + Teams.nearest(place, League.NFL).nickname
    fun fill(s: String) = s.replace("{mlb}", mlb).replace("{nfl}", nfl)
        .let { if (it.startsWith("the ")) it.replaceFirstChar { c -> c.uppercase() } else it }
    return copy(text = fill(text), options = options.map(::fill))
}

/** Whether a question only makes sense once a home ZIP is set. */
val Question.needsPlace: Boolean
    get() = "{mlb}" in text || "{nfl}" in text || options.any { "{mlb}" in it || "{nfl}" in it }
