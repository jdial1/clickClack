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
import androidx.compose.ui.unit.sp
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

// MEDIUM, matching the nav tab. These are two identical-looking 2-stud
// tiles on two bars that sit within a hundred dp of each other, and they were
// different thicknesses. HIGH is for things thick enough to press *down*,
// which is a button, not a row of markers.
private val Tile = BrickShape(2, BrickHeight.MEDIUM)

/**
 * The drawable behind a [Reaction].
 *
 * Kenney's emotes pack, the speech-bubble style: a white bubble stays legible
 * on all four brick colours, and a bubble is the right shape for the one place
 * in the app where you say something back. Mapped here rather than in the enum
 * so the model keeps no Android imports.
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
                // The unpicked ones are the same brick, further away. Hiding
                // them until you tap would mean not knowing what is on offer.
                // Glide, not settle: `lift` feeds the brick's alpha below, and
                // a bouncy spring overshoots 1.0 straight into drawImage. The
                // spring lives on the emote's scale instead, where overshoot is
                // the whole point.
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
                            // Not 0.3: six bricks of the same colour sit edge
                            // to edge here, and that far back they stop having
                            // visible edges and read as one smear rather than
                            // as six things you can press. The nav bar gets
                            // away with 0.34 because black joiners separate it.
                            alpha = 0.55f + 0.45f * lift,
                        )
                    }
                    Image(
                        painter = painterResource(reaction.drawable()),
                        contentDescription = reaction.label,
                        modifier = Modifier
                            .align(Alignment.Center)
                            // Body starts below the nub; centring in the whole
                            // tile sits the art half on the studs.
                            .padding(top = nub)
                            // The chosen one leans in. A tenth, not a pop: six
                            // of these sit in a row and anything larger turns
                            // the row into a fairground.
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
        BrickBadge(color, reaction)
        Spacer(Modifier.size(10.dp))
        Text(
            text = "$name: ${reaction.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A single 2-stud brick with one emoji sitting on it.
 *
 * Sized from the brick rather than from the glyph. At a 15dp stud the body is
 * only 13dp tall and a 15sp emoji hangs off the top and both sides, which looks
 * like a rendering bug rather than a badge -- so the stud is set first and the
 * glyph is sized to the body it has to fit inside.
 */
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
