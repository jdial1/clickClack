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
import kotlinx.coroutines.delay
import kotlin.random.Random


/** Accelerating easing for falling bricks. */
private val Gravity = CubicBezierEasing(0.5f, 0f, 1f, 0.5f)

/** Starting offset above rest, in courses. */
private const val DropFrom = -5f

/**
 * Splash: two bricks drop and stack (click, clack), then the wordmark fades in over a building wall.
 * Sounds fire when each fall's `animateTo` returns, so they stay in sync with contact.
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

    LaunchedEffect(Unit) {
        // Wait for decode: SoundPool silently drops plays before a sample is ready (cold start was silent).
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
        // Hold a moment so the background wall keeps building.
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
                    // Text fades in after both bricks land.
                    textAlpha = fade.value,
                )
            }
        }
    }
}


/** One wall piece: color, start stud, row, and width in studs. */
private data class Piece(val slug: String?, val stud: Int, val row: Int, val width: Int)

/** Wall width in studs, matching the rest of the app. */
private const val WallStuds = 12
private const val WallRows = 32
private val WallHeight = BrickHeight.MEDIUM

/** Courses already down when the splash opens. */
private const val WallBase = 7

/** Chance a course leaves a gap instead of placing the next piece. */
private const val GapChance = 0.28f

/**
 * Animated background wall, built bottom-up from a fixed seed. Each new piece is a
 * brighter "ghost" that wanders briefly, then drops into its slot.
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

        // Start with the bottom WallBase courses already placed.
        val seeded = courses.count { it.row < WallBase }
        placed.addAll(courses.take(seeded))

        courses.drop(seeded).forEach { piece ->
            ghostPiece = piece
            // Wander to one random spot first.
            ghost.animateTo(
                targetValue = Offset(
                    rng.nextInt(WallStuds - piece.width + 1).toFloat(),
                    (piece.row + rng.nextFloat() * 2f).coerceAtMost(WallRows - 1f),
                ),
                animationSpec = tween(WanderMs, easing = FastOutSlowInEasing),
            )
            // Tween, not spring: a spring's settle time would blur into the next wander.
            ghost.animateTo(
                targetValue = Offset(piece.stud.toFloat(), piece.row.toFloat()),
                animationSpec = tween(PlaceMs, easing = FastOutSlowInEasing),
            )
            placed += piece
        }
        ghostPiece = null
    }

    // Alpha on the layer, not per brick, or lower nubs show through the bricks above (see BrickWall).
    // Separate layer for the ghost because it uses a different alpha.
    Box(modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.13f },
        ) {
            // Read in the draw lambda so a new piece only invalidates draw.
            placed.forEach { p -> wallPiece(atlas, p, p.stud.toFloat(), p.row.toFloat()) }
        }

        ghostPiece?.let { piece ->
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.40f },
            ) {
                wallPiece(atlas, piece, ghost.value.x, ghost.value.y)
            }
        }
    }
}

/** Ghost wander and drop durations (about one piece per 600ms). Faster made the ghost unreadable. */
private const val WanderMs = 280
private const val PlaceMs = 320

/** Brick widths the wall uses. */
private val WallWidths = listOf(6, 4, 2)

/** One course of mixed-width pieces with random 2-stud gaps. */
private fun courseOf(row: Int, rng: Random, slugs: List<String?>): List<Piece> {
    val out = mutableListOf<Piece>()
    var stud = 0
    while (stud < WallStuds) {
        val room = WallStuds - stud
        val fits = WallWidths.filter { it <= room }
        // Never start a course with a gap.
        if (stud > 0 && room > 2 && rng.nextFloat() < GapChance) {
            stud += 2
            continue
        }
        val width = fits[rng.nextInt(fits.size)]
        out += Piece(slugs[rng.nextInt(slugs.size)], stud, row, width)
        stud += width
    }
    // Shuffle placement order within the course (visual only).
    return out.shuffled(rng)
}

/** Draws one wall piece at stud coordinates, opaque (the layer applies alpha). */
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
