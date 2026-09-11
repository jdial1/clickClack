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

/**
 * Every noise the UI can make, by meaning rather than by filename.
 *
 * Swapping a sound is a one-line change here, which matters because these were
 * chosen from the pack by name, not by ear.
 */
enum class Sfx(val res: Int, val volume: Float) {
    /** Any ordinary tap: a button, a pip, a menu row. */
    TAP(R.raw.sfx_click1, 0.5f),

    /** Moving between tabs. Quieter -- it fires often. */
    NAV(R.raw.sfx_click3, 0.32f),

    /** A choice turning on, and the same choice turning back off. */
    SELECT(R.raw.sfx_switch2, 0.55f),
    DESELECT(R.raw.sfx_switch3, 0.45f),

    /** A field taking focus. */
    FOCUS(R.raw.sfx_rollover4, 0.3f),

    /** Lifting a brick out of the tray. */
    PICK_UP(R.raw.sfx_rollover1, 0.6f),

    /** A brick snapping onto the plate. The one that should feel good. */
    PLACE(R.raw.sfx_switch11, 0.85f),

    /** Pressing and letting go of something chunky. */
    PRESS(R.raw.sfx_mouseclick1, 0.5f),
    RELEASE(R.raw.sfx_mouserelease1, 0.45f),

    /** Sealing the day, and the seal lifting. The two big moments. */
    SEAL(R.raw.sfx_switch33, 0.9f),

    /**
     * The two halves of the app's name, and the only sounds picked by ear
     * rather than by filename.
     *
     * Chosen by measuring the pack instead of guessing: every clip's duration
     * and bytes-per-second was read out of its Ogg pages, and these two are the
     * shortest and the most and least dense of the short ones -- 28ms bright
     * for the click, 37ms duller for the clack. Short means percussive; the
     * density gap is what makes them audibly a pair rather than the same sound
     * twice.
     */
    CLICK(R.raw.sfx_brick_click, 0.75f),
    CLACK(R.raw.sfx_brick_clack, 0.8f),
}

/**
 * Thin wrapper over [SoundPool].
 *
 * SoundPool rather than MediaPlayer because these are short, need to overlap,
 * and must not stall the UI thread. Sounds are decoded once up front; a play
 * before the pool finishes loading is dropped rather than queued, which is the
 * right trade for UI chirps -- a click that arrives late is worse than one that
 * never arrives.
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
     * Click, then clack: two bricks meeting.
     *
     * [gapMs] is the beat between them. Below about 50ms the pair fuses into
     * one thud and the name stops being audible in it; much above 140ms they
     * read as two separate events rather than one join.
     */
    suspend fun clickClack(gapMs: Long = 90) {
        play(Sfx.CLICK)
        kotlinx.coroutines.delay(gapMs)
        play(Sfx.CLACK)
    }

    fun release() {
        pool.release()
        loaded.clear()
        ready.clear()
    }
}

/**
 * No-op by default so previews and tests never touch the audio system, and any
 * composable can call [LocalSounds.current.play] without a null check.
 */
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

suspend fun SoundKit?.clickClack(gapMs: Long = 90) {
    this?.clickClack(gapMs)
}
