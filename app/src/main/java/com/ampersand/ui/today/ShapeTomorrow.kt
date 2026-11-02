package com.ampersand.ui.today

import com.ampersand.ui.components.panel
import com.ampersand.ui.components.Emphasis
import com.ampersand.ui.theme.IconSize
import com.ampersand.ui.theme.Alpha
import com.ampersand.ui.theme.Radius
import com.ampersand.ui.theme.Corner
import com.ampersand.ui.theme.Inset
import com.ampersand.ui.theme.Space
import com.ampersand.ui.components.rememberPhoto
import com.ampersand.ui.components.compressPhoto
import com.ampersand.ui.brick.BrickPill
import com.ampersand.ui.components.IconBrick
import androidx.compose.foundation.border
import com.ampersand.ui.components.IconAction
import com.ampersand.model.Moments
import com.ampersand.model.Moment
import com.ampersand.ui.components.ThemeIcon
import com.ampersand.ui.components.UiIcon
import com.ampersand.model.DayEntry
import com.ampersand.ui.components.WeatherBrickArt
import com.ampersand.ui.components.MoodArt
import com.ampersand.model.MoodEntry
import com.ampersand.model.Mood
import androidx.compose.foundation.layout.width
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.structureSlug
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.data.MockQuestions
import com.ampersand.model.Answer
import com.ampersand.model.QuizTheme
import com.ampersand.model.Turns
import com.ampersand.ui.quiz.BrickChip
import androidx.compose.foundation.layout.FlowRow
import com.ampersand.model.label
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.ampersand.model.CoupleState
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.AmpCheckboxGroup
import com.ampersand.ui.components.AmpDragRank
import com.ampersand.ui.components.AmpDropdown
import com.ampersand.ui.components.AmpField
import com.ampersand.ui.components.AmpRadioGroup
import com.ampersand.ui.components.AmpScale
import com.ampersand.ui.components.DepthDots
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.glass
import com.ampersand.ui.brick.quietSlug
import com.ampersand.ui.brick.inkOnBricks
import com.ampersand.ui.theme.toArgbLong
import com.ampersand.ui.components.QuietNote
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.components.BackAction
import com.ampersand.ui.components.QuietAction
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.SpecialGold
import com.ampersand.ui.theme.Stud
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** What you are asked next: tomorrow's theme once today is open, and a question of your own. */
@Composable
internal fun ShapeTomorrow(
    state: CoupleState,
    themes: List<QuizTheme>,
    onPick: (String) -> Unit,
    onWrite: () -> Unit,
) {
    SectionLabel("Shape tomorrow")
    Spacer(Modifier.height(Space.m))
    if (state.today.isRevealed && themes.isNotEmpty()) {
        TomorrowPick(state, themes, onPick)
        Spacer(Modifier.height(Space.m))
    }
    WriteOne(state, onWrite)
}

/** Button to write your own question, plus a note on how many are already queued. */
@Composable
internal fun WriteOne(state: CoupleState, onWrite: () -> Unit) {
    val you = BrickColor.of(state.you.colorArgb)
    val queued = state.wildcards
    val mine = queued.count { it.byUid == state.you.uid }
    val theirs = queued.size - mine

    AmpButton(
        label = if (queued.isEmpty()) "Write a question for ${state.them.name}" else "Write another",
        icon = UiIcon.WRITE,
        slug = you.slug,
        onClick = onWrite,
    )

    if (queued.isEmpty()) {
        Spacer(Modifier.height(Space.s))
        QuietNote("It arrives as a card with your name on it, and ${state.them.name} gets a notification.")
    }

    if (queued.isNotEmpty()) {
        Spacer(Modifier.height(Space.m))
        QuietNote(
            when {
                mine > 0 && theirs > 0 ->
                    "${count(mine)} from you and ${count(theirs)} from ${state.them.name} are queued."
                theirs > 0 ->
                    "${state.them.name} wrote one. It's up next."
                mine == 1 -> "Yours is up next."
                else -> "$mine of yours are queued."
            },
        )
    }
}

internal fun count(n: Int) = if (n == 1) "One" else n.toString()

/** One-tap "we talked about it" for a revealed day. Not undoable; hidden once marked. */
@Composable
internal fun DidYouTalk(state: CoupleState, day: DayEntry, onTalkedAbout: (LocalDate) -> Unit) {
    if (!day.isRevealed || day.wasTalkedAbout) return
    val yesterday = day

    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val interaction = remember { MutableInteractionSource() }
    val you = BrickColor.of(state.you.colorArgb)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(Radius.medium)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = Alpha.strong))
            .clickable(interactionSource = interaction, indication = null) {
                sounds.play(Sfx.SEAL)
                feel.play(Feel.SEAL)
                onTalkedAbout(yesterday.date)
            }
            .padding(horizontal = Space.m, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        BrickDot(slug = you.slug, studSize = Stud.pip)
        Column(Modifier.weight(1f)) {
            Text(
                "Did you talk about this one?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Space.hair))
            Text(
                yesterday.question.text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Text(
            "We did",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Who picked today's theme, or null. Checked against the question's category,
 * since a wildcard, capsule or occasion can override the pick.
 */
internal fun pickedBy(state: CoupleState): String? {
    val theme = Turns.themeFor(state.today.date, state.history, state.you.uid, state.them.uid)
        ?: return null
    if (state.today.question.category != theme) return null
    val yesterday = state.history.first { it.date == state.today.date.minusDays(1) }
    return if (yesterday.nextThemeBy == state.you.uid) "You" else state.them.name
}

/**
 * Tomorrow's theme picker. The chooser sees the themes; the other partner only
 * sees whether a pick was made, not which one.
 */
@Composable
internal fun TomorrowPick(
    state: CoupleState,
    themes: List<QuizTheme>,
    onPick: (String) -> Unit,
) {
    val day = state.today
    val yours = Turns.chooser(day.date, state.you.uid, state.them.uid) == state.you.uid
    val picked = day.nextTheme

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = when {
                yours && picked == null -> "Your turn to pick tomorrow's theme."
                // Not sealed, so not hidden: the pick is a gesture, shared as soon as it is made.
                yours -> "Tomorrow is ${picked!!.lowercase()}."
                picked == null -> "${state.them.name} picks tomorrow's theme."
                else -> "${state.them.name} picked ${picked.orEmpty().lowercase()} for tomorrow."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (yours && picked == null) {
            Spacer(Modifier.height(Space.m))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                themes.forEach { theme ->
                    BrickChip(
                        label = theme.label,
                        on = true,
                        color = theme.brick,
                        onClick = { onPick(theme.label) },
                    )
                }
            }
        }
    }
}
