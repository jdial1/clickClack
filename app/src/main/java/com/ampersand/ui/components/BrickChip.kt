package com.ampersand.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickPill
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Alpha
import com.ampersand.ui.theme.IconSize
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.theme.Space
import com.ampersand.ui.theme.Stud

/** Toggle chip drawn as a brick pill; selected is full brightness, unselected is faded. */
@Composable
fun BrickChip(
    label: String,
    on: Boolean,
    color: BrickColor?,
    icon: UiIcon? = null,
    onClick: () -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    // Color stays constant; on/off is shown by alpha only.
    val lift by animateFloatAsState(if (on) 1f else 0f, Motion.glide, label = "chipLift")

    BrickPill(
        slug = color?.slug,
        studSize = Stud.chip,
        height = BrickHeight.MEDIUM,
        alpha = Alpha.rise(Alpha.quiet, lift),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) {
            sounds.play(if (on) Sfx.DESELECT else Sfx.SELECT)
            feel.play(if (on) Feel.DESELECT else Feel.SELECT)
            onClick()
        },
    ) {
        Row(Modifier.padding(horizontal = Space.m, vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                IconBrick(it, size = IconSize.small)
                Spacer(Modifier.width(Space.s))
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = inkOnBrick(color?.slug))
        }
    }
}
