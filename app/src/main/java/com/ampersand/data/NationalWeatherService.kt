package com.ampersand.data

import com.ampersand.model.DayWeather
import com.ampersand.model.Place
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Today's forecast from api.weather.gov.
 *
 * Free, no key, public domain, and covering every US ZIP -- which is the whole
 * country this app is for. Two requests: the point tells us which forecast
 * office and grid square a location belongs to, and the forecast for that
 * square gives today's period. NWS asks for a User-Agent that says who is
 * calling, and refuses requests without one.
 *
 * Blocking, so call it off the main thread. Throws on anything unexpected;
 * the caller treats that as a day with no weather.
 */
object NationalWeatherService {

    private const val Agent = "ClickClack/0.1 (personal couples app)"

    fun today(place: Place): DayWeather {
        val point = get("https://api.weather.gov/points/%.4f,%.4f".format(place.lat, place.lon))
            .getJSONObject("properties")
        val city = point.optJSONObject("relativeLocation")?.optJSONObject("properties")?.optString("city")
            ?.takeIf { it.isNotBlank() }
        val period = get(point.getString("forecast"))
            .getJSONObject("properties")
            .getJSONArray("periods")
            .getJSONObject(0)
        return DayWeather(
            tempF = period.getInt("temperature"),
            summary = period.getString("shortForecast"),
            city = city,
            precipChance = period.optJSONObject("probabilityOfPrecipitation")
                ?.takeUnless { it.isNull("value") }?.optInt("value"),
            daytime = period.optBoolean("isDaytime", true),
        )
    }

    private fun get(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", Agent)
            connection.setRequestProperty("Accept", "application/geo+json")
            check(connection.responseCode == 200) { "NWS ${connection.responseCode} for $url" }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }
}
