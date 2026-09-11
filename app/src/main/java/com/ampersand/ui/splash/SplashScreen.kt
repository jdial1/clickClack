package com.ampersand.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Tile = BrickShape(2, BrickHeight.MEDIUM)
private const val StudSize = 46

/**
 * Two bricks arriving from opposite sides and locking together.
 *
 * The launcher icon, animated, and the name acted out rather than typed: one
 * brick comes in and lands -- *click* -- and the second lands on it -- *clack*.
 * The sounds are fired by the animation rather than played as a fixed cue, so
 * each one hits on contact and not a frame either side of it.
 *
 * Each brick settles on a spring with a little bounce, because a brick dropped
 * onto another brick does not stop dead, and the overshoot is most of what
 * sells it as a physical object rather than a fading graphic.
 *
 * [onDone] fires once the pair has settled. It is read through
 * [rememberUpdatedState] so the sequence, which outlives a recomposition, calls
 * the current one rather than the one captured when it started.
 */
@Composable
fun SplashScreen(
    you: BrickColor,
    them: BrickColor,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    val sounds = LocalSounds.current
    val finish by rememberUpdatedState(onDone)

    val bottom = remember { Animatable(-1.6f) }
    val top = remember { Animatable(1.6f) }
    val fade = remember { Animatable(0f) }

    val lower = them.asset(Tile)
    val upper = you.asset(Tile)
    val atlas = rememberBricks(listOf(lower, upper))

    LaunchedEffect(Unit) {
        launch {
            bottom.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
        // The gap between the two arrivals *is* the gap between the two sounds.
        // Tuned together: much shorter and the pair fuses into one thud.
        delay(120)
        sounds.play(Sfx.CLICK)

        delay(130)
        launch {
            top.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
        delay(120)
        sounds.play(Sfx.CLACK)

        fade.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        delay(420)
        finish()
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val stud = StudSize.dp
                val course = stud * Tile.h * BrickArt.ROW_RATIO
                val nub = stud * BrickArt.NUB_RATIO

                Canvas(Modifier.size(stud * Tile.w * 2, course * 2 + nub)) {
                    val studPx = size.width / (Tile.w * 2f)
                    val coursePx = studPx * Tile.h * BrickArt.ROW_RATIO
                    val nubPx = studPx * BrickArt.NUB_RATIO
                    val span = studPx * Tile.w

                    // Lower brick, in from the left; upper brick, in from the
                    // right and offset a stud, so they land in running bond.
                    drawBrickArt(
                        atlas = atlas,
                        asset = lower,
                        studs = Tile.w,
                        rows = Tile.h,
                        left = bottom.value * size.width,
                        bottom = size.height,
                        studPx = studPx,
                    )
                    drawBrickArt(
                        atlas = atlas,
                        asset = upper,
                        studs = Tile.w,
                        rows = Tile.h,
                        left = span * 0.5f + top.value * size.width,
                        bottom = size.height - coursePx,
                        studPx = studPx,
                    )
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Click Clack",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = fade.value),
                )
            }
        }
    }
}

@Preview(name = "Splash", showBackground = true, heightDp = 640)
@Composable
private fun PreviewSplash() = AmpersandTheme {
    SplashScreen(you = BrickColor.RED, them = BrickColor.BLUE)
}
