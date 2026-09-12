package com.ampersand.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.data.MockQuestions
import com.ampersand.model.Answer
import com.ampersand.model.Quiz
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import com.ampersand.model.CoupleState
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.brickFace
import com.ampersand.ui.components.AmpCheckboxGroup
import com.ampersand.ui.components.AmpDragRank
import com.ampersand.ui.components.AmpDropdown
import com.ampersand.ui.components.AmpField
import com.ampersand.ui.components.AmpRadioGroup
import com.ampersand.ui.components.AmpScale
import com.ampersand.ui.components.DepthDots
import com.ampersand.ui.components.PersonDot
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.inkOnBricks
import com.ampersand.ui.theme.toArgbLong
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Radius
import com.ampersand.ui.theme.glow
import java.time.format.DateTimeFormatter

private val DayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM")

@Composable
fun TodayScreen(
    state: CoupleState,
    onSubmit: (values: List<String>, guess: List<String>?) -> Unit,
    onOpenReveal: () -> Unit,
    modifier: Modifier = Modifier,
    decks: List<Quiz> = emptyList(),
    onOpenDeck: (String) -> Unit = {},
) {
    val you = Color(state.you.colorArgb)
    val them = Color(state.them.colorArgb)
    val day = state.today
    val question = day.question
    val yourAnswer = day.answerFor(state.you.uid)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(day.date.format(DayFormat))
            Pill("${state.streak} day streak")
        }

        Spacer(Modifier.height(18.dp))
        QuestionHero(
            question = question,
            you = BrickColor.of(state.you.colorArgb),
            them = BrickColor.of(state.them.colorArgb),
        )
        Spacer(Modifier.height(28.dp))

        if (yourAnswer == null) {
            AnswerForm(state, you, them, onSubmit)
        } else {
            SealedPanel(state, you, them, onOpenReveal)
        }

        if (decks.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            SectionLabel("Decks")
            Spacer(Modifier.height(4.dp))
            Text(
                "One question a day is the habit. These are for when you have an evening.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            decks.forEach { deck ->
                DeckRow(deck, state, onOpenDeck)
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

/**
 * The question, on the gradient between the two of you.
 *
 * This is the one surface that says what the app is about without words: your
 * colour on one edge, theirs on the other, and the question sitting in the
 * blend. Every couple gets a different hero.
 */
@Composable
private fun QuestionHero(question: Question, you: BrickColor, them: BrickColor) {
    // One line of text crosses both halves of the wall, so the ink has to suit
    // the paler of the two. White over pack yellow is unreadable.
    val ink = inkOnBricks(you.slug, them.slug)

    // The hero is a wall built from both of you, not a gradient panel. The
    // blend idea survives -- it is just said in brick rather than in colour
    // interpolation, which is the only version of it the pack can actually make.
    BrickSlab(
        slugs = listOf(you.slug, them.slug),
        studs = 12,
        height = BrickHeight.MEDIUM,
        modifier = Modifier.fillMaxWidth(),
    ) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 26.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = question.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.85f),
            )
            DepthDots(question.depth, tint = ink)
        }
        Spacer(Modifier.height(14.dp))
        SelectionContainer {
            Text(
                text = question.text,
                style = MaterialTheme.typography.displaySmall,
                color = ink,
            )
        }
    }
    }
}

/** Small filled capsule, used for metadata that should not compete. */
@Composable
private fun Pill(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .brickFace(
                MaterialTheme.colorScheme.surfaceVariant,
                corner = 12.dp,
                studded = false,
                lip = 4.dp,
            )
            .padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 10.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- Draft -------------------------------------------------------------------

/**
 * One in-progress answer, covering every question type.
 *
 * A single object rather than eight loose `remember`s, so resetting on a
 * question change is one line and cannot half-happen.
 */
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

@Composable
private fun AnswerForm(
    state: CoupleState,
    youColor: Color,
    themColor: Color,
    onSubmit: (List<String>, List<String>?) -> Unit,
) {
    val question = state.today.question
    // Keyed on the question so switching questions clears both drafts.
    var mine by remember(question.id) { mutableStateOf(Draft.forQuestion(question)) }
    var theirs by remember(question.id) { mutableStateOf(Draft.forQuestion(question)) }

    val needsGuess = question.type != QuestionType.REFLECT
    val ready = mine.isComplete(question.type) &&
        (!needsGuess || theirs.isComplete(question.type))

    FieldLabel(youLabel(question.type), youColor)
    AnswerInput(question, mine, youColor, forPartner = false, state = state) { mine = it }

    if (needsGuess) {
        Spacer(Modifier.height(24.dp))
        FieldLabel(themLabel(question.type, state.them.name), themColor)
        AnswerInput(question, theirs, themColor, forPartner = true, state = state) { theirs = it }
    }

    Spacer(Modifier.height(28.dp))

    AmpButton(
        label = "Seal it",
        slug = BrickColor.of(state.you.colorArgb).slug,
        enabled = ready,
        sfx = Sfx.SEAL,
        onClick = {
            onSubmit(
                mine.values(question.type),
                if (needsGuess) theirs.values(question.type) else null,
            )
        },
    )

    Spacer(Modifier.height(12.dp))
    Text(
        text = "Sealed until you have both answered. No edits after.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

private fun youLabel(type: QuestionType) = when (type) {
    QuestionType.RANK -> "Your order"
    QuestionType.MULTI_SELECT -> "Your picks"
    QuestionType.SCALE -> "Your answer"
    else -> "Your answer"
}

private fun themLabel(type: QuestionType, name: String) = when (type) {
    QuestionType.RANK -> "$name's order, as you predict it"
    QuestionType.MULTI_SELECT -> "What $name will pick"
    QuestionType.SCALE -> "What $name will say"
    else -> "What $name will say"
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
            accent = accent,
            placeholder = if (forPartner) {
                "Your best guess."
            } else {
                "Honestly, though."
            },
            minLines = if (question.type == QuestionType.REFLECT) 4 else 2,
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

/** After you submit: your answer stays visible, theirs does not exist yet. */
@Composable
private fun SealedPanel(
    state: CoupleState,
    youColor: Color,
    themColor: Color,
    onOpenReveal: () -> Unit,
) {
    val day = state.today
    val yours = day.answerFor(state.you.uid)!!
    val bothIn = day.isRevealed

    Column(
        Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonDot(state.you.name, youColor, size = 28)
            Spacer(Modifier.size(10.dp))
            SectionLabel("You said")
        }
        Spacer(Modifier.height(10.dp))
        AnswerValues(yours.values, day.question.type)

        yours.guessText?.let { guess ->
            Spacer(Modifier.height(14.dp))
            SectionLabel("You guessed ${state.them.name} would say")
            Spacer(Modifier.height(6.dp))
            Text(
                guess,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    AnimatedVisibility(visible = !bothIn, enter = fadeIn(), exit = fadeOut()) {
        WaitingLine(state.them.name)
    }

    AnimatedVisibility(visible = bothIn, enter = fadeIn(), exit = fadeOut()) {
        AmpButton(
            label = "Open it",
            slug = BrickColor.of(state.you.colorArgb).slug,
            sfx = Sfx.SEAL,
            onClick = onOpenReveal,
        )
    }
}

/**
 * The waiting state, said with the app's own name.
 *
 * Half the name is on screen and the other half has not happened yet: your
 * answer made the click, and the clack is theirs to make. It is the one place
 * where the title of the app is also the state the app is in, which is the
 * entire reason it is called this.
 *
 * "Clack" breathes rather than spinning. Nothing is loading -- someone is
 * thinking -- and a spinner would promise that waiting is a technical problem
 * about to resolve itself.
 */
@Composable
private fun WaitingLine(theirName: String) {
    val pending by rememberInfiniteTransition(label = "clack").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "clackFade",
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(Radius.control)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Click.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "Clack.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = pending),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "$theirName has not answered yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A ranked answer is numbered; everything else is one line. */
@Composable
internal fun AnswerValues(values: List<String>, type: QuestionType) {
    if (type == QuestionType.RANK) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            values.forEachIndexed { i, value ->
                Text(
                    "${i + 1}.  $value",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    } else {
        Text(
            values.joinToString(", "),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun FieldLabel(text: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrickDot(slug = BrickColor.of(accent.toArgbLong()).slug, studSize = 14.dp)
        Spacer(Modifier.size(8.dp))
        SectionLabel(text)
    }
    Spacer(Modifier.height(10.dp))
}

// --- Previews ----------------------------------------------------------------

@Composable
private fun previewOf(questionId: String): CoupleState {
    val base = MockCouple.state()
    return base.copy(
        today = base.today.copy(question = MockQuestions.byId(questionId), answers = emptyMap()),
    )
}

@Preview(name = "Today - Rank (drag)", showBackground = true, heightDp = 1100)
@Composable
private fun PreviewRank() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        TodayScreen(previewOf("k1"), { _, _ -> }, {})
    }
}

@Preview(name = "Today - Multi select", showBackground = true, heightDp = 1100)
@Composable
private fun PreviewMulti() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        TodayScreen(previewOf("m1"), { _, _ -> }, {})
    }
}

@Preview(name = "Today - Pick one", showBackground = true, heightDp = 900)
@Composable
private fun PreviewPickOne() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        TodayScreen(previewOf("p1"), { _, _ -> }, {})
    }
}

@Preview(name = "Today - Scale", showBackground = true, heightDp = 800)
@Composable
private fun PreviewScale() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        TodayScreen(previewOf("s1"), { _, _ -> }, {})
    }
}

@Preview(name = "Today - Dropdown", showBackground = true, heightDp = 800)
@Composable
private fun PreviewDropdown() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        TodayScreen(previewOf("d1"), { _, _ -> }, {})
    }
}

@Preview(name = "Today - Guess Me", showBackground = true, heightDp = 800)
@Composable
private fun PreviewGuessMe() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        TodayScreen(previewOf("g1"), { _, _ -> }, {})
    }
}

@Preview(name = "Today - Sealed, waiting", showBackground = true, heightDp = 800)
@Composable
private fun PreviewSealed() = AmpersandTheme {
    val base = previewOf("g1")
    val sealed = base.copy(
        today = base.today.copy(
            answers = mapOf(
                base.you.uid to Answer.of(
                    base.you.uid,
                    "The shoebox of photos under the bed.",
                    "Your laptop, obviously.",
                ),
            ),
        ),
    )
    Surface(color = MaterialTheme.colorScheme.background) {
        TodayScreen(sealed, { _, _ -> }, {})
    }
}

@Preview(name = "Today - Rank, dark", showBackground = true, heightDp = 1100)
@Composable
private fun PreviewRankDark() = AmpersandTheme(darkTheme = true) {
    Surface(color = MaterialTheme.colorScheme.background) {
        TodayScreen(previewOf("k1"), { _, _ -> }, {})
    }
}

/**
 * One question's input, with the draft held for you.
 *
 * The deck player needs exactly what the daily question needs -- the same eight
 * controls, chosen the same way -- so this is the shared entry point rather
 * than a second copy that drifts. [rememberDraft] is keyed on the question, so
 * moving to the next one genuinely clears the form instead of carrying the last
 * answer forward as a suggestion.
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


/**
 * One deck, with where the two of you are in it.
 *
 * The status is the whole point of the row. "Played" is not a state worth
 * showing on its own -- what you want to know before tapping is whether this is
 * a fresh one, one you are waiting on them for, or one with a result sitting in
 * it.
 */
@Composable
private fun DeckRow(deck: Quiz, state: CoupleState, onOpen: (String) -> Unit) {
    val mine = state.myRun(deck.id)
    val theirs = state.theirRun(deck.id)
    val status = when {
        mine != null && theirs != null -> "Open the result"
        mine != null -> "Waiting on ${state.them.name}"
        theirs != null -> "${state.them.name} has played this"
        else -> "${deck.size} questions  ·  about ${deck.minutes} min"
    }

    BrickSlab(
        slugs = listOf(deck.theme.brick.slug),
        studs = 12,
        height = BrickHeight.MEDIUM,
        alpha = if (mine != null && theirs == null) 0.55f else 1f,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(deck.id) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(deck.theme.glyph, fontSize = 20.sp)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = deck.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = inkOnBrick(deck.theme.brick.slug),
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = inkOnBrick(deck.theme.brick.slug).copy(alpha = 0.85f),
                )
            }
        }
    }
}
