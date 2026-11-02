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

/** What makes today special: your dates and birthdays (with years), then a holiday. */
internal fun specialToday(state: CoupleState): List<Pair<String, String>> {
    val today = state.today.date
    val dates = com.ampersand.model.Dates.upcoming(state).filter { it.next == today }.map { d ->
        val years = today.year - d.since.year
        d.label to when {
            d.label.endsWith("half-birthday") -> "$years and a half"
            years > 0 -> "$years ${if (years == 1) "year" else "years"}"
            else -> "Today"
        }
    }
    val holiday = com.ampersand.model.Occasion.on(
        today, state.anniversary, state.homeState.takeIf { com.ampersand.model.QuestionSource.HOLIDAYS !in state.sourcesOff },
    )?.takeIf { o -> dates.none { it.first.contains("anniversary", ignoreCase = true) } || !o.id.contains("anniversary") }
        ?.let { it.label to "Happy ${it.label}" }
    // A milestone replaces the plain anniversary line it falls on.
    val together = com.ampersand.model.Together.on(today, state.anniversary, state.marriedOn)
    val shown = if (together == null) dates else dates.filterNot { it.first.contains("anniversary", ignoreCase = true) }
    return listOfNotNull(together?.let { it.title to it.detail }) + shown + listOfNotNull(holiday)
}

/** A gold-bordered card naming today's special days. */
@Composable
internal fun SpecialDayCard(state: CoupleState, events: List<Pair<String, String>>, onAddPhoto: (String, String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = state.today.date
    val photo = state.photos.firstOrNull { it.date == today }
    val label = events.first().first
    // Asked once a day; "Not now" hides the offer until tomorrow.
    var declined by rememberSaveable(today) { mutableStateOf(false) }
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { compressPhoto(context, it) }?.let { onAddPhoto(label, it) } }

    Column(
        Modifier
            .fillMaxWidth()
            .panel(Emphasis.RAISED)
            .border(2.dp, SpecialGold, Radius.medium)
            .padding(horizontal = Space.m, vertical = Space.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBrick(UiIcon.DATES, size = IconSize.large)
            Spacer(Modifier.width(Space.m))
            Column {
                events.forEach { (title, detail) ->
                    Text("Today: $title", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        if (photo != null) {
            rememberPhoto(photo.data)?.let {
                Spacer(Modifier.height(Space.m))
                androidx.compose.foundation.Image(
                    it, contentDescription = photo.label,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(Radius.medium),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        } else if (!declined) {
            Spacer(Modifier.height(Space.s))
            Text("Add a photo to remember today?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.l)) {
                IconAction(UiIcon.ADD, "Add a photo", onClick = {
                    pick.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
                IconAction(UiIcon.CLOSE, "Not now", onClick = { declined = true })
            }
        }
    }
}

/** Moments as flat rows: a missed day to answer, and one thing from before. */
@Composable
internal fun MomentsList(
    state: CoupleState,
    moments: List<Moment>,
    onOpenDay: (LocalDate) -> Unit,
    onAnswerDay: (LocalDate) -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    moments.forEach { m ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = Space.s)
                .panel()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    sounds.play(Sfx.NAV)
                    feel.play(Feel.NAV)
                    if (m.kind == Moment.Kind.MISSED) onAnswerDay(m.date) else onOpenDay(m.date)
                }
                .padding(horizontal = Space.m, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBrick(
                when (m.kind) {
                    Moment.Kind.MISSED -> UiIcon.WRITE
                    Moment.Kind.KEPT -> UiIcon.KEEP
                    Moment.Kind.ON_THIS_DAY -> UiIcon.DATES
                    else -> UiIcon.DETAILS
                },
                size = IconSize.medium,
            )
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(m.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(m.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 2)
                // In full, not a link: reading it by accident is the whole value.
                m.day?.let { day ->
                    listOf(state.you.uid to "You", state.them.uid to state.them.name).forEach { (uid, who) ->
                        day.answerFor(uid)?.let {
                            Spacer(Modifier.height(Space.xs))
                            Text("$who: ${it.response}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }
    }
}
