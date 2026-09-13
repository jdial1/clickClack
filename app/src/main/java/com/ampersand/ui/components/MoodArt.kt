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
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.BrickTile
import com.ampersand.ui.brick.rememberBricks

/**
 * A mood as a small arrangement of real bricks, in the person's own colour.
 *
 * Each is the picture the name describes: two bricks pressed together, one on
 * its side, one upside down with its studs in the air, a gap where a piece
 * should be. Three use the pack's crates, which already look like a box you
 * have not opened and a box you should not drop.
 */
@Composable
fun MoodArt(mood: Mood, color: BrickColor, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val two = BrickShape(2, BrickHeight.MEDIUM)
    val one = BrickShape(1, BrickHeight.MEDIUM)
    val atlas = rememberBricks(listOf(color.asset(two), color.asset(one)))
    val stud = size / 3
    val course = stud * (2 * BrickArt.ROW_RATIO)

    @Composable
    fun Tile(shape: BrickShape, m: Modifier = Modifier) = BrickTile(shape, color, stud, atlas, m)

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (mood) {
            Mood.SNAPPED_IN -> {
                Tile(two, Modifier.offset(y = course / 2))
                Tile(two, Modifier.offset(y = -course / 2))
            }
            Mood.BUILDING -> {
                Tile(one, Modifier.offset(x = -stud, y = course))
                Tile(one, Modifier.offset(x = 0.dp, y = 0.dp))
                Tile(one, Modifier.offset(x = stud, y = -course))
            }
            Mood.NEW_SET -> Crate(R.drawable.extra_box_exclamation, size)
            Mood.STILL_IN_THE_BOX -> Crate(R.drawable.extra_crate, size)
            Mood.HANDLE_WITH_CARE -> Crate(R.drawable.extra_crate_explosive, size)
            Mood.SORTING -> {
                Tile(one, Modifier.offset(x = -stud, y = -course / 2).rotate(-18f))
                Tile(one, Modifier.offset(x = stud * 0.8f, y = -course * 0.8f).rotate(22f))
                Tile(one, Modifier.offset(x = -stud * 0.4f, y = course * 0.9f).rotate(8f))
                Tile(one, Modifier.offset(x = stud, y = course * 0.6f).rotate(-30f))
            }
            Mood.LOOSE -> Tile(two, Modifier.rotate(14f))
            Mood.MISSING_A_PIECE -> {
                Tile(one, Modifier.offset(x = -stud))
                Tile(one, Modifier.offset(x = stud))
            }
            Mood.KNOCKED_OVER -> Tile(two, Modifier.rotate(90f))
            Mood.STEPPED_ON_ONE -> Tile(one, Modifier.rotate(180f))
        }
    }
}

@Composable
private fun Crate(id: Int, size: Dp) {
    Image(painterResource(id), contentDescription = null, modifier = Modifier.size(size * 0.8f))
}
