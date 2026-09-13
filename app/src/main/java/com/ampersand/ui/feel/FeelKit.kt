package com.ampersand.ui.feel

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * UI haptics by meaning. Names mirror [com.ampersand.ui.sound.Sfx]; several share
 * a haptic type because Android has few primitives.
 */
enum class Feel(
    private val type: HapticFeedbackType,
    /** API level that added [type]; below it [fallback] is used. */
    private val since: Int,
    private val fallback: HapticFeedbackType = HapticFeedbackType.KeyboardTap,
) {
    /** Any ordinary tap: a button, a pip, a menu row. */
    TAP(HapticFeedbackType.KeyboardTap, 8),

    /** Moving between tabs. */
    NAV(HapticFeedbackType.SegmentTick, 34),

    /** A choice turning on, and the same choice turning back off. */
    SELECT(HapticFeedbackType.ToggleOn, 34),
    DESELECT(HapticFeedbackType.ToggleOff, 34),

    /** A field taking focus. */
    FOCUS(HapticFeedbackType.TextHandleMove, 27),

    /** A brick leaving the tray, and the moment it lands. */
    PICK_UP(HapticFeedbackType.GestureThresholdActivate, 34, HapticFeedbackType.LongPress),
    PLACE(HapticFeedbackType.Confirm, 30, HapticFeedbackType.VirtualKey),

    /** A button going down, and coming back up. */
    PRESS(HapticFeedbackType.VirtualKey, 5),
    RELEASE(HapticFeedbackType.KeyboardTap, 8),

    /** Submitting an answer. */
    SEAL(HapticFeedbackType.Confirm, 30, HapticFeedbackType.LongPress),

    /** A refused action: invalid drop, pick past the limit, empty day. */
    REJECT(HapticFeedbackType.Reject, 30, HapticFeedbackType.LongPress),

    /** One step of progress or a drag crossing a notch. */
    TICK(HapticFeedbackType.SegmentFrequentTick, 34, HapticFeedbackType.KeyboardTap),
    ;

    /** [type] if supported, else [fallback]. Unsupported constants silently do nothing (minSdk 26). */
    internal fun resolve(): HapticFeedbackType =
        if (Build.VERSION.SDK_INT >= since) type else fallback
}

/** Plays haptics. Toggled separately from sound. */
class FeelKit(private val haptics: HapticFeedback) {

    var enabled by mutableStateOf(true)

    fun play(feel: Feel) {
        if (!enabled) return
        haptics.performHapticFeedback(feel.resolve())
    }
}

/** Null by default (previews, tests); use the null-safe [play] extension. */
val LocalFeel = staticCompositionLocalOf<FeelKit?> { null }

@Composable
fun rememberFeelKit(): FeelKit {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) { FeelKit(haptics) }
}

/** Convenience so call sites read as `feel.play(Feel.TAP)` even when absent. */
fun FeelKit?.play(feel: Feel) {
    this?.play(feel)
}
