package com.ampersand.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.ampersand.ui.components.BrickWall
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import kotlinx.coroutines.delay

private val Tile = BrickShape(2, BrickHeight.MEDIUM)
private const val StudSize = 46

/** Accelerating, because things fall faster the longer they have been falling. */
private val Gravity = CubicBezierEasing(0.5f, 0f, 1f, 0.5f)

/** How far above its resting place a brick starts, as a fraction of the drop. */
private const val DropFrom = -5f

/**
 * Two bricks dropped from above, landing one on the other.
 *
 * The name, acted out: the first brick hits the ground -- *click* -- and the
 * second lands on top of it -- *clack*.
 *
 * The fall is a tween on an accelerating curve rather than a spring, because a
 * spring is symmetrical and a falling object is not: it should arrive fast and
 * stop dead. Stopping dead is also what lets the sound be exact. Each clip is
 * fired the instant its `animateTo` returns, which is the frame of contact, so
 * the audio cannot drift out of step with the picture the way a fixed cue does.
 *
 * The small recoil afterwards is the only springy part. A brick landing on a
 * brick does bounce a little, and without it the whole thing reads as a graphic
 * sliding into place rather than an object arriving.
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

    // 0 is resting; negative is above it.
    val lowerY = remember { Animatable(DropFrom) }
    val upperY = remember { Animatable(DropFrom) }
    val fade = remember { Animatable(0f) }

    val lower = them.asset(Tile)
    val upper = you.asset(Tile)
    val atlas = rememberBricks(listOf(lower, upper))

    LaunchedEffect(Unit) {
        delay(180)

        lowerY.animateTo(0f, tween(420, easing = Gravity))
        sounds.play(Sfx.CLICK)
        lowerY.animateTo(-0.10f, tween(80))
        lowerY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh))

        upperY.animateTo(0f, tween(380, easing = Gravity))
        sounds.play(Sfx.CLACK)
        upperY.animateTo(-0.09f, tween(75))
        upperY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh))

        fade.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        delay(380)
        finish()
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier.fillMaxSize()) {
            BrickWall(you = you, them = them, alpha = 0.10f)

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val stud = StudSize.dp
                    val course = stud * Tile.h * BrickArt.ROW_RATIO
                    val nub = stud * BrickArt.NUB_RATIO

                    Canvas(Modifier.size(stud * Tile.w * 2, course * 2 + nub)) {
                        val studPx = size.width / (Tile.w * 2f)
                        val coursePx = studPx * Tile.h * BrickArt.ROW_RATIO
                        val span = studPx * Tile.w

                        // Offsets are in courses, so a brick starts five of its
                        // own heights up and the two falls read as the same
                        // speed regardless of screen size.
                        drawBrickArt(
                            atlas = atlas,
                            asset = lower,
                            studs = Tile.w,
                            rows = Tile.h,
                            left = span * 0.25f,
                            bottom = size.height + lowerY.value * coursePx,
                            studPx = studPx,
                        )
                        drawBrickArt(
                            atlas = atlas,
                            asset = upper,
                            studs = Tile.w,
                            rows = Tile.h,
                            // Offset by a stud, so they land in running bond.
                            left = span * 0.75f,
                            bottom = size.height - coursePx + upperY.value * coursePx,
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
}

@Preview(name = "Splash", showBackground = true, heightDp = 640)
@Composable
private fun PreviewSplash() = AmpersandTheme {
    SplashScreen(you = BrickColor.RED, them = BrickColor.BLUE)
}
