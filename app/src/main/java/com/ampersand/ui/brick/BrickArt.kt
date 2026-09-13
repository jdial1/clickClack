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
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import kotlin.math.roundToInt

/**
 * Brick art geometry from the pack's pixel sizes: stud 64px wide, shortest body 28px,
 * nub 16px above the body. Expressed as ratios of stud width.
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

    /** As [aspect], plus the top nub. Use when cropping tight to content, or top studs get clipped. */
    fun aspectWithNub(w: Int, rows: Int): Float = w / (rows * ROW_RATIO + NUB_RATIO)

    /** Brick widths in the pack, largest first. There are no 3- or 5-stud bricks. */
    val WIDTHS = listOf(6, 4, 2, 1)

    /** Asset name for [shape] in [slug]; null slug gives the unprefixed gray set (`brick_low_1`). */
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

/** Draws one brick from the pack art, untinted, with its bottom-left at ([left], [bottom]). */
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
    // The image includes the nub, so shift it up by the nub height.
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

/** A 1-stud brick used for depth, score pips and list bullets. [slug] null uses the gray brick. */
@Composable
fun BrickDot(
    slug: String?,
    modifier: Modifier = Modifier,
    studSize: Dp = 14.dp,
    filled: Boolean = true,
    /** Alpha for an unfilled marker. Raise it for unselected options that must stay visible. */
    dimAlpha: Float = 0.28f,
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
            alpha = if (filled) 1f else dimAlpha,
        )
    }
}
