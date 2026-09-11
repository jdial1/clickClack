package com.ampersand.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import kotlin.math.ceil
import kotlin.random.Random

/** Bricks per screen width. Six-stud bricks give two per row, half the width each. */
private const val Cols = 12
private val WallShape = BrickShape(6, BrickHeight.MEDIUM)

/** Fixed seed: the wall must not reshuffle on every recomposition. */
private const val WallSeed = 20260911

private data class WallBrick(val startStud: Int, val row: Int, val color: BrickColor)

/**
 * The app is built on a half-finished wall of the two of you.
 *
 * Six-stud bricks stacked in running bond, alternating between your colour and
 * theirs, with random gaps where a brick has not been laid yet. It is the same
 * idea as the plate on the Build tab, running quietly behind everything: a thing
 * the two of you are still making.
 *
 * Drawn from the real pack art rather than procedurally, so the background is
 * made of exactly the same bricks as the sculpture. Layout is generated once
 * against a fixed seed -- a wall that rearranged itself on every recomposition
 * would be a strobe light.
 */
@Composable
fun BrickWall(
    you: BrickColor,
    them: BrickColor,
    modifier: Modifier = Modifier,
    alpha: Float = 0.12f,
) {
    val atlas = rememberBricks(listOf(you.asset(WallShape), them.asset(WallShape)))

    BoxWithConstraints(modifier.fillMaxSize()) {
        val stud = maxWidth / Cols
        val rowH = stud * WallShape.h * BrickArt.ROW_RATIO
        val rows = ceil(maxHeight / rowH).toInt() + 1

        val bricks = remember(rows, you, them) { layout(rows, you, them) }

        // Alpha on the whole layer, not per brick. Drawing each brick
        // translucent let the nub of a lower brick show *through* the brick
        // stacked on it -- studs appearing in the middle of a solid wall.
        // One offscreen layer means they occlude properly first, then fade.
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha },
        ) {
            val studPx = size.width / Cols
            val rowPx = studPx * WallShape.h * BrickArt.ROW_RATIO

            bricks.forEach { brick ->
                drawBrickArt(
                    atlas = atlas,
                    asset = brick.color.asset(WallShape),
                    studs = WallShape.w,
                    rows = WallShape.h,
                    left = brick.startStud * studPx,
                    // Row 0 sits at the bottom, so the wall builds upward.
                    bottom = size.height - brick.row * rowPx,
                    studPx = studPx,
                )
            }
        }
    }
}

/**
 * Running bond with holes: every other course is offset by half a brick, and
 * a small fraction of slots are left empty -- just enough to read as unfinished
 * rather than tiled. Alpha is tuned for legibility
 * rather than realism: at full strength the wall wins every contrast fight with
 * the body text sitting on it.
 *
 * Bricks are allowed to start off the left edge and run off the right one. That
 * is what stops the pattern reading as a tidy grid of tiles.
 */
private fun layout(rows: Int, you: BrickColor, them: BrickColor): List<WallBrick> {
    val random = Random(WallSeed)
    val out = mutableListOf<WallBrick>()

    for (row in 0 until rows) {
        val offset = if (row % 2 == 0) 0 else -WallShape.w / 2
        var start = offset
        while (start < Cols) {
            if (random.nextFloat() > 0.12f) {
                out += WallBrick(
                    startStud = start,
                    row = row,
                    color = if (random.nextBoolean()) you else them,
                )
            }
            start += WallShape.w
        }
    }
    return out
}
