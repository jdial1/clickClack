package com.ampersand.ui.components

import com.ampersand.ui.theme.IconSize
import com.ampersand.ui.theme.Space
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ampersand.R

/** Icon bricks for repeated actions. Drawn by tools/ui/icons.py, all in the grey slate brick. */
enum class UiIcon(@DrawableRes val res: Int) {
    SETTINGS(R.drawable.icon_settings),
    DETAILS(R.drawable.icon_details),
    BACK(R.drawable.icon_back),
    NEXT(R.drawable.icon_next),
    DONE(R.drawable.icon_done),
    ADD(R.drawable.icon_add),
    CLOSE(R.drawable.icon_close),
    WRITE(R.drawable.icon_write),
    RETAKE(R.drawable.icon_retake),
    FILTER(R.drawable.icon_filter),
    SHOW_MORE(R.drawable.icon_show_more),
    REMINDER(R.drawable.icon_reminder),
    DATES(R.drawable.icon_dates),
    SIGN_OUT(R.drawable.icon_sign_out),
    KEEP(R.drawable.icon_keep),
}

@Composable
fun IconBrick(icon: UiIcon, modifier: Modifier = Modifier, size: Dp = com.ampersand.ui.theme.IconSize.medium, turned: Boolean = false) {
    Image(
        painterResource(icon.res),
        contentDescription = null,
        modifier = modifier.size(size).then(if (turned) Modifier.rotate(180f) else Modifier),
    )
}

/** An icon brick and its label, for small text actions like Back, Remove and Edit. */
@Composable
fun IconAction(
    icon: UiIcon,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    turned: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
            )
            .padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBrick(icon, turned = turned)
        Spacer(Modifier.width(Space.s))
        Text(label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** The theme's icon brick, in the theme's colour. Drawn by tools/ui/themes.py. */
@Composable
fun ThemeIcon(theme: com.ampersand.model.QuizTheme, modifier: Modifier = Modifier, size: Dp = com.ampersand.ui.theme.IconSize.tile) {
    val res = when (theme) {
        com.ampersand.model.QuizTheme.KNOW_ME -> R.drawable.theme_know_me
        com.ampersand.model.QuizTheme.EVERYDAY -> R.drawable.theme_everyday
        com.ampersand.model.QuizTheme.FUTURE -> R.drawable.theme_future
        com.ampersand.model.QuizTheme.PLAYFUL -> R.drawable.theme_playful
        com.ampersand.model.QuizTheme.DEEP -> R.drawable.theme_deep
        com.ampersand.model.QuizTheme.FOOD -> R.drawable.theme_food
        com.ampersand.model.QuizTheme.TRAVEL -> R.drawable.theme_travel
        com.ampersand.model.QuizTheme.ROMANTIC -> R.drawable.theme_romantic
        com.ampersand.model.QuizTheme.SPICY -> R.drawable.theme_spicy
    }
    Image(painterResource(res), contentDescription = null, modifier = modifier.size(size))
}
