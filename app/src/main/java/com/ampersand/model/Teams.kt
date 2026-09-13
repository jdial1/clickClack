package com.ampersand.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A league, its token in question text, and the months it is worth asking about.
 *
 * Months rather than exact dates: seasons drift by a week or two each year, and
 * a hockey question in the first week of October is not wrong, just early.
 */
enum class League(val token: String, val months: Set<Int>) {
    MLB("{mlb}", (3..10).toSet()),
    NFL("{nfl}", setOf(9, 10, 11, 12, 1, 2)),
    NBA("{nba}", setOf(10, 11, 12, 1, 2, 3, 4, 5, 6)),
    NHL("{nhl}", setOf(10, 11, 12, 1, 2, 3, 4, 5, 6)),
    MLS("{mls}", (2..11).toSet()),
    COLLEGE("{college}", setOf(8, 9, 10, 11, 12, 1)),
    ;

    fun inSeason(date: java.time.LocalDate): Boolean = date.monthValue in months
}

/** What a person would tick: "Hockey (NHL)". */
val League.label: String
    get() = when (this) {
        League.MLB -> "Baseball (MLB)"
        League.NFL -> "Pro football (NFL)"
        League.COLLEGE -> "College football"
        League.NBA -> "Basketball (NBA)"
        League.NHL -> "Hockey (NHL)"
        League.MLS -> "Soccer (MLS)"
    }

/**
 * Which sport questions the draw may use: leagues one of you follows, that have
 * a local team in season. And whether sports belongs in the daily question at
 * all -- not by default: only once one of you follows something, or asked for
 * more sports outright.
 */
object Following {
    fun leagues(active: Set<League>, followed: Set<League>): Set<League> = active intersect followed

    fun hideSports(followed: Set<League>, interests: Set<Topic>): Boolean =
        followed.isEmpty() && Topic.SPORTS !in interests
}

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

        Team("Hawks", "Atlanta", League.NBA, 33.757, -84.396),
        Team("Celtics", "Boston", League.NBA, 42.366, -71.062),
        Team("Nets", "Brooklyn", League.NBA, 40.683, -73.975),
        Team("Hornets", "Charlotte", League.NBA, 35.225, -80.839),
        Team("Bulls", "Chicago", League.NBA, 41.881, -87.674),
        Team("Cavaliers", "Cleveland", League.NBA, 41.496, -81.688),
        Team("Mavericks", "Dallas", League.NBA, 32.790, -96.810),
        Team("Nuggets", "Denver", League.NBA, 39.749, -105.008),
        Team("Pistons", "Detroit", League.NBA, 42.341, -83.055),
        Team("Warriors", "Golden State", League.NBA, 37.768, -122.388),
        Team("Rockets", "Houston", League.NBA, 29.751, -95.362),
        Team("Pacers", "Indiana", League.NBA, 39.764, -86.155),
        Team("Clippers", "Los Angeles", League.NBA, 33.945, -118.341),
        Team("Lakers", "Los Angeles", League.NBA, 34.043, -118.267),
        Team("Grizzlies", "Memphis", League.NBA, 35.138, -90.051),
        Team("Heat", "Miami", League.NBA, 25.781, -80.188),
        Team("Bucks", "Milwaukee", League.NBA, 43.045, -87.917),
        Team("Timberwolves", "Minnesota", League.NBA, 44.980, -93.276),
        Team("Pelicans", "New Orleans", League.NBA, 29.949, -90.082),
        Team("Knicks", "New York", League.NBA, 40.751, -73.994),
        Team("Thunder", "Oklahoma City", League.NBA, 35.463, -97.515),
        Team("Magic", "Orlando", League.NBA, 28.539, -81.384),
        Team("76ers", "Philadelphia", League.NBA, 39.901, -75.172),
        Team("Suns", "Phoenix", League.NBA, 33.446, -112.071),
        Team("Trail Blazers", "Portland", League.NBA, 45.532, -122.667),
        Team("Kings", "Sacramento", League.NBA, 38.580, -121.500),
        Team("Spurs", "San Antonio", League.NBA, 29.427, -98.438),
        Team("Raptors", "Toronto", League.NBA, 43.643, -79.379),
        Team("Jazz", "Utah", League.NBA, 40.768, -111.901),
        Team("Wizards", "Washington", League.NBA, 38.898, -77.021),

        Team("Ducks", "Anaheim", League.NHL, 33.808, -117.877),
        Team("Bruins", "Boston", League.NHL, 42.366, -71.062),
        Team("Sabres", "Buffalo", League.NHL, 42.875, -78.876),
        Team("Flames", "Calgary", League.NHL, 51.037, -114.052),
        Team("Hurricanes", "Carolina", League.NHL, 35.803, -78.722),
        Team("Blackhawks", "Chicago", League.NHL, 41.881, -87.674),
        Team("Avalanche", "Colorado", League.NHL, 39.749, -105.008),
        Team("Blue Jackets", "Columbus", League.NHL, 39.969, -83.006),
        Team("Stars", "Dallas", League.NHL, 32.790, -96.810),
        Team("Red Wings", "Detroit", League.NHL, 42.341, -83.055),
        Team("Oilers", "Edmonton", League.NHL, 53.547, -113.498),
        Team("Panthers", "Florida", League.NHL, 26.158, -80.326),
        Team("Kings", "Los Angeles", League.NHL, 34.043, -118.267),
        Team("Wild", "Minnesota", League.NHL, 44.945, -93.101),
        Team("Canadiens", "Montreal", League.NHL, 45.496, -73.569),
        Team("Predators", "Nashville", League.NHL, 36.159, -86.779),
        Team("Devils", "New Jersey", League.NHL, 40.734, -74.171),
        Team("Islanders", "New York", League.NHL, 40.717, -73.726),
        Team("Rangers", "New York", League.NHL, 40.751, -73.994),
        Team("Senators", "Ottawa", League.NHL, 45.297, -75.927),
        Team("Flyers", "Philadelphia", League.NHL, 39.901, -75.172),
        Team("Penguins", "Pittsburgh", League.NHL, 40.439, -79.989),
        Team("Sharks", "San Jose", League.NHL, 37.333, -121.901),
        Team("Kraken", "Seattle", League.NHL, 47.622, -122.354),
        Team("Blues", "St. Louis", League.NHL, 38.627, -90.203),
        Team("Lightning", "Tampa Bay", League.NHL, 27.943, -82.452),
        Team("Maple Leafs", "Toronto", League.NHL, 43.643, -79.379),
        Team("Mammoth", "Utah", League.NHL, 40.768, -111.901),
        Team("Canucks", "Vancouver", League.NHL, 49.278, -123.109),
        Team("Golden Knights", "Vegas", League.NHL, 36.103, -115.178),
        Team("Capitals", "Washington", League.NHL, 38.898, -77.021),
        Team("Jets", "Winnipeg", League.NHL, 49.893, -97.144),

        Team("Atlanta United", "Atlanta", League.MLS, 33.755, -84.401),
        Team("Austin FC", "Austin", League.MLS, 30.388, -97.720),
        Team("CF Montreal", "Montreal", League.MLS, 45.563, -73.553),
        Team("Charlotte FC", "Charlotte", League.MLS, 35.226, -80.853),
        Team("Chicago Fire", "Chicago", League.MLS, 41.862, -87.617),
        Team("FC Cincinnati", "Cincinnati", League.MLS, 39.111, -84.522),
        Team("Colorado Rapids", "Colorado", League.MLS, 39.806, -104.892),
        Team("Columbus Crew", "Columbus", League.MLS, 39.968, -83.017),
        Team("D.C. United", "Washington", League.MLS, 38.868, -77.013),
        Team("FC Dallas", "Dallas", League.MLS, 33.155, -96.835),
        Team("Houston Dynamo", "Houston", League.MLS, 29.752, -95.352),
        Team("Inter Miami", "Miami", League.MLS, 25.790, -80.300),
        Team("LA Galaxy", "Los Angeles", League.MLS, 33.864, -118.261),
        Team("LAFC", "Los Angeles", League.MLS, 34.013, -118.285),
        Team("Minnesota United", "Minnesota", League.MLS, 44.953, -93.165),
        Team("Nashville SC", "Nashville", League.MLS, 36.130, -86.765),
        Team("New England Revolution", "New England", League.MLS, 42.092, -71.265),
        Team("New York City FC", "New York", League.MLS, 40.830, -73.927),
        Team("New York Red Bulls", "New York", League.MLS, 40.737, -74.150),
        Team("Orlando City", "Orlando", League.MLS, 28.541, -81.389),
        Team("Philadelphia Union", "Philadelphia", League.MLS, 39.832, -75.379),
        Team("Portland Timbers", "Portland", League.MLS, 45.521, -122.692),
        Team("Real Salt Lake", "Salt Lake", League.MLS, 40.583, -111.893),
        Team("San Diego FC", "San Diego", League.MLS, 32.784, -117.119),
        Team("San Jose Earthquakes", "San Jose", League.MLS, 37.351, -121.925),
        Team("Seattle Sounders", "Seattle", League.MLS, 47.596, -122.331),
        Team("Sporting Kansas City", "Kansas City", League.MLS, 39.122, -94.823),
        Team("St. Louis City SC", "St. Louis", League.MLS, 38.631, -90.211),
        Team("Toronto FC", "Toronto", League.MLS, 43.633, -79.419),
        Team("Vancouver Whitecaps", "Vancouver", League.MLS, 49.277, -123.112),

        // Major-conference college football, by school. `city` holds the
        // school name, which is what a question says: "Ohio State football".
        Team("Crimson Tide", "Alabama", League.COLLEGE, 33.208, -87.550),
        Team("Razorbacks", "Arkansas", League.COLLEGE, 36.068, -94.179),
        Team("Tigers", "Auburn", League.COLLEGE, 32.602, -85.489),
        Team("Gators", "Florida", League.COLLEGE, 29.650, -82.349),
        Team("Bulldogs", "Georgia", League.COLLEGE, 33.950, -83.373),
        Team("Wildcats", "Kentucky", League.COLLEGE, 38.022, -84.505),
        Team("Tigers", "LSU", League.COLLEGE, 30.412, -91.184),
        Team("Rebels", "Ole Miss", League.COLLEGE, 34.362, -89.534),
        Team("Bulldogs", "Mississippi State", League.COLLEGE, 33.456, -88.794),
        Team("Tigers", "Missouri", League.COLLEGE, 38.936, -92.333),
        Team("Sooners", "Oklahoma", League.COLLEGE, 35.206, -97.442),
        Team("Gamecocks", "South Carolina", League.COLLEGE, 33.973, -81.019),
        Team("Volunteers", "Tennessee", League.COLLEGE, 35.955, -83.925),
        Team("Longhorns", "Texas", League.COLLEGE, 30.284, -97.733),
        Team("Aggies", "Texas A&M", League.COLLEGE, 30.610, -96.340),
        Team("Commodores", "Vanderbilt", League.COLLEGE, 36.144, -86.809),
        Team("Fighting Illini", "Illinois", League.COLLEGE, 40.099, -88.236),
        Team("Hoosiers", "Indiana", League.COLLEGE, 39.181, -86.526),
        Team("Hawkeyes", "Iowa", League.COLLEGE, 41.659, -91.551),
        Team("Terrapins", "Maryland", League.COLLEGE, 38.990, -76.947),
        Team("Wolverines", "Michigan", League.COLLEGE, 42.266, -83.749),
        Team("Spartans", "Michigan State", League.COLLEGE, 42.728, -84.485),
        Team("Golden Gophers", "Minnesota", League.COLLEGE, 44.977, -93.225),
        Team("Cornhuskers", "Nebraska", League.COLLEGE, 40.821, -96.706),
        Team("Wildcats", "Northwestern", League.COLLEGE, 42.066, -87.692),
        Team("Buckeyes", "Ohio State", League.COLLEGE, 40.002, -83.020),
        Team("Ducks", "Oregon", League.COLLEGE, 44.058, -123.068),
        Team("Nittany Lions", "Penn State", League.COLLEGE, 40.812, -77.856),
        Team("Boilermakers", "Purdue", League.COLLEGE, 40.435, -86.919),
        Team("Scarlet Knights", "Rutgers", League.COLLEGE, 40.514, -74.465),
        Team("Bruins", "UCLA", League.COLLEGE, 34.161, -118.168),
        Team("Trojans", "USC", League.COLLEGE, 34.014, -118.288),
        Team("Huskies", "Washington", League.COLLEGE, 47.650, -122.302),
        Team("Badgers", "Wisconsin", League.COLLEGE, 43.070, -89.413),
        Team("Wildcats", "Arizona", League.COLLEGE, 32.229, -110.949),
        Team("Sun Devils", "Arizona State", League.COLLEGE, 33.426, -111.933),
        Team("Bears", "Baylor", League.COLLEGE, 31.558, -97.116),
        Team("Cougars", "BYU", League.COLLEGE, 40.258, -111.655),
        Team("Bearcats", "Cincinnati", League.COLLEGE, 39.131, -84.516),
        Team("Buffaloes", "Colorado", League.COLLEGE, 40.009, -105.267),
        Team("Cougars", "Houston", League.COLLEGE, 29.722, -95.349),
        Team("Cyclones", "Iowa State", League.COLLEGE, 42.014, -93.636),
        Team("Jayhawks", "Kansas", League.COLLEGE, 38.963, -95.252),
        Team("Wildcats", "Kansas State", League.COLLEGE, 39.202, -96.594),
        Team("Cowboys", "Oklahoma State", League.COLLEGE, 36.126, -97.066),
        Team("Horned Frogs", "TCU", League.COLLEGE, 32.710, -97.368),
        Team("Red Raiders", "Texas Tech", League.COLLEGE, 33.591, -101.873),
        Team("Knights", "UCF", League.COLLEGE, 28.608, -81.193),
        Team("Utes", "Utah", League.COLLEGE, 40.760, -111.849),
        Team("Mountaineers", "West Virginia", League.COLLEGE, 39.650, -79.955),
        Team("Eagles", "Boston College", League.COLLEGE, 42.335, -71.166),
        Team("Golden Bears", "Cal", League.COLLEGE, 37.871, -122.251),
        Team("Tigers", "Clemson", League.COLLEGE, 34.679, -82.843),
        Team("Blue Devils", "Duke", League.COLLEGE, 35.995, -78.942),
        Team("Seminoles", "Florida State", League.COLLEGE, 30.438, -84.304),
        Team("Yellow Jackets", "Georgia Tech", League.COLLEGE, 33.773, -84.393),
        Team("Cardinals", "Louisville", League.COLLEGE, 38.206, -85.759),
        Team("Hurricanes", "Miami", League.COLLEGE, 25.958, -80.239),
        Team("Wolfpack", "NC State", League.COLLEGE, 35.800, -78.720),
        Team("Tar Heels", "North Carolina", League.COLLEGE, 35.907, -79.048),
        Team("Panthers", "Pitt", League.COLLEGE, 40.447, -80.016),
        Team("Mustangs", "SMU", League.COLLEGE, 32.837, -96.783),
        Team("Cardinal", "Stanford", League.COLLEGE, 37.435, -122.161),
        Team("Orange", "Syracuse", League.COLLEGE, 43.036, -76.136),
        Team("Cavaliers", "Virginia", League.COLLEGE, 38.031, -78.514),
        Team("Hokies", "Virginia Tech", League.COLLEGE, 37.220, -80.418),
        Team("Demon Deacons", "Wake Forest", League.COLLEGE, 36.130, -80.255),
        Team("Fighting Irish", "Notre Dame", League.COLLEGE, 41.698, -86.234),
    )

    /** Farther than this and the nearest team is not a local team, just the least far one. */
    const val LocalMiles = 300.0

    /** The nearest team in [league], if it is close enough to count as local. */
    fun local(place: Place, league: League): Team? =
        nearest(place, league).takeIf { milesBetween(place.lat, place.lon, it.lat, it.lon) <= LocalMiles }

    /** Leagues with a local team that are in season on [date]. What decides which team questions can be asked. */
    fun activeLeagues(place: Place, date: java.time.LocalDate): Set<League> =
        League.entries.filter { it.inSeason(date) && local(place, it) != null }.toSet()

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
fun Question.forPlace(place: Place?, date: java.time.LocalDate? = null): Question {
    if (!needsPlace || place == null) return this
    val names = League.entries.associate { league ->
        league.token to Teams.nearest(place, league).let {
            when (league) {
                League.COLLEGE -> it.city // "Ohio State football"
                League.MLS -> it.nickname // club names carry their own city: "Columbus Crew"
                else -> "the ${it.nickname}"
            }
        }
    }
    val light = date?.let { Sun.daylight(it, place.lat, place.lon) }
    fun fill(s: String): String {
        var out = s
        names.forEach { (token, name) -> out = out.replace(token, name) }
        light?.let {
            out = out.replace("{sunset}", it.sunsetText).replace("{sunrise}", it.sunriseText)
                .replace("{daylight}", it.lengthText)
        }
        return if (out.startsWith("the ")) out.replaceFirstChar { c -> c.uppercase() } else out
    }
    return copy(text = fill(text), options = options.map(::fill))
}

private val PlaceTokens = League.entries.map { it.token } + listOf("{sunset}", "{sunrise}", "{daylight}")

/** Whether a question only makes sense once a home ZIP is set. */
val Question.needsPlace: Boolean
    get() = PlaceTokens.any { t -> t in text || options.any { t in it } }

/** Which leagues a question names. It is only asked while all of them are local and in season. */
val Question.leagues: Set<League>
    get() = League.entries.filter { l -> l.token in text || options.any { l.token in it } }.toSet()
