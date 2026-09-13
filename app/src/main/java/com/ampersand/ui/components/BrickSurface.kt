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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ampersand.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * Moves the element down while pressed and springs back on release.
 * Uses graphicsLayer, not layout: [com.ampersand.ui.brick.BrickSlab] ignores
 * height constraints, so layout changes would shove its neighbors.
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
 * Horizontal shake, e.g. for a rejected action. Increment [key] to shake again;
 * 0 does nothing, so it won't fire on first composition.
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

/**
 * Fade-and-rise entrance, drawn in graphicsLayer so layout never reflows.
 * [index] delays by that many [Motion.StaggerMs]. Restarts when [key] changes.
 */
fun Modifier.arrive(
    key: Any?,
    index: Int = 0,
    rise: Dp = 8.dp,
    slide: Dp = 0.dp,
    enabled: Boolean = true,
    /** Called when the animation finishes. Not called if disposed mid-animation. */
    onLanded: () -> Unit = {},
): Modifier = composed {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(key, enabled) {
        if (!enabled) {
            // Disabled: show the final state immediately.
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        delay(index * Motion.StaggerMs.toLong())
        progress.animateTo(1f, Motion.glide)
        // Not in a finally, so cancellation skips the cue.
        onLanded()
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * rise.toPx()
        translationX = (1f - p) * slide.toPx()
    }
}
