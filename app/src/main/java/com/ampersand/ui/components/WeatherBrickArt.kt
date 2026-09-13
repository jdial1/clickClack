package com.ampersand.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ampersand.R
import com.ampersand.model.WeatherBrick

/** The day's sky as a brick. Drawn by tools/weather/bricks.py. */
@Composable
fun WeatherBrickArt(brick: WeatherBrick, modifier: Modifier = Modifier) {
    val id = when (brick) {
        WeatherBrick.SUNNY -> R.drawable.weather_sunny
        WeatherBrick.PARTLY_CLOUDY -> R.drawable.weather_partly_cloudy
        WeatherBrick.CLOUDY -> R.drawable.weather_cloudy
        WeatherBrick.RAINY -> R.drawable.weather_rainy
        WeatherBrick.STORMY -> R.drawable.weather_stormy
        WeatherBrick.SNOWY -> R.drawable.weather_snowy
        WeatherBrick.WINDY -> R.drawable.weather_windy
        WeatherBrick.FOGGY -> R.drawable.weather_foggy
        WeatherBrick.HOT -> R.drawable.weather_hot
        WeatherBrick.COLD -> R.drawable.weather_cold
        WeatherBrick.CLEAR_NIGHT -> R.drawable.weather_clear_night
    }
    Image(painterResource(id), contentDescription = null, modifier = modifier)
}
