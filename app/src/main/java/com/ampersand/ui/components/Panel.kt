package com.ampersand.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.glass
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.theme.Corner
import com.ampersand.ui.theme.Space

/** How much a panel lifts off the wall. */
enum class Emphasis(val strength: Float) {
    NORMAL(1f),
    RAISED(1.4f),
    SELECTED(2.2f),
}

/**
 * The neutral card: a glass pane on the wall, for content you read. Order the chain
 * `.panel().clickable { }.padding(...)` so the press area covers the whole pane.
 */
@Composable
fun Modifier.panel(emphasis: Emphasis = Emphasis.NORMAL): Modifier =
    glass(inkOnBrick(structureSlug()), corner = Corner.medium, strength = emphasis.strength)

/**
 * A row you press on its own: a title, a line of detail, and something at the end. One
 * brick high, the same as [AmpButton], because things you press are bricks and they
 * should come in one size. The text sits directly on the brick, with no pane behind it.
 */
@Composable
fun ActionRow(
    title: String,
    detail: String?,
    modifier: Modifier = Modifier,
    slug: String? = structureSlug(),
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val ink = inkOnBrick(slug)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    BrickSlab(
        slugs = listOf(slug),
        studs = 12,
        height = BrickHeight.HIGH,
        snug = true,
        modifier = modifier
            .fillMaxWidth()
            .pressDepth(pressed)
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick),
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = Space.l, end = Space.l, top = Space.s, bottom = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = ink)
                detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ink) }
            }
            trailing()
        }
    }
}
