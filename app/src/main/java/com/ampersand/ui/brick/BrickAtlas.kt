package com.ampersand.ui.brick

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.ampersand.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Asset name to drawable id.
 *
 * Written out rather than resolved with getIdentifier() so a missing or
 * renamed asset is a compile error instead of a blank brick at runtime.
 * Generated from the pack file list; regenerate if the pack changes.
 */
internal val BrickDrawables: Map<String, Int> = mapOf(
    "black_brick_high_1" to R.drawable.black_brick_high_1,
    "black_brick_high_2" to R.drawable.black_brick_high_2,
    "black_brick_high_4" to R.drawable.black_brick_high_4,
    "black_brick_high_6" to R.drawable.black_brick_high_6,
    "black_brick_high_slope_inverted_left_2" to R.drawable.black_brick_high_slope_inverted_left_2,
    "black_brick_high_slope_inverted_left_4" to R.drawable.black_brick_high_slope_inverted_left_4,
    "black_brick_high_slope_inverted_right_2" to R.drawable.black_brick_high_slope_inverted_right_2,
    "black_brick_high_slope_inverted_right_4" to R.drawable.black_brick_high_slope_inverted_right_4,
    "black_brick_high_slope_left_2" to R.drawable.black_brick_high_slope_left_2,
    "black_brick_high_slope_left_4" to R.drawable.black_brick_high_slope_left_4,
    "black_brick_high_slope_right_2" to R.drawable.black_brick_high_slope_right_2,
    "black_brick_high_slope_right_4" to R.drawable.black_brick_high_slope_right_4,
    "black_brick_low_1" to R.drawable.black_brick_low_1,
    "black_brick_low_2" to R.drawable.black_brick_low_2,
    "black_brick_low_4" to R.drawable.black_brick_low_4,
    "black_brick_low_6" to R.drawable.black_brick_low_6,
    "black_brick_medium_1" to R.drawable.black_brick_medium_1,
    "black_brick_medium_2" to R.drawable.black_brick_medium_2,
    "black_brick_medium_4" to R.drawable.black_brick_medium_4,
    "black_brick_medium_6" to R.drawable.black_brick_medium_6,
    "black_brick_medium_slope_inverted_left_2" to R.drawable.black_brick_medium_slope_inverted_left_2,
    "black_brick_medium_slope_inverted_left_4" to R.drawable.black_brick_medium_slope_inverted_left_4,
    "black_brick_medium_slope_inverted_right_2" to R.drawable.black_brick_medium_slope_inverted_right_2,
    "black_brick_medium_slope_inverted_right_4" to R.drawable.black_brick_medium_slope_inverted_right_4,
    "black_brick_medium_slope_left_2" to R.drawable.black_brick_medium_slope_left_2,
    "black_brick_medium_slope_left_4" to R.drawable.black_brick_medium_slope_left_4,
    "black_brick_medium_slope_right_2" to R.drawable.black_brick_medium_slope_right_2,
    "black_brick_medium_slope_right_4" to R.drawable.black_brick_medium_slope_right_4,
    "blue_brick_high_1" to R.drawable.blue_brick_high_1,
    "blue_brick_high_2" to R.drawable.blue_brick_high_2,
    "blue_brick_high_4" to R.drawable.blue_brick_high_4,
    "blue_brick_high_6" to R.drawable.blue_brick_high_6,
    "blue_brick_high_slope_inverted_left_2" to R.drawable.blue_brick_high_slope_inverted_left_2,
    "blue_brick_high_slope_inverted_left_4" to R.drawable.blue_brick_high_slope_inverted_left_4,
    "blue_brick_high_slope_inverted_right_2" to R.drawable.blue_brick_high_slope_inverted_right_2,
    "blue_brick_high_slope_inverted_right_4" to R.drawable.blue_brick_high_slope_inverted_right_4,
    "blue_brick_high_slope_left_2" to R.drawable.blue_brick_high_slope_left_2,
    "blue_brick_high_slope_left_4" to R.drawable.blue_brick_high_slope_left_4,
    "blue_brick_high_slope_right_2" to R.drawable.blue_brick_high_slope_right_2,
    "blue_brick_high_slope_right_4" to R.drawable.blue_brick_high_slope_right_4,
    "blue_brick_low_1" to R.drawable.blue_brick_low_1,
    "blue_brick_low_2" to R.drawable.blue_brick_low_2,
    "blue_brick_low_4" to R.drawable.blue_brick_low_4,
    "blue_brick_low_6" to R.drawable.blue_brick_low_6,
    "blue_brick_medium_1" to R.drawable.blue_brick_medium_1,
    "blue_brick_medium_2" to R.drawable.blue_brick_medium_2,
    "blue_brick_medium_4" to R.drawable.blue_brick_medium_4,
    "blue_brick_medium_6" to R.drawable.blue_brick_medium_6,
    "blue_brick_medium_slope_inverted_left_2" to R.drawable.blue_brick_medium_slope_inverted_left_2,
    "blue_brick_medium_slope_inverted_left_4" to R.drawable.blue_brick_medium_slope_inverted_left_4,
    "blue_brick_medium_slope_inverted_right_2" to R.drawable.blue_brick_medium_slope_inverted_right_2,
    "blue_brick_medium_slope_inverted_right_4" to R.drawable.blue_brick_medium_slope_inverted_right_4,
    "blue_brick_medium_slope_left_2" to R.drawable.blue_brick_medium_slope_left_2,
    "blue_brick_medium_slope_left_4" to R.drawable.blue_brick_medium_slope_left_4,
    "blue_brick_medium_slope_right_2" to R.drawable.blue_brick_medium_slope_right_2,
    "blue_brick_medium_slope_right_4" to R.drawable.blue_brick_medium_slope_right_4,
    "brick_high_1" to R.drawable.brick_high_1,
    "brick_high_2" to R.drawable.brick_high_2,
    "brick_high_4" to R.drawable.brick_high_4,
    "brick_high_6" to R.drawable.brick_high_6,
    "brick_high_slope_inverted_left_2" to R.drawable.brick_high_slope_inverted_left_2,
    "brick_high_slope_inverted_left_4" to R.drawable.brick_high_slope_inverted_left_4,
    "brick_high_slope_inverted_right_2" to R.drawable.brick_high_slope_inverted_right_2,
    "brick_high_slope_inverted_right_4" to R.drawable.brick_high_slope_inverted_right_4,
    "brick_high_slope_left_2" to R.drawable.brick_high_slope_left_2,
    "brick_high_slope_left_4" to R.drawable.brick_high_slope_left_4,
    "brick_high_slope_right_2" to R.drawable.brick_high_slope_right_2,
    "brick_high_slope_right_4" to R.drawable.brick_high_slope_right_4,
    "brick_low_1" to R.drawable.brick_low_1,
    "brick_low_2" to R.drawable.brick_low_2,
    "brick_low_4" to R.drawable.brick_low_4,
    "brick_low_6" to R.drawable.brick_low_6,
    "brick_medium_1" to R.drawable.brick_medium_1,
    "brick_medium_2" to R.drawable.brick_medium_2,
    "brick_medium_4" to R.drawable.brick_medium_4,
    "brick_medium_6" to R.drawable.brick_medium_6,
    "brick_medium_slope_inverted_left_2" to R.drawable.brick_medium_slope_inverted_left_2,
    "brick_medium_slope_inverted_left_4" to R.drawable.brick_medium_slope_inverted_left_4,
    "brick_medium_slope_inverted_right_2" to R.drawable.brick_medium_slope_inverted_right_2,
    "brick_medium_slope_inverted_right_4" to R.drawable.brick_medium_slope_inverted_right_4,
    "brick_medium_slope_left_2" to R.drawable.brick_medium_slope_left_2,
    "brick_medium_slope_left_4" to R.drawable.brick_medium_slope_left_4,
    "brick_medium_slope_right_2" to R.drawable.brick_medium_slope_right_2,
    "brick_medium_slope_right_4" to R.drawable.brick_medium_slope_right_4,
    "extra_character_a" to R.drawable.extra_character_a,
    "extra_character_b" to R.drawable.extra_character_b,
    "extra_character_c" to R.drawable.extra_character_c,
    "extra_character_d" to R.drawable.extra_character_d,
    "extra_character_e" to R.drawable.extra_character_e,
    "green_brick_high_1" to R.drawable.green_brick_high_1,
    "green_brick_high_2" to R.drawable.green_brick_high_2,
    "green_brick_high_4" to R.drawable.green_brick_high_4,
    "green_brick_high_6" to R.drawable.green_brick_high_6,
    "green_brick_high_slope_inverted_left_2" to R.drawable.green_brick_high_slope_inverted_left_2,
    "green_brick_high_slope_inverted_left_4" to R.drawable.green_brick_high_slope_inverted_left_4,
    "green_brick_high_slope_inverted_right_2" to R.drawable.green_brick_high_slope_inverted_right_2,
    "green_brick_high_slope_inverted_right_4" to R.drawable.green_brick_high_slope_inverted_right_4,
    "green_brick_high_slope_left_2" to R.drawable.green_brick_high_slope_left_2,
    "green_brick_high_slope_left_4" to R.drawable.green_brick_high_slope_left_4,
    "green_brick_high_slope_right_2" to R.drawable.green_brick_high_slope_right_2,
    "green_brick_high_slope_right_4" to R.drawable.green_brick_high_slope_right_4,
    "green_brick_low_1" to R.drawable.green_brick_low_1,
    "green_brick_low_2" to R.drawable.green_brick_low_2,
    "green_brick_low_4" to R.drawable.green_brick_low_4,
    "green_brick_low_6" to R.drawable.green_brick_low_6,
    "green_brick_medium_1" to R.drawable.green_brick_medium_1,
    "green_brick_medium_2" to R.drawable.green_brick_medium_2,
    "green_brick_medium_4" to R.drawable.green_brick_medium_4,
    "green_brick_medium_6" to R.drawable.green_brick_medium_6,
    "green_brick_medium_slope_inverted_left_2" to R.drawable.green_brick_medium_slope_inverted_left_2,
    "green_brick_medium_slope_inverted_left_4" to R.drawable.green_brick_medium_slope_inverted_left_4,
    "green_brick_medium_slope_inverted_right_2" to R.drawable.green_brick_medium_slope_inverted_right_2,
    "green_brick_medium_slope_inverted_right_4" to R.drawable.green_brick_medium_slope_inverted_right_4,
    "green_brick_medium_slope_left_2" to R.drawable.green_brick_medium_slope_left_2,
    "green_brick_medium_slope_left_4" to R.drawable.green_brick_medium_slope_left_4,
    "green_brick_medium_slope_right_2" to R.drawable.green_brick_medium_slope_right_2,
    "green_brick_medium_slope_right_4" to R.drawable.green_brick_medium_slope_right_4,
    "red_brick_high_1" to R.drawable.red_brick_high_1,
    "red_brick_high_2" to R.drawable.red_brick_high_2,
    "red_brick_high_4" to R.drawable.red_brick_high_4,
    "red_brick_high_6" to R.drawable.red_brick_high_6,
    "red_brick_high_slope_inverted_left_2" to R.drawable.red_brick_high_slope_inverted_left_2,
    "red_brick_high_slope_inverted_left_4" to R.drawable.red_brick_high_slope_inverted_left_4,
    "red_brick_high_slope_inverted_right_2" to R.drawable.red_brick_high_slope_inverted_right_2,
    "red_brick_high_slope_inverted_right_4" to R.drawable.red_brick_high_slope_inverted_right_4,
    "red_brick_high_slope_left_2" to R.drawable.red_brick_high_slope_left_2,
    "red_brick_high_slope_left_4" to R.drawable.red_brick_high_slope_left_4,
    "red_brick_high_slope_right_2" to R.drawable.red_brick_high_slope_right_2,
    "red_brick_high_slope_right_4" to R.drawable.red_brick_high_slope_right_4,
    "red_brick_low_1" to R.drawable.red_brick_low_1,
    "red_brick_low_2" to R.drawable.red_brick_low_2,
    "red_brick_low_4" to R.drawable.red_brick_low_4,
    "red_brick_low_6" to R.drawable.red_brick_low_6,
    "red_brick_medium_1" to R.drawable.red_brick_medium_1,
    "red_brick_medium_2" to R.drawable.red_brick_medium_2,
    "red_brick_medium_4" to R.drawable.red_brick_medium_4,
    "red_brick_medium_6" to R.drawable.red_brick_medium_6,
    "red_brick_medium_slope_inverted_left_2" to R.drawable.red_brick_medium_slope_inverted_left_2,
    "red_brick_medium_slope_inverted_left_4" to R.drawable.red_brick_medium_slope_inverted_left_4,
    "red_brick_medium_slope_inverted_right_2" to R.drawable.red_brick_medium_slope_inverted_right_2,
    "red_brick_medium_slope_inverted_right_4" to R.drawable.red_brick_medium_slope_inverted_right_4,
    "red_brick_medium_slope_left_2" to R.drawable.red_brick_medium_slope_left_2,
    "red_brick_medium_slope_left_4" to R.drawable.red_brick_medium_slope_left_4,
    "red_brick_medium_slope_right_2" to R.drawable.red_brick_medium_slope_right_2,
    "red_brick_medium_slope_right_4" to R.drawable.red_brick_medium_slope_right_4,
    "yellow_brick_high_1" to R.drawable.yellow_brick_high_1,
    "yellow_brick_high_2" to R.drawable.yellow_brick_high_2,
    "yellow_brick_high_4" to R.drawable.yellow_brick_high_4,
    "yellow_brick_high_6" to R.drawable.yellow_brick_high_6,
    "yellow_brick_high_slope_inverted_left_2" to R.drawable.yellow_brick_high_slope_inverted_left_2,
    "yellow_brick_high_slope_inverted_left_4" to R.drawable.yellow_brick_high_slope_inverted_left_4,
    "yellow_brick_high_slope_inverted_right_2" to R.drawable.yellow_brick_high_slope_inverted_right_2,
    "yellow_brick_high_slope_inverted_right_4" to R.drawable.yellow_brick_high_slope_inverted_right_4,
    "yellow_brick_high_slope_left_2" to R.drawable.yellow_brick_high_slope_left_2,
    "yellow_brick_high_slope_left_4" to R.drawable.yellow_brick_high_slope_left_4,
    "yellow_brick_high_slope_right_2" to R.drawable.yellow_brick_high_slope_right_2,
    "yellow_brick_high_slope_right_4" to R.drawable.yellow_brick_high_slope_right_4,
    "yellow_brick_low_1" to R.drawable.yellow_brick_low_1,
    "yellow_brick_low_2" to R.drawable.yellow_brick_low_2,
    "yellow_brick_low_4" to R.drawable.yellow_brick_low_4,
    "yellow_brick_low_6" to R.drawable.yellow_brick_low_6,
    "yellow_brick_medium_1" to R.drawable.yellow_brick_medium_1,
    "yellow_brick_medium_2" to R.drawable.yellow_brick_medium_2,
    "yellow_brick_medium_4" to R.drawable.yellow_brick_medium_4,
    "yellow_brick_medium_6" to R.drawable.yellow_brick_medium_6,
    "yellow_brick_medium_slope_inverted_left_2" to R.drawable.yellow_brick_medium_slope_inverted_left_2,
    "yellow_brick_medium_slope_inverted_left_4" to R.drawable.yellow_brick_medium_slope_inverted_left_4,
    "yellow_brick_medium_slope_inverted_right_2" to R.drawable.yellow_brick_medium_slope_inverted_right_2,
    "yellow_brick_medium_slope_inverted_right_4" to R.drawable.yellow_brick_medium_slope_inverted_right_4,
    "yellow_brick_medium_slope_left_2" to R.drawable.yellow_brick_medium_slope_left_2,
    "yellow_brick_medium_slope_left_4" to R.drawable.yellow_brick_medium_slope_left_4,
    "yellow_brick_medium_slope_right_2" to R.drawable.yellow_brick_medium_slope_right_2,
    "yellow_brick_medium_slope_right_4" to R.drawable.yellow_brick_medium_slope_right_4,
)

typealias BrickAtlas = Map<String, ImageBitmap>

/**
 * Process-wide cache of decoded bricks, per asset. Per-caller caching re-decoded the
 * same PNGs for each slab and caused an ANR on the Us page. Bounded by the 145-file pack.
 */
private val decoded = ConcurrentHashMap<String, ImageBitmap>()

/** Decodes only the named bricks, once per process (all 145 up front would be about 14MB). */
@Composable
fun rememberBricks(names: Collection<String>): BrickAtlas {
    val context = LocalContext.current
    val wanted = names.toSortedSet()
    return remember(wanted) {
        wanted.mapNotNull { name ->
            val cached = decoded[name]
            if (cached != null) return@mapNotNull name to cached
            val id = BrickDrawables[name] ?: return@mapNotNull null
            val bitmap = BitmapFactory.decodeResource(context.resources, id)
                ?.asImageBitmap() ?: return@mapNotNull null
            decoded[name] = bitmap
            name to bitmap
        }.toMap()
    }
}
