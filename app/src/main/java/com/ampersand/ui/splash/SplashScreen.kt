package com.ampersand.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.BrickAtlas
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.components.BrickWall
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.awaitReady
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Wordmark
import kotlinx.coroutines.delay
import kotlin.random.Random

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
    val feel = LocalFeel.current
    val finish by rememberUpdatedState(onDone)

    // 0 is resting; negative is above it.
    val lowerY = remember { Animatable(DropFrom) }
    val upperY = remember { Animatable(DropFrom) }
    val fade = remember { Animatable(0f) }

    val lower = them.asset(Tile)
    val upper = you.asset(Tile)
    val atlas = rememberBricks(listOf(lower, upper))

    LaunchedEffect(Unit) {
        // Hold the curtain until the clips are decoded. SoundPool drops a play
        // that arrives before its sample is ready, and on a cold start it is
        // still working through a dozen of them when the first brick would
        // land -- which is why the launch was silent.
        sounds.awaitReady(Sfx.CLICK)
        sounds.awaitReady(Sfx.CLACK)
        delay(120)

        lowerY.animateTo(0f, tween(420, easing = Gravity))
        sounds.play(Sfx.CLICK)
        feel.play(Feel.PLACE)
        lowerY.animateTo(-0.10f, tween(80))
        lowerY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh))

        upperY.animateTo(0f, tween(380, easing = Gravity))
        sounds.play(Sfx.CLACK)
        feel.play(Feel.PLACE)
        upperY.animateTo(-0.09f, tween(75))
        upperY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh))

        fade.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        // Longer than it needs to be for the wordmark, and on purpose: the
        // wall behind is still going up, and cutting away mid-course is the
        // one thing that would make it look like a loading bar.
        delay(900)
        finish()
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier.fillMaxSize()) {
            BuildingWall(you = you, them = them)

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
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = Wordmark,
                            // The face is already heavy; letting the letters
                            // touch is what makes it read as one moulded object
                            // rather than as a word.
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = fade.value),
                    )
                }
            }
        }
    }
}


/** One piece of the wall: which colour, and which cell it ended up in. */
private data class Piece(val slug: String?, val col: Int, val row: Int)

/** Grid of the wall, in 4-stud pieces across a 12-stud screen. */
private const val WallCols = 3
private const val WallRows = 32

/** Courses already down when the splash opens. */
private const val WallBase = 7
private val WallPiece = BrickShape(4, BrickHeight.MEDIUM)

/**
 * The wall behind the splash, being built while you watch.
 *
 * Everywhere else in the app the wall is a still background generated once from
 * a fixed seed -- it is the ground, and ground does not move. Here it is the
 * only screen with nothing to do, so it does the thing the whole app is about:
 * a piece arrives, hovers while it decides, and settles into place. By the time
 * the wordmark has faded up there is a wall under it that was not there when
 * you opened the app.
 *
 * Bottom row first. A wall built top-down reads as tetris; built from the floor
 * up it reads as a wall.
 *
 * The ghost is the same idea as the landing hint on the build plate: a piece
 * that is not committed yet, drawn brighter than the placed ones so the eye
 * follows it. It wanders before it lands because a piece that flew straight to
 * its slot would look placed rather than considered -- and because two people
 * deciding where something goes is the app.
 */
@Composable
private fun BuildingWall(you: BrickColor, them: BrickColor, modifier: Modifier = Modifier) {
    val slugs = remember(you, them) { listOf(you.slug, them.slug, null) }
    val atlas = rememberBricks(slugs.map { BrickArt.assetFor(it, WallPiece) })

    val placed = remember { mutableStateListOf<Piece>() }
    val ghost = remember { Animatable(Offset(1f, 0f), Offset.VectorConverter) }
    var ghostSlug by remember { mutableStateOf<String?>(null) }
    var ghostShowing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val rng = Random(11)
        // Shuffled within each row, so it fills unevenly rather than sweeping
        // left to right like a progress bar.
        val cells = (0 until WallRows).flatMap { row ->
            (0 until WallCols).map { col -> col to row }.shuffled(rng)
        }

        // The bottom courses are already down when you arrive. A splash that
        // starts on bare ground and places six pieces reads as an empty screen
        // with something loading on it; starting part-built reads as a wall
        // that is going up, which is the thing worth showing.
        val seeded = WallBase * WallCols
        cells.take(seeded).forEach { (col, row) ->
            placed += Piece(slugs[rng.nextInt(slugs.size)], col, row)
        }

        ghostShowing = true
        cells.drop(seeded).forEach { (col, row) ->
            ghostSlug = slugs[rng.nextInt(slugs.size)]
            // One hesitation, not three. Any longer and the splash becomes a
            // screensaver with a logo on it.
            ghost.animateTo(
                targetValue = Offset(
                    rng.nextFloat() * (WallCols - 1),
                    (row + rng.nextFloat() * 2f).coerceAtMost(WallRows - 1f),
                ),
                animationSpec = tween(70, easing = FastOutSlowInEasing),
            )
            // A tween rather than a spring for the landing: a spring settles
            // over a couple of hundred milliseconds, and at three pieces a
            // second that is the difference between a wall going up and five
            // bricks arriving before the splash ends.
            ghost.animateTo(
                targetValue = Offset(col.toFloat(), row.toFloat()),
                animationSpec = tween(80, easing = FastOutSlowInEasing),
            )
            placed += Piece(ghostSlug, col, row)
        }
        ghostShowing = false
    }

    // Alpha on the layer, not on each brick -- the same trap BrickWall
    // documents. Drawn translucent one at a time, the nub of a lower course
    // shows *through* the course stacked on it and the wall grows studs in its
    // middle. One offscreen layer lets them occlude first, then fades the lot.
    //
    // Two layers rather than one, because the ghost is a different opacity from
    // the wall it is about to join.
    Box(modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.13f },
        ) {
            // Read inside the draw lambda, so a piece landing invalidates the
            // draw and nothing else -- this runs under a screen that is already
            // animating three other things.
            placed.forEach { p ->
                wallPiece(atlas, p.slug, p.col.toFloat(), p.row.toFloat())
            }
        }

        if (ghostShowing) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    // Brighter than the wall, dimmer than anything in front of
                    // it: it is a decision, not an object.
                    .graphicsLayer { alpha = 0.40f },
            ) {
                wallPiece(atlas, ghostSlug, ghost.value.x, ghost.value.y)
            }
        }
    }
}

/** One wall piece, in cell coordinates. Opaque: the layer does the fading. */
private fun DrawScope.wallPiece(atlas: BrickAtlas, slug: String?, col: Float, row: Float) {
    val studPx = size.width / (WallCols * WallPiece.w).toFloat()
    val coursePx = studPx * WallPiece.h * BrickArt.ROW_RATIO
    drawBrickArt(
        atlas = atlas,
        asset = BrickArt.assetFor(slug, WallPiece),
        studs = WallPiece.w,
        rows = WallPiece.h,
        left = col * studPx * WallPiece.w,
        bottom = size.height - row * coursePx,
        studPx = studPx,
    )
}

@Preview(name = "Splash", showBackground = true, heightDp = 640)
@Composable
private fun PreviewSplash() = AmpersandTheme {
    SplashScreen(you = BrickColor.RED, them = BrickColor.BLUE)
}
