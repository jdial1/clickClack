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

@Composable
internal fun AnswerForm(
    state: CoupleState,
    youColor: Color,
    themColor: Color,
    question: Question = state.today.question,
    onSubmit: (List<String>, List<String>?) -> Unit,
) {
    // Keyed on the question so switching questions clears both drafts.
    var mine by remember(question.id) { mutableStateOf(Draft.forQuestion(question)) }
    var theirs by remember(question.id) { mutableStateOf(Draft.forQuestion(question)) }

    val needsGuess = question.type != QuestionType.REFLECT
    val ready = mine.isComplete(question.type) &&
        (!needsGuess || theirs.isComplete(question.type))

    FieldLabel(youLabel(question.type), youColor)
    AnswerInput(question, mine, youColor, forPartner = false, state = state) { mine = it }

    if (needsGuess) {
        Spacer(Modifier.height(Space.xl))
        FieldLabel(themLabel(question.type, state.them.name), themColor)
        AnswerInput(question, theirs, themColor, forPartner = true, state = state) { theirs = it }
    }

    Spacer(Modifier.height(Space.xxl))

    AmpButton(
        label = "Submit",
        slug = BrickColor.of(state.you.colorArgb).slug,
        enabled = ready,
        // Silent on the click: the seal is cued by the panel landing instead.
        sfx = null,
        feelOnClick = Feel.PRESS,
        onClick = {
            onSubmit(
                mine.values(question.type),
                if (needsGuess) theirs.values(question.type) else null,
            )
        },
    )

    Spacer(Modifier.height(Space.m))
    Text(
        text = "Hidden until you both answer. You can't edit after.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

internal fun youLabel(type: QuestionType) = when (type) {
    QuestionType.RANK -> "Your order"
    QuestionType.MULTI_SELECT -> "Your picks"
    QuestionType.SCALE -> "Your answer"
    else -> "Your answer"
}

internal fun themLabel(type: QuestionType, name: String) = when (type) {
    QuestionType.RANK -> "Your guess at $name's order"
    QuestionType.MULTI_SELECT -> "What $name will pick"
    QuestionType.SCALE -> "What $name will say"
    else -> "What $name will say"
}

/** One in-progress answer for any question type; a single object so it resets atomically. */
internal data class Draft(
    val text: String = "",
    val pick: String? = null,
    val multi: Set<String> = emptySet(),
    val rank: List<String> = emptyList(),
    val scale: Int? = null,
) {
    fun values(question: Question): List<String> = values(question.type)

    fun isComplete(question: Question): Boolean = isComplete(question.type)

    fun values(type: QuestionType): List<String> = when (type) {
        QuestionType.REFLECT, QuestionType.GUESS_ME -> listOf(text.trim())
        QuestionType.MULTI_SELECT -> multi.toList()
        QuestionType.RANK -> rank
        QuestionType.SCALE -> listOfNotNull(scale?.toString())
        else -> listOfNotNull(pick)
    }

    fun isComplete(type: QuestionType): Boolean = when (type) {
        QuestionType.REFLECT, QuestionType.GUESS_ME -> text.isNotBlank()
        QuestionType.MULTI_SELECT -> multi.isNotEmpty()
        QuestionType.RANK -> rank.isNotEmpty()
        QuestionType.SCALE -> scale != null
        else -> pick != null
    }

    companion object {
        fun forQuestion(q: Question) = Draft(rank = q.options)
    }
}

/** Dispatches to the right control. The only place question type maps to UI. */
@Composable
internal fun AnswerInput(
    question: Question,
    draft: Draft,
    accent: Color,
    forPartner: Boolean,
    state: CoupleState,
    onChange: (Draft) -> Unit,
) {
    when (question.type) {
        QuestionType.REFLECT, QuestionType.GUESS_ME -> AmpField(
            value = draft.text,
            onValueChange = { onChange(draft.copy(text = it)) },
            // Accent is always a brick color; the field needs the BrickColor.
            color = BrickColor.of(accent.toArgbLong()),
            placeholder = if (forPartner) {
                "Your best guess"
            } else {
                "Your answer"
            },
        )

        QuestionType.THIS_OR_THAT, QuestionType.PICK_ONE -> AmpRadioGroup(
            options = question.options,
            selected = draft.pick,
            accent = accent,
            onSelect = { onChange(draft.copy(pick = it)) },
        )

        QuestionType.MULTI_SELECT -> AmpCheckboxGroup(
            options = question.options,
            selected = draft.multi,
            accent = accent,
            max = question.maxChoices,
            onToggle = { option ->
                val next = if (option in draft.multi) {
                    draft.multi - option
                } else {
                    draft.multi + option
                }
                onChange(draft.copy(multi = next))
            },
        )

        QuestionType.RANK -> AmpDragRank(
            items = draft.rank,
            accent = accent,
            onReorder = { onChange(draft.copy(rank = it)) },
        )

        QuestionType.SCALE -> AmpScale(
            value = draft.scale,
            accent = accent,
            lowLabel = question.lowLabel,
            highLabel = question.highLabel,
            onSelect = { onChange(draft.copy(scale = it)) },
        )

        QuestionType.DROPDOWN -> AmpDropdown(
            options = question.options,
            selected = draft.pick,
            accent = accent,
            placeholder = if (forPartner) "What ${state.them.name} picks" else "Choose",
            onSelect = { onChange(draft.copy(pick = it)) },
        )
    }
}

@Composable
internal fun FieldLabel(text: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrickDot(slug = BrickColor.of(accent.toArgbLong()).slug, studSize = Stud.pip)
        Spacer(Modifier.size(Space.s))
        SectionLabel(text)
    }
    Spacer(Modifier.height(Space.m))
}

// --- Previews ----------------------------------------------------------------

/**
 * Shared question input for the deck player. [rememberDraft] is keyed on the
 * question id, so moving to the next question clears the draft.
 */
@Composable
internal fun rememberDraft(question: Question): MutableState<Draft> =
    remember(question.id) { mutableStateOf(Draft.forQuestion(question)) }

@Composable
internal fun QuestionInput(
    question: Question,
    draft: MutableState<Draft>,
    color: BrickColor,
    forPartner: Boolean,
    state: CoupleState,
) {
    AnswerInput(
        question = question,
        draft = draft.value,
        accent = Color(color.argb),
        forPartner = forPartner,
        state = state,
    ) { draft.value = it }
}
