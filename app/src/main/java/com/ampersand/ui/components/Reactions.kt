package com.ampersand.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.model.Reaction
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play

private val Tile = BrickShape(2, BrickHeight.HIGH)

/**
 * The six reactions, each on its own brick, in your colour.
 *
 * Two studs wide and four rows high apiece, so the six of them come to exactly
 * twelve studs -- the same width as the plate, the hero and every slab in the
 * app. Nothing here is sized in dp: the run is laid out on stud weights, so it
 * stays a true brick run at any screen width.
 *
 * Tapping your current reaction clears it. That matters more than it sounds:
 * the alternative is that a mis-tap is permanent, and the whole reason this is
 * a reaction and not an answer is that it is allowed to be taken back.
 */
@Composable
fun AmpReactionRow(
    selected: Reaction?,
    color: BrickColor,
    onSelect: (Reaction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sounds = LocalSounds.current
    val asset = color.asset(Tile)
    val atlas = rememberBricks(listOf(asset))

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stud = maxWidth / (Reaction.entries.size * Tile.w)
        val tall = stud * (Tile.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO)
        val nub = stud * BrickArt.NUB_RATIO

        Row(Modifier.fillMaxWidth().height(tall)) {
            Reaction.entries.forEach { reaction ->
                val on = reaction == selected
                // The unpicked ones are the same brick, further away. Hiding
                // them until you tap would mean not knowing what is on offer.
                val lift by animateFloatAsState(if (on) 1f else 0f, label = "reactionLift")

                Box(
                    Modifier
                        .width(stud * Tile.w)
                        .height(tall)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            sounds.play(if (on) Sfx.DESELECT else Sfx.SELECT)
                            onSelect(if (on) null else reaction)
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawBrickArt(
                            atlas = atlas,
                            asset = asset,
                            studs = Tile.w,
                            rows = Tile.h,
                            left = 0f,
                            bottom = size.height,
                            studPx = size.width / Tile.w,
                            alpha = 0.3f + 0.7f * lift,
                        )
                    }
                    Text(
                        text = reaction.emoji,
                        fontSize = (stud.value * 0.62f).sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            // Body starts below the nub; centring in the whole
                            // tile sits the glyph half on the studs.
                            .padding(top = nub),
                    )
                }
            }
        }
    }
}

/**
 * What they said back, in their colour and their words.
 *
 * Shows the [Reaction.label] rather than only the glyph. An emoji on its own is
 * ambiguous in exactly the way this app is trying not to be, and the label is
 * the sentence the glyph was standing in for all along.
 */
@Composable
fun PartnerReaction(
    name: String,
    reaction: Reaction,
    color: BrickColor,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            BrickBadge(color)
            Text(reaction.emoji, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = "$name: ${reaction.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A single 2-stud brick, sized to hold one emoji. */
@Composable
private fun BrickBadge(color: BrickColor) {
    val shape = BrickShape(2, BrickHeight.MEDIUM)
    val asset = color.asset(shape)
    val atlas = rememberBricks(listOf(asset))
    val stud = 15.dp

    Canvas(
        Modifier.size(
            stud * shape.w,
            stud * (shape.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO),
        ),
    ) {
        drawBrickArt(
            atlas = atlas,
            asset = asset,
            studs = shape.w,
            rows = shape.h,
            left = 0f,
            bottom = size.height,
            studPx = size.width / shape.w,
        )
    }
}
