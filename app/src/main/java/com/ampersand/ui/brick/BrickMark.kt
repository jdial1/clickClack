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

/** The 2-stud brick used in the app's mark. */
private val Tile = BrickShape(2, BrickHeight.MEDIUM)

/**
 * The app logo: two bricks in running bond labeled "Click" and "Clack".
 * Used animated on the splash and static on sign-in. Offsets are in courses, not dp.
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

        // Center each word on the brick body, below the nub.
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
                // Scaled from the stud size so the mark fits at any size.
                fontSize = (stud.value * 0.46f).sp,
                letterSpacing = (-0.5).sp,
            ),
            color = inkOnBrick(slug).copy(alpha = alpha),
        )
    }
}
