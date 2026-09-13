package com.ampersand.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ampersand.R
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.model.Mood
import com.ampersand.ui.brick.BrickTile
import com.ampersand.ui.brick.rememberBricks

/**
 * Draws a mood. Faces use character sprites in [faceColor] (null: the sprite's own
 * color; recolors come from tools/moods/faces.py). Built moods use plain bricks in [color].
 */
@Composable
fun MoodArt(
    mood: Mood,
    color: BrickColor,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    faceColor: BrickColor? = null,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (mood.kind) {
            Mood.Kind.FACE -> Sprite(face(mood, faceColor), size)
            Mood.Kind.SPECIAL -> Sprite(special(mood), size)
            Mood.Kind.BUILT -> Built(mood, color, size)
        }
    }
}

@Composable
private fun Built(mood: Mood, color: BrickColor, size: Dp) {
    val two = BrickShape(2, BrickHeight.MEDIUM)
    val one = BrickShape(1, BrickHeight.MEDIUM)
    val atlas = rememberBricks(listOf(color.asset(two), color.asset(one)))
    val stud = size / 3

    @Composable
    fun Tile(shape: BrickShape, m: Modifier = Modifier) = BrickTile(shape, color, stud, atlas, m)

    when (mood) {
        Mood.MISSING_A_PIECE -> {
            Tile(one, Modifier.offset(x = -stud))
            Tile(one, Modifier.offset(x = stud))
        }
        Mood.KNOCKED_OVER -> Tile(two, Modifier.rotate(90f))
        // Two studs, not one: a single upside-down stud is too small to read.
        Mood.STEPPED_ON_ONE -> Tile(two, Modifier.rotate(180f))
        else -> Unit
    }
}

@Composable
private fun Sprite(id: Int, size: Dp) {
    Image(painterResource(id), contentDescription = null, modifier = Modifier.size(size * 0.8f))
}

private fun special(mood: Mood): Int = when (mood) {
    Mood.SOMETHING_TO_TELL -> R.drawable.extra_box_exclamation
    Mood.RARE_PIECE -> R.drawable.extra_box_coin
    Mood.SOLID_GROUND -> R.drawable.extra_dirt_top
    Mood.STILL_IN_THE_BOX -> R.drawable.extra_crate
    Mood.HEAVY -> R.drawable.extra_stone_detail
    else -> R.drawable.extra_crate_explosive
}

/** Each face's own color first, then red, yellow, green, blue in [BrickColor] order. */
private val faces = mapOf(
    Mood.SNAPPED_IN to listOf(
        R.drawable.extra_character_a, R.drawable.extra_character_a_red, R.drawable.extra_character_a_yellow,
        R.drawable.extra_character_a_green, R.drawable.extra_character_a_blue,
    ),
    Mood.FLAT_PLATE to listOf(
        R.drawable.extra_character_b, R.drawable.extra_character_b_red, R.drawable.extra_character_b_yellow,
        R.drawable.extra_character_b_green, R.drawable.extra_character_b_blue,
    ),
    Mood.CLICKED to listOf(
        R.drawable.extra_character_c, R.drawable.extra_character_c_red, R.drawable.extra_character_c_yellow,
        R.drawable.extra_character_c_green, R.drawable.extra_character_c_blue,
    ),
    Mood.PLAYING_IT_COOL to listOf(
        R.drawable.extra_character_d, R.drawable.extra_character_d_red, R.drawable.extra_character_d_yellow,
        R.drawable.extra_character_d_green, R.drawable.extra_character_d_blue,
    ),
    Mood.NEW_SET to listOf(
        R.drawable.extra_character_e, R.drawable.extra_character_e_red, R.drawable.extra_character_e_yellow,
        R.drawable.extra_character_e_green, R.drawable.extra_character_e_blue,
    ),
)

private fun face(mood: Mood, color: BrickColor?): Int = faces.getValue(mood)[(color?.ordinal ?: -1) + 1]
