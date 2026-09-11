package com.ampersand.ui.brick

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import kotlin.math.ceil

/** Readable text colour for a brick of [slug]. The pale bricks need dark ink. */
fun inkOnBrick(slug: String?): Color =
    if (slug == null || slug == "yellow") Color(0xFF15121F) else Color.White

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
 * Height comes from the **content**, not from a course count. The first version
 * took a `courses` parameter and clipped whatever did not fit; here the box
 * wraps its content and the canvas lays however many courses are needed to back
 * it, which is also what a wall does.
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

    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            val studPx = size.width / studs
            val coursePx = studPx * height.rows * BrickArt.ROW_RATIO
            val nubPx = studPx * BrickArt.NUB_RATIO
            if (coursePx <= 0f) return@Canvas
            // Subtract the nub: the topmost course's studs sit *above* its body,
            // and without this they are drawn outside the slab and overlap
            // whatever is laid out above it.
            val courses = ceil((size.height - nubPx) / coursePx).toInt().coerceAtLeast(1)
            var n = 0

            for (course in 0 until courses) {
                // Reverse alternate courses: [6,4,2] then [2,4,6]. Without this
                // every seam lines up and the slab reads as tiles.
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
        }

        Box(Modifier.fillMaxWidth(), content = content)
    }
}
