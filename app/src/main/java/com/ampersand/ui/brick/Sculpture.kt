package com.ampersand.ui.brick

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import com.ampersand.model.BuildPlate

/**
 * A finished sculpture at any size, cropped to [BuildPlate.builtHeight].
 * Drawn bottom row first so upper bricks cover lower nubs. No baseplate or grid.
 */
@Composable
fun Sculpture(
    plate: BuildPlate,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val rows = plate.builtHeight().coerceAtLeast(1)
    val atlas = rememberBricks(plate.placed.mapNotNull { plate.block(it.blockId)?.asset })

    // Height from width, not `aspectRatio`: in a LazyColumn that could overlap the caption below.
    BoxWithConstraints(modifier) {
        val height = maxWidth / BrickArt.aspectWithNub(plate.gridW, rows)

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clipToBounds()
                // Alpha on the layer, not per brick, so lower nubs do not show through.
                .graphicsLayer { this.alpha = alpha },
        ) {
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
                    )
                }
        }
    }
}
