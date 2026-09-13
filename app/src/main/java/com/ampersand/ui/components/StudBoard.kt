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
 * Faint background wall of six-stud bricks in both partners' colors, with random
 * gaps. Layout uses a fixed seed so it doesn't reshuffle on recomposition.
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

        // Alpha on the whole layer, not per brick, or lower nubs show through.
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
 * Running bond: odd rows offset by half a brick, about 12% of slots left empty.
 * Bricks may overhang either edge.
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
