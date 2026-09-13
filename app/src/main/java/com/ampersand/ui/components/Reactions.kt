package com.ampersand.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ampersand.R
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.model.Reaction
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Motion

// MEDIUM to match the nav tabs.
private val Tile = BrickShape(2, BrickHeight.MEDIUM)

/**
 * Drawable for a [Reaction], from Kenney's emotes pack (speech-bubble style).
 * Mapped here so the model has no Android imports.
 */
@DrawableRes
fun Reaction.drawable(): Int = when (icon) {
    "heart" -> R.drawable.emote_heart
    "laugh" -> R.drawable.emote_laugh
    "hearts" -> R.drawable.emote_hearts
    "exclamation" -> R.drawable.emote_exclamation
    "question" -> R.drawable.emote_question
    else -> R.drawable.emote_star
}

/**
 * Row of reaction bricks in your color, sized from the width (12 studs total).
 * Tapping the current reaction clears it.
 */
@Composable
fun AmpReactionRow(
    selected: Reaction?,
    color: BrickColor,
    onSelect: (Reaction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val asset = color.asset(Tile)
    val atlas = rememberBricks(listOf(asset))

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stud = maxWidth / (Reaction.entries.size * Tile.w)
        val tall = stud * (Tile.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO)
        val nub = stud * BrickArt.NUB_RATIO

        Row(Modifier.fillMaxWidth().height(tall)) {
            Reaction.entries.forEach { reaction ->
                val on = reaction == selected
                // `lift` uses glide, not a spring: it feeds alpha, and overshoot past 1.0 breaks drawImage.
                val lift by animateFloatAsState(
                    if (on) 1f else 0f, Motion.glide, label = "reactionLift",
                )
                val pop by animateFloatAsState(
                    if (on) 1f else 0f, Motion.settle, label = "reactionPop",
                )

                Box(
                    Modifier
                        .width(stud * Tile.w)
                        .height(tall)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            sounds.play(if (on) Sfx.DESELECT else Sfx.SELECT)
                            feel.play(if (on) Feel.DESELECT else Feel.SELECT)
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
                            // Higher floor than the nav bar so adjacent same-color bricks stay distinct.
                            alpha = 0.55f + 0.45f * lift,
                        )
                    }
                    Image(
                        painter = painterResource(reaction.drawable()),
                        contentDescription = reaction.label,
                        modifier = Modifier
                            .align(Alignment.Center)
                            // Center on the body, below the nub.
                            .padding(top = nub)
                            // Selected emote scales up 10%.
                            .graphicsLayer {
                                scaleX = 1f + 0.10f * pop
                                scaleY = 1f + 0.10f * pop
                            }
                            .size(stud * 1.1f),
                    )
                }
            }
        }
    }
}

/** The partner's reaction: badge plus "name: label" text. */
@Composable
fun PartnerReaction(
    name: String,
    reaction: Reaction,
    color: BrickColor,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        BrickBadge(color, reaction)
        Spacer(Modifier.size(10.dp))
        Text(
            text = "$name: ${reaction.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A 2-stud brick with the reaction emote sized to fit its body. */
@Composable
private fun BrickBadge(color: BrickColor, reaction: Reaction) {
    val shape = BrickShape(2, BrickHeight.MEDIUM)
    val asset = color.asset(shape)
    val atlas = rememberBricks(listOf(asset))
    val stud = 21.dp
    val body = stud * shape.h * BrickArt.ROW_RATIO
    val nub = stud * BrickArt.NUB_RATIO

    Box(
        Modifier.size(stud * shape.w, body + nub),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
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
        Image(
            painter = painterResource(reaction.drawable()),
            contentDescription = reaction.label,
            modifier = Modifier.padding(top = nub).size(body * 0.9f),
        )
    }
}
