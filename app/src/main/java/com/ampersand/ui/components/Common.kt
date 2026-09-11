package com.ampersand.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampersand.model.Depth
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.theme.Duo

/** Small, letter-spaced, uppercase. Used for every section heading in the app. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
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
            BrickDot(slug = null, studSize = 13.dp, filled = i < depth.level)
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
    val corner = (size * 0.26f).dp
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner))
            .brickFace(color, corner = corner, lip = (size * 0.13f).dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Duo.inkOn(color),
            modifier = Modifier.padding(bottom = (size * 0.12f).dp),
        )
    }
}

/** Centred, quiet, used for empty and waiting states. */
@Composable
fun QuietNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
