package com.ampersand.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampersand.model.Depth
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.BrickPill
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.quietSlug
import com.ampersand.ui.theme.Stud
import com.ampersand.ui.theme.toArgbLong
import com.ampersand.ui.brick.onWall

/** Small, letter-spaced, uppercase. Used for every section heading in the app. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    /**
     * False when the label is already inside a panel of its own. Two frostings
     * stacked is a panel with a panel in it, which reads as a mistake.
     */
    panel: Boolean = true,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = if (panel) modifier.onWall(corner = 10.dp) else modifier,
    )
}

/**
 * Three studs, filled to the question's depth. Light Tuesday vs. real evening.
 *
 * [tint] is for the gradient hero, where theme colours would vanish.
 */
@Composable
fun DepthDots(depth: Depth, modifier: Modifier = Modifier, tint: Color? = null) {
    // Neutral brick: depth sits on the gradient hero, where a palette colour
    // would disappear into the background it is drawn on.
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { i ->
            BrickDot(slug = quietSlug(), studSize = Stud.pip, filled = i < depth.level)
        }
    }
}

/**
 * A person's initial on their own brick. The only "avatar" the app needs.
 *
 * Was a circle. A round avatar is the single most "generic app" shape there is,
 * and it sat next to actual bricks everywhere it appeared.
 */
@Composable
fun PersonDot(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 28,
) {
    // One real stud with an initial on it. Sized from [size] so every existing
    // call site keeps the footprint it asked for, nub included.
    val slug = BrickColor.of(color.toArgbLong()).slug
    BrickPill(
        slug = slug,
        studSize = (size * 0.82f).dp,
        height = BrickHeight.LOW,
        modifier = modifier,
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = inkOnBrick(slug),
        )
    }
}

/** Centred, quiet, used for empty and waiting states. */
@Composable
fun QuietNote(text: String, modifier: Modifier = Modifier, panel: Boolean = true) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = (if (panel) modifier.onWall() else modifier).fillMaxWidth(),
    )
}
