package com.ampersand.ui.components

import com.ampersand.ui.theme.Corner
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Space
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
    /** False when the label is already inside a panel, to avoid a double backdrop. */
    panel: Boolean = true,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = if (panel) modifier.onWall(corner = Corner.medium) else modifier,
    )
}

/** Three studs, filled to the question's depth level. [tint] is currently unused. */
@Composable
fun DepthDots(depth: Depth, modifier: Modifier = Modifier, tint: Color? = null) {
    // Neutral brick so it stays visible on the gradient hero.
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        repeat(3) { i ->
            BrickDot(slug = quietSlug(), studSize = Stud.pip, filled = i < depth.level)
        }
    }
}

/** A person's initial on a one-stud brick in their color. */
@Composable
fun PersonDot(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 28,
) {
    // Stud scaled so the total footprint, nub included, matches [size].
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

/** The one way back: a brick, because things you press are bricks. Top left of a screen. */
@Composable
fun BackAction(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    BrickPill(
        slug = structureSlug(),
        studSize = Stud.chip,
        height = BrickHeight.MEDIUM,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) {
            sounds.play(Sfx.NAV)
            feel.play(Feel.NAV)
            onBack()
        },
    ) {
        IconAction(
            UiIcon.BACK,
            "Back",
            Modifier.padding(horizontal = Space.m, vertical = Space.hair),
            color = inkOnBrick(structureSlug()),
        )
    }
}

/** A way out that should not compete with the brick beside it: "Not now", "Leave". */
@Composable
fun QuietAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                sounds.play(Sfx.NAV)
                feel.play(Feel.NAV)
                onClick()
            }
            .padding(vertical = Space.s),
    )
}

/** Centered muted text for empty and waiting states. */
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
