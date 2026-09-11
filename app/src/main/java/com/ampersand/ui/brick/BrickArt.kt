package com.ampersand.ui.brick

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.size
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import kotlin.math.roundToInt

/**
 * Brick art geometry, from the pack's own pixel sizes.
 *
 * A stud is 64px across, the shortest brick body is 28px tall, and the nub on
 * top is 16px and overhangs the body. Everything below is expressed as ratios of
 * the stud width so bricks scale to any screen.
 */
object BrickArt {
    const val STUD_PX = 64f
    const val ROW_PX = 28f
    const val NUB_PX = 16f

    /** Cell height as a fraction of cell width. Bricks are wider than tall. */
    const val ROW_RATIO = ROW_PX / STUD_PX

    /** How far the nub overhangs the body, as a fraction of stud width. */
    const val NUB_RATIO = NUB_PX / STUD_PX

    /** Plate aspect ratio for a grid of [w] studs by [h] rows. */
    fun aspect(w: Int, h: Int): Float = (w * STUD_PX) / (h * ROW_PX)

    /** Full drawn height of a brick [rows] tall, including the nub. */
    fun heightFor(rows: Int, studPx: Float): Float = rows * studPx * ROW_RATIO + studPx * NUB_RATIO

    /**
     * The widths available in the pack, largest first.
     *
     * There is no 3- or 5-stud brick, so a run of that length is built the way
     * you would build it out of the box: a 2 and a 1.
     */
    val WIDTHS = listOf(6, 4, 2, 1)

    /**
     * Asset name for [shape] in [slug], or the neutral greyscale set when the
     * slug is null. The pack ships those unprefixed (`brick_low_1`), which is
     * why this is not just string concatenation at every call site.
     */
    fun assetFor(slug: String?, shape: BrickShape): String =
        if (slug.isNullOrEmpty()) shape.asset else "${slug}_${shape.asset}"

    /** Greedy decomposition of [studs] into real brick widths. */
    fun runOf(studs: Int): List<Int> {
        val out = mutableListOf<Int>()
        var left = studs
        while (left > 0) {
            val w = WIDTHS.first { it <= left }
            out += w
            left -= w
        }
        return out
    }
}

/**
 * Draws one brick from the pack art.
 *
 * The art is already the right colour, so nothing is tinted: earlier this
 * multiplied a greyscale source, which could not reach the pack's saturated reds
 * without clipping and left the bricks slightly off from the rest of the UI.
 */
fun DrawScope.drawBrickArt(
    atlas: BrickAtlas,
    asset: String,
    studs: Int,
    rows: Int,
    left: Float,
    bottom: Float,
    studPx: Float,
    alpha: Float = 1f,
) {
    val image = atlas[asset] ?: return
    val rowPx = studPx * BrickArt.ROW_RATIO
    val nubPx = studPx * BrickArt.NUB_RATIO
    val bodyH = rows * rowPx
    // The nub sits above the body, so the image is taller than the footprint and
    // is drawn shifted up by exactly that overhang.
    val top = bottom - bodyH - nubPx

    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize((studs * studPx).roundToInt(), (bodyH + nubPx).roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Medium,
    )
}

/** A single brick, sized to its own art. Use for chrome: nav tiles, markers. */
@Composable
fun BrickTile(
    shape: BrickShape,
    color: BrickColor,
    studSize: Dp,
    atlas: BrickAtlas,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val w = studSize * shape.w
    val h = studSize * (shape.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO)
    Canvas(modifier.size(w, h)) {
        drawBrickArt(
            atlas = atlas,
            asset = color.asset(shape),
            studs = shape.w,
            rows = shape.h,
            left = 0f,
            bottom = size.height,
            studPx = size.width / shape.w,
            alpha = alpha,
        )
    }
}

/** Convenience for the plate, which draws [com.ampersand.model.Block]s. */
fun DrawScope.drawBrick(
    block: com.ampersand.model.Block,
    atlas: BrickAtlas,
    left: Float,
    bottom: Float,
    studPx: Float,
    alpha: Float = 1f,
) = drawBrickArt(
    atlas = atlas,
    asset = block.asset,
    studs = block.shape.w,
    rows = block.shape.h,
    left = left,
    bottom = bottom,
    studPx = studPx,
    alpha = alpha,
)

private val DotShape = BrickShape(1, BrickHeight.LOW)

/**
 * The small marker used for depth, score pips and list bullets.
 *
 * A real 1-stud brick from the pack rather than a drawn circle. Top-down studs
 * were a shortcut: nothing else in the app is seen from above, so they read as a
 * different object at every size. [slug] null uses the neutral greyscale brick,
 * for markers sitting on a gradient where a palette colour would vanish.
 */
@Composable
fun BrickDot(
    slug: String?,
    modifier: Modifier = Modifier,
    studSize: Dp = 14.dp,
    filled: Boolean = true,
) {
    val asset = BrickArt.assetFor(slug, DotShape)
    val atlas = rememberBricks(listOf(asset))
    val h = studSize * (DotShape.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO)
    Canvas(modifier.size(studSize, h)) {
        drawBrickArt(
            atlas = atlas,
            asset = asset,
            studs = DotShape.w,
            rows = DotShape.h,
            left = 0f,
            bottom = size.height,
            studPx = size.width,
            alpha = if (filled) 1f else 0.28f,
        )
    }
}
