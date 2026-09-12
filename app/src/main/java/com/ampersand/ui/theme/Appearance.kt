package com.ampersand.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

private const val PREFS = "appearance"
private const val KEY_MODE = "mode"

/**
 * Light, dark, or whatever the phone is doing.
 *
 * [SYSTEM] is the default and stays a real option rather than an implied one.
 * A two-way switch has to pick a side the moment you touch it, and someone who
 * lets their phone go dark at sunset is not choosing dark -- they are choosing
 * to stop choosing. Taking that away is the sort of thing that gets a setting
 * turned back off.
 */
enum class ThemeMode(val label: String) {
    SYSTEM("Automatic"),
    LIGHT("Light"),
    DARK("Dark"),
}

/**
 * The chosen mode, and the one thing in the app that has to outlive a launch.
 *
 * Sound and vibration do not persist: they are moment-to-moment, and a default
 * of "on" is right every morning. An appearance that reset every launch would
 * simply be broken -- you would set it, leave, come back, and find the app had
 * forgotten. So this one goes to disk.
 */
class Appearance(private val context: Context) {

    var mode: ThemeMode by mutableStateOf(read(context))
        private set

    fun set(next: ThemeMode) {
        mode = next
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, next.name).apply()
    }

    /** Resolved against the phone, which only [ThemeMode.SYSTEM] asks about. */
    @Composable
    fun isDark(): Boolean = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    private companion object {
        fun read(context: Context): ThemeMode {
            val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MODE, null)
            return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
        }
    }
}

/**
 * Null in previews and tests, which then render whatever the preview asked for
 * rather than reaching for a phone setting that is not there.
 */
val LocalAppearance = staticCompositionLocalOf<Appearance?> { null }

@Composable
fun rememberAppearance(context: Context): Appearance =
    remember(context) { Appearance(context.applicationContext) }
