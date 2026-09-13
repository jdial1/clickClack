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

/** Theme mode. [SYSTEM] (follow the phone) is the default. */
enum class ThemeMode(val label: String) {
    SYSTEM("Automatic"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** The chosen theme mode, persisted in SharedPreferences (unlike sound and haptics). */
class Appearance(private val context: Context) {

    var mode: ThemeMode by mutableStateOf(read(context))
        private set

    fun set(next: ThemeMode) {
        mode = next
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, next.name).apply()
    }

    /** Whether to use dark theme; [ThemeMode.SYSTEM] follows the phone. */
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

/** Null in previews and tests. */
val LocalAppearance = staticCompositionLocalOf<Appearance?> { null }

@Composable
fun rememberAppearance(context: Context): Appearance =
    remember(context) { Appearance(context.applicationContext) }
