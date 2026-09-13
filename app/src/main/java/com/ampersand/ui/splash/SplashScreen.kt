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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.BrickMark
import com.ampersand.ui.brick.BrickAtlas
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.awaitReady
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Stud
import kotlinx.coroutines.delay
import kotlin.random.Random

private val Tile = BrickShape(2, BrickHeight.MEDIUM)
private val StudSize = Stud.hero

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
                BrickMark(
                    you = you,
                    them = them,
                    upperCourses = upperY.value,
                    lowerCourses = lowerY.value,
                    // The words arrive after the bricks have, so the mark reads
                    // as two objects landing and then being named rather than
                    // as a logo dropped in whole.
                    textAlpha = fade.value,
                )
            }
        }
    }
}


/** One piece of the wall: colour, where it starts in studs, and how wide. */
private data class Piece(val slug: String?, val stud: Int, val row: Int, val width: Int)

/** The wall is a 12-stud course, the same width the rest of the app builds on. */
private const val WallStuds = 12
private const val WallRows = 32
private val WallHeight = BrickHeight.MEDIUM

/** Courses already down when the splash opens. */
private const val WallBase = 7

/** How often a course leaves a hole rather than laying the next piece. */
private const val GapChance = 0.28f

/**
 * The wall behind the splash, being built while you watch.
 *
 * Everywhere else in the app the wall is a still background generated once from
 * a fixed seed -- it is the ground, and ground does not move. Here it is the
 * only screen with nothing to do, so it does the thing the whole app is about:
 * a piece arrives, hovers while it decides, and settles. By the time the
 * wordmark has faded up there is a wall under it that was not there when you
 * opened the app.
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
    val atlas = rememberBricks(
        remember(slugs) {
            slugs.flatMap { slug ->
                WallWidths.map { BrickArt.assetFor(slug, BrickShape(it, WallHeight)) }
            }
        },
    )

    val placed = remember { mutableStateListOf<Piece>() }
    val ghost = remember { Animatable(Offset(4f, 0f), Offset.VectorConverter) }
    var ghostPiece by remember { mutableStateOf<Piece?>(null) }

    LaunchedEffect(Unit) {
        val rng = Random(11)
        val courses = (0 until WallRows).flatMap { row -> courseOf(row, rng, slugs) }

        // The bottom courses are already down when you arrive. A splash that
        // starts on bare ground and places six pieces reads as an empty screen
        // with something loading on it; starting part-built reads as a wall
        // that is going up, which is the thing worth showing.
        val seeded = courses.count { it.row < WallBase }
        placed.addAll(courses.take(seeded))

        courses.drop(seeded).forEach { piece ->
            ghostPiece = piece
            // One hesitation, not three, but a hesitation you can actually
            // see. At the original pacing the ghost crossed the wall and
            // landed inside a sixth of a second, which read as pieces
            // appearing rather than as a piece being carried somewhere.
            ghost.animateTo(
                targetValue = Offset(
                    rng.nextInt(WallStuds - piece.width + 1).toFloat(),
                    (piece.row + rng.nextFloat() * 2f).coerceAtMost(WallRows - 1f),
                ),
                animationSpec = tween(WanderMs, easing = FastOutSlowInEasing),
            )
            // A tween rather than a spring for the landing. A spring settles
            // over a couple of hundred milliseconds of its own, which at this
            // cadence would blur the moment of contact into the next wander --
            // and contact is the only frame in the loop worth seeing.
            ghost.animateTo(
                targetValue = Offset(piece.stud.toFloat(), piece.row.toFloat()),
                animationSpec = tween(PlaceMs, easing = FastOutSlowInEasing),
            )
            placed += piece
        }
        ghostPiece = null
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
            placed.forEach { p -> wallPiece(atlas, p, p.stud.toFloat(), p.row.toFloat()) }
        }

        ghostPiece?.let { piece ->
            Canvas(
                Modifier
                    .fillMaxSize()
                    // Brighter than the wall, dimmer than anything in front of
                    // it: it is a decision, not an object.
                    .graphicsLayer { alpha = 0.40f },
            ) {
                wallPiece(atlas, piece, ghost.value.x, ghost.value.y)
            }
        }
    }
}

/** The stock widths this wall builds from. */
/**
 * How long the ghost drifts before it commits, and how long the drop takes.
 *
 * Together they are the whole pacing of the wall: one piece every 600ms, or
 * not quite two a second. They were 70 and 80, which is six and a half pieces
 * a second -- fast enough that the ghost never appeared to be anywhere, and
 * the wall read as bricks popping into existence rather than as something
 * being built. Quartering the speed is what makes the wandering legible, which
 * was the only reason to have a ghost rather than just fade pieces in.
 *
 * The wall keeps building for as long as anyone is looking at it, so a slower
 * cadence costs nothing: it is not a progress bar and has nothing to finish.
 */
private const val WanderMs = 280
private const val PlaceMs = 320

private val WallWidths = listOf(6, 4, 2)

/**
 * One course, left to right, with holes in it.
 *
 * A wall of identical pieces butted edge to edge is a tiled background, which
 * is what the app already has three screens of. Mixed widths give it bond, and
 * the gaps are what stop it reading as a texture: a wall someone is part way
 * through building has holes in it, and those holes are the only thing that
 * says a person is doing this rather than a pattern being drawn.
 *
 * Gaps are two studs, because a one-stud hole reads as a seam and a four-stud
 * hole reads as a missing row.
 */
private fun courseOf(row: Int, rng: Random, slugs: List<String?>): List<Piece> {
    val out = mutableListOf<Piece>()
    var stud = 0
    while (stud < WallStuds) {
        val room = WallStuds - stud
        val fits = WallWidths.filter { it <= room }
        // Never open the course with a hole: a wall that starts in mid-air
        // looks like a drawing mistake rather than an unfinished job.
        if (stud > 0 && room > 2 && rng.nextFloat() < GapChance) {
            stud += 2
            continue
        }
        val width = fits[rng.nextInt(fits.size)]
        out += Piece(slugs[rng.nextInt(slugs.size)], stud, row, width)
        stud += width
    }
    // Shuffled so a course fills unevenly rather than sweeping left to right
    // like a progress bar. Order only matters to the eye -- every piece in a
    // course lands before the one above it either way.
    return out.shuffled(rng)
}

/** One wall piece, in stud coordinates. Opaque: the layer does the fading. */
private fun DrawScope.wallPiece(atlas: BrickAtlas, piece: Piece, stud: Float, row: Float) {
    val studPx = size.width / WallStuds
    val coursePx = studPx * WallHeight.rows * BrickArt.ROW_RATIO
    drawBrickArt(
        atlas = atlas,
        asset = BrickArt.assetFor(piece.slug, BrickShape(piece.width, WallHeight)),
        studs = piece.width,
        rows = WallHeight.rows,
        left = stud * studPx,
        bottom = size.height - row * coursePx,
        studPx = studPx,
    )
}


@Preview(name = "Splash", showBackground = true, heightDp = 640)
@Composable
private fun PreviewSplash() = AmpersandTheme {
    SplashScreen(you = BrickColor.RED, them = BrickColor.BLUE)
}
