package com.ampersand.ui.brick

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/** Readable text colour for a brick of [slug]. The pale bricks need dark ink. */
fun inkOnBrick(slug: String?): Color =
    if (slug == null || slug == "yellow") DarkInk else Color.White

/**
 * Ink for a surface built from **two** colours, where one line of text has to
 * cross both.
 *
 * Dark unless both halves want white. White on pack yellow is about 1.8:1,
 * which fails even the large-text bar, so a single yellow brick anywhere in the
 * surface decides it for the whole thing. Dark ink is the safer default anyway:
 * on pack red it measures about 5.5:1 against white's 3.2:1.
 */
fun inkOnBricks(a: String?, b: String?): Color =
    if (inkOnBrick(a) == Color.White && inkOnBrick(b) == Color.White) Color.White else DarkInk

/**
 * The brick that reads as *quiet* against the current ground.
 *
 * The pack's neutral brick is `#DBDBDB`, so a marker drawn in it disappears
 * against a pale background and reads perfectly on a dark one. Black is the
 * exact opposite. Neither is "the neutral brick" in any absolute sense, and
 * hard-coding either is why depth pips, score pips and list bullets all
 * vanished in light mode.
 */
@Composable
fun quietSlug(): String? =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) null else "black"

/**
 * The brick that reads as *structure* against the current ground: the inverse.
 *
 * Used for the nav bar joiners, which have to separate the tabs rather than
 * recede into the wall behind them.
 */
@Composable
fun structureSlug(): String? =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) "black" else null

/**
 * A frosted panel to sit text on, when the text is on brick.
 *
 * Brick art is not a flat colour: every course has a seam and a shadow, and a
 * line of type that crosses one gets a stripe through it. On the stat cards it
 * read as a strikethrough.
 *
 * This is a scrim rather than a true backdrop blur. Compose's `Modifier.blur`
 * blurs the composable it is applied to, not what is behind it, and there is no
 * backdrop filter short of a `RuntimeShader` on API 33+. What a scrim does get
 * is the part that matters: it flattens the seams under the text and lifts
 * contrast, with a hairline top edge so it still reads as a pane laid on the
 * brick rather than a painted rectangle.
 *
 * Tinted from the ink it is carrying -- dark ink wants a light pane, white ink
 * a dark one -- so it works on any colour brick in either theme.
 */
@Composable
fun Modifier.glass(
    ink: Color,
    corner: Dp = 16.dp,
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

/** As [glass], for a surface whose ink is decided by its brick colour. */
@Composable
fun Modifier.glassOn(slug: String?, corner: Dp = 16.dp): Modifier =
    glass(inkOnBrick(slug), corner)

/**
 * A wide surface built out of real bricks.
 *
 * The pack only ships 1, 2, 4 and 6 stud widths, so a full-width button or card
 * cannot be a single image. The earlier answer was `brickFace`, which *drew* a
 * rounded panel with stud-shaped dots along the top — and that is exactly what
 * still read as "clean modern card" rather than brick.
 *
 * This lays real courses instead: [BrickArt.runOf] decomposes the width into
 * stock bricks (12 studs becomes a 6, a 4 and a 2), and alternate courses lay
 * that run in reverse so seams stagger like real bond rather than stacking into
 * a visible column.
 *
 * Height comes from the **content**, rounded up to a whole number of courses.
 * That rounding is why this is a custom [Layout] rather than a Box: a plain
 * content-sized box gives a short slab a course count that does not divide its
 * height, and the top course is then drawn *above* the slab, over whatever is
 * laid out there. On the sign-in screen that was the button covering the line
 * of copy above it.
 *
 * [slugs] cycles, so passing two colours gives a surface built from both of you.
 */
@Composable
fun BrickSlab(
    slugs: List<String?>,
    modifier: Modifier = Modifier,
    studs: Int = 12,
    height: BrickHeight = BrickHeight.MEDIUM,
    alpha: Float = 1f,
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

                // Exact, because the measure pass made the height a whole
                // number of courses. Rounding rather than ceiling so a float
                // hair over the boundary does not add a course that overflows.
                val courses = ((size.height - nubPx) / coursePx).roundToInt().coerceAtLeast(1)
                var n = 0

                for (course in 0 until courses) {
                    // Reverse alternate courses: [6,4,2] then [2,4,6]. Without
                    // this every seam lines up and the slab reads as tiles.
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
                            alpha = alpha,
                        )
                        x += w * studPx
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

        // Round the height UP to a whole number of courses, plus the nub the
        // top course sticks above its body.
        //
        // This is the whole point of the custom layout. With a content-sized
        // box, a short slab -- a button, say -- gets a course count that does
        // not divide its height, and the top course is simply drawn above the
        // slab, over whatever is laid out there. On the sign-in screen that was
        // the button eating the line of copy above it.
        val needed = max(placeable.height.toFloat(), coursePx + nubPx)
        val courses = ceil((needed - nubPx) / coursePx).toInt().coerceAtLeast(1)
        val slabHeight = (courses * coursePx + nubPx).roundToInt()

        layout(width, slabHeight) {
            // Centred in the slack the rounding created. Placed at the top, a
            // button whose label needs a course and a half sits against the
            // nubs with an empty course under it.
            placeable.place(0, ((slabHeight - placeable.height) / 2).coerceAtLeast(0))
        }
    }
}

/**
 * [BrickSlab]'s content-width sibling: a real brick that hugs what is on it.
 *
 * The slab takes the width it is given and divides it into studs. A chip cannot
 * do that -- "Reflect" and "This or that" are not the same width and neither is
 * a whole number of anything -- so this works the other way round: the stud is
 * a fixed size, the content is measured, and the brick is however many studs it
 * takes to cover it. Labels therefore snap to 1, 2, 4 and 6 stud widths, which
 * is not a compromise: it is what a box of bricks actually contains.
 *
 * The stud count is recovered in the draw pass by dividing the final width by
 * the stud, rather than hoisted into state. The measure pass set that width to
 * exactly `studs * studPx`, so the division is exact and there is no frame
 * where the art and the layout disagree.
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
    // Every stock width, because which ones are needed is not known until the
    // content has been measured.
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
                        alpha = alpha,
                    )
                    x += w * studPx
                }
            }
        },
        // The nub is reserved as padding on the content rather than fixed up
        // afterwards in the placement, so it is part of what gets measured. Done
        // the other way round, a label taller than one course centres through
        // the nub and sits on the studs.
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
