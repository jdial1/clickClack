package com.ampersand.ui.brick

import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private val DarkInk = Color(0xFF15121F)

/** Readable text color for a brick of [slug]. Pale bricks need dark ink. */
fun inkOnBrick(slug: String?): Color =
    if (slug == null || slug == "yellow") DarkInk else Color.White

/** Ink for text crossing two brick colors: white only if both want white (white on yellow fails contrast). */
fun inkOnBricks(a: String?, b: String?): Color =
    if (inkOnBrick(a) == Color.White && inkOnBrick(b) == Color.White) Color.White else DarkInk

/** A low-emphasis brick slug for the current background: gray on dark, black on light. */
@Composable
fun quietSlug(): String? =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) null else "black"

/** The inverse of [quietSlug], for elements that must stand out (nav bar joiners). */
@Composable
fun structureSlug(): String? =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) "black" else null

/** A scrim behind text on the brick wall background, so seams do not cut through the text. */
@Composable
fun Modifier.onWall(corner: Dp = com.ampersand.ui.theme.Corner.medium, strength: Float = 1f): Modifier {
    // Use background luminance; the themed ink is not a fixed constant to compare against.
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pane = if (light) Color.White else Color.Black
    val shape = RoundedCornerShape(corner)
    return this
        .clip(shape)
        .background(pane.copy(alpha = (if (light) 0.58f else 0.34f) * strength))
        .padding(horizontal = 10.dp, vertical = 6.dp)
}

/**
 * A translucent panel behind text on brick, tinted opposite to [ink], with a hairline border.
 * A scrim, not a real blur: Compose has no backdrop blur below API 33.
 */
@Composable
fun Modifier.glass(
    ink: Color,
    corner: Dp = com.ampersand.ui.theme.Corner.medium,
    strength: Float = 1f,
): Modifier {
    val dark = ink == DarkInk
    val pane = if (dark) Color.White else Color.Black
    val shape = RoundedCornerShape(corner)
    return this
        .clip(shape)
        .background(pane.copy(alpha = (if (dark) 0.34f else 0.26f) * strength))
        .border(1.dp, (if (dark) Color.White else Color.White).copy(alpha = 0.18f * strength), shape)
}

/** As [glass], using the ink for [slug]. */
@Composable
fun Modifier.glassOn(slug: String?, corner: Dp = com.ampersand.ui.theme.Corner.medium): Modifier =
    glass(inkOnBrick(slug), corner)

/**
 * A full-width surface built from courses of real bricks ([BrickArt.runOf]), with
 * alternate courses reversed so seams stagger.
 *
 * Height is the content height rounded up to whole courses. A custom [Layout] so
 * the top course never draws outside the slab. [slugs] cycles per brick.
 */
@Composable
fun BrickSlab(
    slugs: List<String?>,
    modifier: Modifier = Modifier,
    studs: Int = 12,
    height: BrickHeight = BrickHeight.MEDIUM,
    alpha: Float = 1f,
    /**
     * Stay one size on any phone. Bricks scale with the screen but text does not, so a
     * button's content is taller against a brick on a narrow phone. Snug lets empty
     * padding overhang rather than adding a course. For content that is mostly padding.
     */
    snug: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val run = remember(studs) { BrickArt.runOf(studs) }
    val assets = remember(run, slugs, height) {
        slugs.flatMap { slug ->
            run.distinct().map { w -> BrickArt.assetFor(slug, BrickShape(w, height)) }
        }
    }
    val atlas = rememberBricks(assets)

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val studPx = size.width / studs
                val coursePx = studPx * height.rows * BrickArt.ROW_RATIO
                val nubPx = studPx * BrickArt.NUB_RATIO
                if (coursePx <= 0f) return@drawBehind

                // Height is whole courses; round (not ceil) to absorb float error.
                val courses = ((size.height - nubPx) / coursePx).roundToInt().coerceAtLeast(1)
                var n = 0

                translucent(alpha) {
                for (course in 0 until courses) {
                    // Reverse alternate courses so seams stagger.
                    val widths = if (course % 2 == 0) run else run.reversed()
                    var x = 0f
                    widths.forEach { w ->
                        drawBrickArt(
                            atlas = atlas,
                            asset = BrickArt.assetFor(
                                slugs[n++ % slugs.size],
                                BrickShape(w, height),
                            ),
                            studs = w,
                            rows = height.rows,
                            left = x,
                            // Course 0 is the bottom one.
                            bottom = size.height - course * coursePx,
                            studPx = studPx,
                        )
                        x += w * studPx
                    }
                }
                }
            },
        content = { Box(Modifier.fillMaxWidth(), content = content) },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val studPx = width / studs.toFloat()
        val coursePx = studPx * height.rows * BrickArt.ROW_RATIO
        val nubPx = studPx * BrickArt.NUB_RATIO

        val placeable = measurables.first().measure(constraints.copy(minHeight = 0))

        // Round up to whole courses plus the top nub, so nothing draws above the slab.
        val needed = max(placeable.height.toFloat(), coursePx + nubPx)
        val courses = ceil((needed - nubPx) / coursePx - if (snug) 0.5f else 0f).toInt().coerceAtLeast(1)
        val slabHeight = (courses * coursePx + nubPx).roundToInt()

        layout(width, slabHeight) {
            // Center content vertically in the rounded height.
            val top = (slabHeight - placeable.height) / 2
            placeable.place(0, if (snug) top else top.coerceAtLeast(0))
        }
    }
}

/**
 * A content-sized brick chip: fixed [studSize], width rounded up to whole studs.
 * The draw pass recovers the stud count from the measured width (exactly `studs * studPx`).
 */
@Composable
fun BrickPill(
    slug: String?,
    modifier: Modifier = Modifier,
    studSize: Dp = 26.dp,
    height: BrickHeight = BrickHeight.LOW,
    alpha: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val studPx = with(LocalDensity.current) { studSize.toPx() }
    // Load every width, since the needed ones are only known after measuring.
    val atlas = rememberBricks(
        remember(slug, height) {
            BrickArt.WIDTHS.map { BrickArt.assetFor(slug, BrickShape(it, height)) }
        },
    )

    Layout(
        modifier = modifier.drawBehind {
            val studs = (size.width / studPx).roundToInt().coerceAtLeast(1)
            val coursePx = studPx * height.rows * BrickArt.ROW_RATIO
            val nubPx = studPx * BrickArt.NUB_RATIO
            if (coursePx <= 0f) return@drawBehind

            val courses = ((size.height - nubPx) / coursePx).roundToInt().coerceAtLeast(1)
            val run = BrickArt.runOf(studs)
            translucent(alpha) {
            for (course in 0 until courses) {
                val widths = if (course % 2 == 0) run else run.reversed()
                var x = 0f
                widths.forEach { w ->
                    drawBrickArt(
                        atlas = atlas,
                        asset = BrickArt.assetFor(slug, BrickShape(w, height)),
                        studs = w,
                        rows = height.rows,
                        left = x,
                        bottom = size.height - course * coursePx,
                        studPx = studPx,
                    )
                    x += w * studPx
                }
            }
            }
        },
        // Reserve the nub as top padding so content is measured and centered below it.
        content = {
            Box(Modifier.padding(top = studSize * BrickArt.NUB_RATIO), content = content)
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(minWidth = 0, minHeight = 0),
        )
        val coursePx = studPx * height.rows * BrickArt.ROW_RATIO
        val nubPx = studPx * BrickArt.NUB_RATIO

        val studs = ceil(placeable.width / studPx).toInt().coerceAtLeast(1)
        val width = (studs * studPx).roundToInt()
        val needed: Float = max(placeable.height - nubPx, coursePx)
        val courses = ceil(needed / coursePx).toInt().coerceAtLeast(1)
        val pillHeight = (courses * coursePx + nubPx).roundToInt()

        layout(width, pillHeight) {
            placeable.place(
                (width - placeable.width) / 2,
                ((pillHeight - placeable.height) / 2).coerceAtLeast(0),
            )
        }
    }
}

/**
 * Draws [block] on one offscreen layer faded as a whole, so lower nubs do not show
 * through. No layer when [alpha] is 1.
 */
private inline fun DrawScope.translucent(alpha: Float, block: DrawScope.() -> Unit) {
    if (alpha >= 1f) return block()
    drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), Paint().apply { this.alpha = alpha }) }
    block()
    drawIntoCanvas { it.restore() }
}
