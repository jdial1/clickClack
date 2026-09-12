package com.ampersand.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ampersand.ui.theme.Motion
import kotlin.math.floor
import kotlin.math.max

/**
 * Brick styling for UI surfaces.
 *
 * The sculpture uses the real pack art, but that art only comes in 1, 2, 4 and 6
 * stud widths — it cannot stretch to a text field or a nav bar without looking
 * mangled. So UI chrome is *drawn* in the same visual language instead: a body,
 * a row of studs along the top, and a darker lip along the bottom that shortens
 * when pressed, the way a key does.
 *
 * Studs sit inside the bounds rather than overhanging. Overhang is what real
 * bricks do, but [drawBehind] clips to bounds and a clipped stud looks like a
 * bug rather than a brick.
 */
fun Modifier.brickFace(
    color: Color,
    corner: Dp = 16.dp,
    studded: Boolean = true,
    /** 0 is standing proud, 1 is fully down. A float so it can be animated. */
    pressed: Float = 0f,
    lip: Dp = 6.dp,
    brush: Brush? = null,
): Modifier = drawBehind {
    val radius = CornerRadius(corner.toPx())
    val lipPx = androidx.compose.ui.util.lerp(lip.toPx(), lip.toPx() * 0.3f, pressed)
        .coerceAtMost(size.height / 3f)

    // The lip is the whole shape in a darker tone; the body sits on top of it,
    // short by the lip. One extra draw, and the surface reads as having depth.
    drawRoundRect(color = lerp(color, Color.Black, 0.26f), cornerRadius = radius)

    val bodySize = Size(size.width, size.height - lipPx)
    if (brush != null) {
        drawRoundRect(brush = brush, size = bodySize, cornerRadius = radius)
    } else {
        drawRoundRect(color = color, size = bodySize, cornerRadius = radius)
    }

    if (!studded) return@drawBehind

    val pitch = StudPitch.toPx()
    val diameter = StudSize.toPx()
    val margin = corner.toPx().coerceAtLeast(diameter)
    val usable = size.width - margin * 2
    if (usable < diameter) return@drawBehind

    val count = max(1, floor(usable / pitch).toInt())
    val spread = (count - 1) * pitch
    val startX = (size.width - spread) / 2f
    val top = StudInset.toPx()
    // Too tall a lip on a short control leaves no room; skip rather than crowd.
    if (top + diameter > bodySize.height * 0.7f) return@drawBehind

    val studColor = lerp(color, Color.White, 0.22f)
    repeat(count) { i ->
        drawRoundRect(
            color = studColor,
            topLeft = Offset(startX + i * pitch - diameter / 2f, top),
            size = Size(diameter, diameter * 0.62f),
            cornerRadius = CornerRadius(diameter * 0.31f),
        )
    }
}

/**
 * A brick going down under a finger, and springing back when it is let go.
 *
 * Travel only, in the draw phase -- deliberately not padding or height.
 * [com.ampersand.ui.brick.BrickSlab] is a custom Layout that rounds its height
 * up to whole courses and ignores an incoming height constraint, so anything
 * that changes layout around one fights it and shoves its neighbours about.
 * `graphicsLayer` moves pixels and nothing else, which is all a keypress is.
 *
 * One nub of travel. Deeper reads as squashy rather than solid, and a scale
 * change reads as a balloon: real keys go down, they do not shrink.
 */
fun Modifier.pressDepth(pressed: Boolean, depth: Dp = Motion.Depth): Modifier = composed {
    val down by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) tween(Motion.PressMs) else Motion.settle,
        label = "pressDepth",
    )
    graphicsLayer { translationY = down * depth.toPx() }
}

/**
 * Two quick cycles sideways, to say no.
 *
 * Keyed on a counter rather than a flag so a second refusal shakes again --
 * a boolean that is already true animates nothing, which is exactly the moment
 * someone taps harder. Starts at zero so it does not fire on first composition.
 */
fun Modifier.shake(key: Int, distance: Dp = 6.dp): Modifier = composed {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key == 0) return@LaunchedEffect
        val px = distance.value
        offset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = Motion.ShakeMs
                px at Motion.ShakeMs / 4
                -px at Motion.ShakeMs / 2
                px * 0.5f at Motion.ShakeMs * 3 / 4
            },
        )
    }
    graphicsLayer { translationX = offset.value * density }
}

/** Stud metrics for UI chrome. Looser than the real art, which is very fine at UI scale. */
val StudPitch: Dp = 30.dp
val StudSize: Dp = 15.dp
private val StudInset: Dp = 7.dp
