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

/**
 * Today's moods. Tapping yours opens a picker (face color row, faces, other bricks);
 * picking sets it and closes. Theirs is hidden when unset.
 */
@Composable
internal fun Moods(state: CoupleState, onSet: (Mood?, BrickColor?) -> Unit, onReply: (String) -> Unit) {
    val today = state.today.date
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)
    val mine = state.you.mood?.takeIf { it.current(today) != null }
    val theirs = state.them.mood?.takeIf { it.current(today) != null }
    var picking by rememberSaveable { mutableStateOf(false) }
    var replying by rememberSaveable { mutableStateOf(false) }
    val theirReply = state.today.moodReplies[state.them.uid]
    val myReply = state.today.moodReplies[state.you.uid]
    // Faces keep their own pack colours.
    val faceColor: BrickColor? = null
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    fun pick(mood: Mood) {
        sounds.play(Sfx.PLACE)
        feel.play(Feel.PLACE)
        val color = faceColor.takeIf { mood.kind == Mood.Kind.FACE }
        onSet(if (mood == mine?.mood && color == mine.color) null else mood, color)
        picking = false
    }

    // One strip: the weather on the left, both moods on the right, replies underneath.
    Column(
        Modifier
            .fillMaxWidth()
            .panel()
            .padding(horizontal = Space.m, vertical = Space.s),
    ) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            state.weather?.let { weather ->
                weather.brick?.let { WeatherBrickArt(it, Modifier.size(IconSize.large)) }
                Spacer(Modifier.width(Space.s))
                Text("${weather.tempF}°", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        MoodChip(who = "You", entry = mine, color = you, empty = "Set mood") { picking = !picking }
        theirs?.let {
            Spacer(Modifier.width(Space.m))
            // Tap theirs to reply.
            MoodChip(who = state.them.name, entry = it, color = them, empty = "") { replying = !replying; picking = false }
        }
    }
    // One reply each, shown once both of you have set a mood.
    theirReply?.takeIf { mine != null }?.let {
        Text("${state.them.name}: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
    }
    myReply?.takeIf { theirs != null }?.let {
        Text("You sent: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
    }
    }
    AnimatedVisibility(visible = replying && theirs != null) {
        Column(Modifier.fillMaxWidth().padding(top = Space.m)) {
            var note by rememberSaveable { mutableStateOf("") }
            fun send(text: String) {
                sounds.play(Sfx.PLACE)
                feel.play(Feel.PLACE)
                onReply(text)
                note = ""
                replying = false
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                MoodReplies.forEach { r -> BrickChip(label = r, on = r == myReply, color = null, onClick = { send(r) }) }
            }
            Spacer(Modifier.height(Space.m))
            AmpField(value = note, onValueChange = { note = it.take(80) }, color = you, placeholder = "Or a short note")
            Spacer(Modifier.height(Space.s))
            AmpButton(label = "Send", slug = you.slug, enabled = note.isNotBlank(), icon = UiIcon.DONE) { send(note.trim()) }
        }
    }
    AnimatedVisibility(visible = picking) {
        Column(Modifier.fillMaxWidth().padding(top = Space.m)) {
            MoodTiles(Mood.entries.filter { it.kind == Mood.Kind.FACE }, you, faceColor, ::pick)
            Spacer(Modifier.height(Space.s))
            MoodTiles(Mood.entries.filter { it.kind != Mood.Kind.FACE }, you, null, ::pick)
        }
    }
}

@Composable
internal fun MoodTiles(moods: List<Mood>, you: BrickColor, faceColor: BrickColor?, onPick: (Mood) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        moods.forEach { mood ->
            Column(
                Modifier
                    .width(84.dp)
                    .panel()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(mood) }
                    .padding(Space.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MoodArt(mood, you, faceColor = faceColor)
                Spacer(Modifier.height(Space.xs))
                Text(
                    mood.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun MoodChip(
    who: String,
    entry: MoodEntry?,
    color: BrickColor,
    empty: String,
    onClick: (() -> Unit)?,
) {
    Column(
        Modifier.then(
            if (onClick == null) Modifier
            else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (entry != null) {
            MoodArt(entry.mood, color, size = IconSize.large, faceColor = entry.color)
            Text("$who: ${entry.mood.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
        } else {
            Text(empty, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

/** Gold used to mark a special day. */


/** One-tap replies to your partner's mood. */
internal val MoodReplies = listOf("Thinking of you", "Here if you need me", "Proud of you", "Sending a hug", "Call you later")
