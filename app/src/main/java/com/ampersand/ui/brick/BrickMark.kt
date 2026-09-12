package com.ampersand.ui.brick

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.ui.theme.Stud
import com.ampersand.ui.theme.Wordmark

/** Two 2-stud bricks in running bond. The app's mark, everywhere it appears. */
private val Tile = BrickShape(2, BrickHeight.MEDIUM)

/**
 * The name, written on the thing the name is about.
 *
 * Two bricks with a word each, read top to bottom: *Click*, then *Clack*. A
 * wordmark set beside a picture of two bricks is a logo with an illustration
 * next to it; a wordmark set *on* them is the object saying its own name, which
 * is the only version of this mark worth having.
 *
 * The same composable animates on the splash and sits still on the sign-in
 * screen. They were two different marks before -- a falling pair on one and a
 * twelve-stud slab with a line of type across it on the other -- which meant
 * the first two screens of the app disagreed about what it was called.
 *
 * Offsets are in *courses* rather than dp, so a brick starting five of its own
 * heights up falls at the same apparent speed on any screen.
 */
@Composable
fun BrickMark(
    you: BrickColor,
    them: BrickColor,
    modifier: Modifier = Modifier,
    stud: Dp = Stud.hero,
    upperCourses: Float = 0f,
    lowerCourses: Float = 0f,
    textAlpha: Float = 1f,
) {
    val course = stud * Tile.h * BrickArt.ROW_RATIO
    val nub = stud * BrickArt.NUB_RATIO

    val upper = you.asset(Tile)
    val lower = them.asset(Tile)
    val atlas = rememberBricks(listOf(upper, lower))

    Box(modifier.size(stud * Tile.w * 2, course * 2 + nub)) {
        Canvas(Modifier.size(stud * Tile.w * 2, course * 2 + nub)) {
            val studPx = size.width / (Tile.w * 2f)
            val coursePx = studPx * Tile.h * BrickArt.ROW_RATIO
            val span = studPx * Tile.w

            drawBrickArt(
                atlas = atlas,
                asset = lower,
                studs = Tile.w,
                rows = Tile.h,
                left = span * 0.25f,
                bottom = size.height + lowerCourses * coursePx,
                studPx = studPx,
            )
            drawBrickArt(
                atlas = atlas,
                asset = upper,
                studs = Tile.w,
                rows = Tile.h,
                // Offset by a stud, so they land in running bond.
                left = span * 0.75f,
                bottom = size.height - coursePx + upperCourses * coursePx,
                studPx = studPx,
            )
        }

        // Each word sits on its own brick's *body*, which starts below that
        // brick's nub -- centring through the nub would ride the type up onto
        // the studs.
        Word(
            text = "Click",
            slug = you.slug,
            stud = stud,
            alpha = textAlpha,
            modifier = Modifier
                .offset(x = stud * 1.5f, y = nub)
                .size(stud * Tile.w, course)
                .graphicsLayer { translationY = upperCourses * course.toPx() },
        )
        Word(
            text = "Clack",
            slug = them.slug,
            stud = stud,
            alpha = textAlpha,
            modifier = Modifier
                .offset(x = stud * 0.5f, y = course + nub)
                .size(stud * Tile.w, course)
                .graphicsLayer { translationY = lowerCourses * course.toPx() },
        )
    }
}

@Composable
private fun Word(
    text: String,
    slug: String?,
    stud: Dp,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = Wordmark,
                // Sized from the brick rather than from the type scale: the
                // mark has to hold together at 46dp on the splash and at 34 on
                // the sign-in screen, and a fixed sp would overflow one of them.
                fontSize = (stud.value * 0.46f).sp,
                letterSpacing = (-0.5).sp,
            ),
            color = inkOnBrick(slug).copy(alpha = alpha),
        )
    }
}
