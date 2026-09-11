package com.ampersand.ui.brick

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ampersand.model.BuildPlate

/**
 * A finished sculpture, drawn at any size.
 *
 * Cropped to [BuildPlate.builtHeight] rather than to the grid, because a plate
 * is fourteen rows tall and a week's work is rarely more than six -- drawn to
 * the full grid, every sculpture on the shelf would be two thirds empty sky and
 * they would all look identical.
 *
 * One Canvas, bottom row first so each nub is overlapped by whatever sits on
 * it. Deliberately has no baseplate, no stud grid and no landing hint: those
 * belong to the plate you are still building, and their absence is most of what
 * makes a shelved piece read as finished.
 */
@Composable
fun Sculpture(
    plate: BuildPlate,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val rows = plate.builtHeight().coerceAtLeast(1)
    val atlas = rememberBricks(plate.placed.mapNotNull { plate.block(it.blockId)?.asset })

    Canvas(modifier.aspectRatio(BrickArt.aspectWithNub(plate.gridW, rows))) {
        val stud = size.width / plate.gridW
        val rowPx = stud * BrickArt.ROW_RATIO

        plate.placed
            .sortedBy { it.y }
            .forEach { p ->
                val block = plate.block(p.blockId) ?: return@forEach
                drawBrick(
                    block = block,
                    atlas = atlas,
                    left = p.x * stud,
                    bottom = size.height - p.y * rowPx,
                    studPx = stud,
                    alpha = alpha,
                )
            }
    }
}
