package com.ampersand.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.ampersand.R

/** UI sounds by meaning, with their clip and volume. */
enum class Sfx(val res: Int, val volume: Float) {
    /** Any ordinary tap: a button, a pip, a menu row. */
    TAP(R.raw.sfx_click1, 0.5f),

    /** Moving between tabs. Quieter because it fires often. */
    NAV(R.raw.sfx_click3, 0.32f),

    /** A choice turning on, and the same choice turning back off. */
    SELECT(R.raw.sfx_switch2, 0.55f),
    DESELECT(R.raw.sfx_switch3, 0.45f),

    /** A field taking focus. */
    FOCUS(R.raw.sfx_rollover4, 0.3f),

    /** Lifting a brick out of the tray. */
    PICK_UP(R.raw.sfx_rollover1, 0.6f),

    /** A brick snapping into place. */
    PLACE(R.raw.sfx_switch11, 0.85f),

    /** Pressing and letting go of something chunky. */
    PRESS(R.raw.sfx_mouseclick1, 0.5f),
    RELEASE(R.raw.sfx_mouserelease1, 0.45f),

    /** Submitting an answer. */
    SEAL(R.raw.sfx_switch33, 0.9f),

    /** Splash "click" (28ms, bright) and "clack" (37ms, duller). */
    CLICK(R.raw.sfx_brick_click, 0.75f),
    CLACK(R.raw.sfx_brick_clack, 0.8f),
}

/**
 * Thin wrapper over [SoundPool] for short, overlapping clips. All clips load up
 * front; [play] drops a clip that isn't loaded yet instead of queuing it.
 */
class SoundKit(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = HashMap<Sfx, Int>(Sfx.entries.size)
    private val ready = HashSet<Int>()

    var enabled by mutableStateOf(true)

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) ready += sampleId
        }
        Sfx.entries.forEach { sfx ->
            loaded[sfx] = pool.load(context, sfx.res, 1)
        }
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        val id = loaded[sfx] ?: return
        if (id !in ready) return
        pool.play(id, sfx.volume, sfx.volume, 1, 0, 1f)
    }

    /**
     * Waits up to [timeoutMs] for a clip to load; returns false on timeout.
     * Used by the splash, which was silent on cold starts because clips weren't loaded yet.
     */
    suspend fun awaitReady(sfx: Sfx, timeoutMs: Long = 2000): Boolean {
        val id = loaded[sfx] ?: return false
        val deadline = System.currentTimeMillis() + timeoutMs
        while (id !in ready && System.currentTimeMillis() < deadline) {
            kotlinx.coroutines.delay(16)
        }
        return id in ready
    }

    fun release() {
        pool.release()
        loaded.clear()
        ready.clear()
    }
}

/** Null by default (previews, tests); use the null-safe [play] extension. */
val LocalSounds = staticCompositionLocalOf<SoundKit?> { null }

@Composable
fun rememberSoundKit(): SoundKit {
    val context = LocalContext.current
    val kit = remember { SoundKit(context.applicationContext) }
    DisposableEffect(kit) { onDispose { kit.release() } }
    return kit
}

/** Convenience so call sites read as `sounds.play(Sfx.TAP)` even when absent. */
fun SoundKit?.play(sfx: Sfx) {
    this?.play(sfx)
}

suspend fun SoundKit?.awaitReady(sfx: Sfx, timeoutMs: Long = 2000) {
    this?.awaitReady(sfx, timeoutMs)
}
