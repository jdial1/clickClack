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
 * Every way the UI can touch you back, by meaning rather than by waveform.
 *
 * Deliberately the same shape as [com.ampersand.ui.sound.Sfx]: the same names
 * for the same moments, so a call site that plays a sound has an obvious
 * haptic to sit beside it and the two cannot drift apart. Where a name has no
 * distinct feeling of its own it shares one -- Android has far fewer haptic
 * primitives than the pack has clips, and inventing a difference the hardware
 * cannot express would only make this table lie.
 */
enum class Feel(
    private val type: HapticFeedbackType,
    /** The API level the platform constant behind [type] arrived in. */
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

    /** A field taking focus. The lightest thing here. */
    FOCUS(HapticFeedbackType.TextHandleMove, 27),

    /** A brick leaving the tray, and the moment it lands. */
    PICK_UP(HapticFeedbackType.GestureThresholdActivate, 34, HapticFeedbackType.LongPress),
    PLACE(HapticFeedbackType.Confirm, 30, HapticFeedbackType.VirtualKey),

    /** A button going down, and coming back up. */
    PRESS(HapticFeedbackType.VirtualKey, 5),
    RELEASE(HapticFeedbackType.KeyboardTap, 8),

    /** Committing an answer. The heaviest, and the rarest. */
    SEAL(HapticFeedbackType.Confirm, 30, HapticFeedbackType.LongPress),

    /** "No": a drop that cannot land, a pick past the cap, an empty day. */
    REJECT(HapticFeedbackType.Reject, 30, HapticFeedbackType.LongPress),

    /** One notch of a thing filling up: a progress brick, a tower course. */
    TICK(HapticFeedbackType.SegmentFrequentTick, 34, HapticFeedbackType.KeyboardTap),
    ;

    /**
     * The best of these this phone actually has.
     *
     * minSdk is 26 and over half of the modern set landed in 30 or 34. An
     * unrecognised constant is not an error -- the platform simply does
     * nothing and returns false -- so without this the app would be silently
     * inert on older phones, which is the worst of the three outcomes.
     */
    internal fun resolve(): HapticFeedbackType =
        if (Build.VERSION.SDK_INT >= since) type else fallback
}

/**
 * The haptic half of the feedback pair.
 *
 * A separate switch from sound rather than one "feedback" switch, because the
 * two are wanted in different combinations: a phone on silent in a quiet room
 * still wants to feel the brick land, and someone who finds vibration
 * unpleasant still wants the click. It also fixes a small dead end -- turning
 * *sound* off could never confirm itself, for the obvious reason.
 */
class FeelKit(private val haptics: HapticFeedback) {

    var enabled by mutableStateOf(true)

    fun play(feel: Feel) {
        if (!enabled) return
        haptics.performHapticFeedback(feel.resolve())
    }
}

/**
 * No-op by default so previews and tests never buzz, and any composable can
 * call [LocalFeel.current.play] without a null check.
 */
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
