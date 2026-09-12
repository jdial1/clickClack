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
import androidx.compose.foundation.Canvas
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
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
import com.ampersand.model.label
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
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
import com.ampersand.ui.components.PersonDot
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.BrickPill
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.glassOn
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.quietSlug
import com.ampersand.ui.brick.glassOn
import com.ampersand.ui.brick.inkOnBricks
import com.ampersand.ui.theme.toArgbLong
import com.ampersand.ui.components.AnswerValues
import com.ampersand.ui.components.QuietNote
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.components.arrive
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.quiz.DeckList
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Radius
import com.ampersand.ui.theme.glow
import com.ampersand.ui.theme.Stud
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM")

@Composable
fun TodayScreen(
    state: CoupleState,
    onSubmit: (values: List<String>, guess: List<String>?) -> Unit,
    onOpenReveal: () -> Unit,
    modifier: Modifier = Modifier,
    decks: List<Quiz> = emptyList(),
    onOpenDeck: (String) -> Unit = {},
    onWrite: () -> Unit = {},
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
            StreakStuds(days = state.streak, you = BrickColor.of(state.you.colorArgb))
        }

        Spacer(Modifier.height(18.dp))
        QuestionHero(
            askedBefore = state.capsule?.date,
            question = question,
            you = BrickColor.of(state.you.colorArgb),
            them = BrickColor.of(state.them.colorArgb),
        )
        Spacer(Modifier.height(28.dp))

        // Whether *this* phone just did it, rather than whether it is done.
        // Coming back to an already-answered day should find the panel sitting
        // there, not watch it arrive again.
        var justSealed by remember { mutableStateOf(false) }

        if (yourAnswer == null) {
            AnswerForm(state, you, them) { values, guess ->
                justSealed = true
                onSubmit(values, guess)
            }
        } else {
            val sounds = LocalSounds.current
            val feel = LocalFeel.current
            Box(
                Modifier.arrive(
                    key = Unit,
                    rise = 10.dp,
                    enabled = justSealed,
                    // The seal is cued by the picture, not by the tap -- the
                    // button that made the noise has already gone by then, which
                    // is why the old one landed on nothing.
                    onLanded = {
                        if (justSealed) {
                            sounds.play(Sfx.SEAL)
                            feel.play(Feel.SEAL)
                        }
                    },
                ),
            ) {
                SealedPanel(state, you, them, onOpenReveal)
            }
        }

        Spacer(Modifier.height(32.dp))
        WriteOne(state, onWrite)

        if (decks.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            SectionLabel("Decks")
            Spacer(Modifier.height(4.dp))

            val waiting = state.decksAwaitingYou(decks.map { it.id })
            Text(
                text = if (waiting > 0) {
                    // The only line here that is asking for something, so it is
                    // the only one that gets to be specific.
                    "${state.them.name} has finished $waiting, waiting on you."
                } else {
                    "One question a day is the habit. These are for an evening."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DeckList(decks = decks, state = state, onOpen = onOpenDeck)
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
private fun QuestionHero(
    question: Question,
    you: BrickColor,
    them: BrickColor,
    /** Set when the two of you answered this exact question once before. */
    askedBefore: LocalDate? = null,
) {
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
            .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 18.dp)
            .glassOn(you.slug)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${question.type.label.uppercase()}  ·  ${question.category.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.85f),
            )
            DepthDots(question.depth, tint = ink)
        }

        // Said before the question rather than after it. Knowing this one is
        // coming back changes how you answer it, and finding out afterwards
        // would make the app feel like it had kept something from you.
        askedBefore?.let { then ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "You both answered this ${agoIn(then)}. Answer it again.",
                style = MaterialTheme.typography.labelSmall,
                color = ink,
            )
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

/**
 * The way in to writing your own question.
 *
 * Sits under the day rather than above it: today already has a question, and
 * offering to write one before you have answered the one in front of you is
 * offering a way out of it.
 *
 * When something is already queued it says so instead. Two people who both
 * write one on the same evening should be able to see that they did.
 */
@Composable
private fun WriteOne(state: CoupleState, onWrite: () -> Unit) {
    val you = BrickColor.of(state.you.colorArgb)
    val queued = state.wildcards
    val mine = queued.count { it.byUid == state.you.uid }
    val theirs = queued.size - mine

    SectionLabel("Your own")
    Spacer(Modifier.height(10.dp))
    AmpButton(
        label = if (queued.isEmpty()) "Write one for ${state.them.name}" else "Write another",
        slug = you.slug,
        onClick = onWrite,
    )

    if (queued.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        QuietNote(
            when {
                mine > 0 && theirs > 0 ->
                    "One of yours and one of ${state.them.name}'s are waiting."
                theirs > 0 ->
                    "${state.them.name} has written one. It is the next question."
                mine == 1 -> "Yours is waiting. It will be the next question."
                else -> "$mine of yours are waiting."
            },
        )
    }
}

/**
 * How long ago, roughly, and never in days.
 *
 * "One hundred and eighty-three days ago" is a number; "six months ago" is a
 * memory. The whole feature is about distance feeling like distance.
 */
internal fun agoIn(then: LocalDate, now: LocalDate = LocalDate.now()): String {
    val months = ChronoUnit.MONTHS.between(then, now)
    return when {
        months >= 24 -> "${months / 12} years ago"
        months >= 12 -> "a year ago"
        months == 1L -> "a month ago"
        months >= 2 -> "$months months ago"
        else -> "a few weeks ago"
    }
}

/** Small filled capsule, used for metadata that should not compete. */
@Composable
private fun Pill(text: String) {
    BrickPill(slug = quietSlug(), studSize = Stud.small, height = BrickHeight.LOW) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = inkOnBrick(quietSlug()),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/**
 * A week of one-stud bricks, lit as far as the streak has got.
 *
 * A pill reading "7 DAY STREAK" is a number about bricks printed on a brick.
 * Seven studs is the same fact as a thing you can see filling up, and it says
 * what a number cannot: how much of the week is left. Which is the part that
 * makes anybody open the app on a Thursday.
 *
 * Seven, and it wraps. A streak of ten lights all seven and reads as the week
 * it is in, because a strip that grows without bound is a strip that stops
 * fitting on the row -- and the count is still there beside it for anyone who
 * wants the whole number.
 */
@Composable
private fun StreakStuds(days: Int, you: BrickColor) {
    val lit = if (days <= 0) 0 else ((days - 1) % StreakWeek) + 1
    val shape = BrickShape(1, BrickHeight.LOW)
    val quiet = quietSlug()
    val atlas = rememberBricks(
        remember(you, quiet) {
            listOf(BrickArt.assetFor(you.slug, shape), BrickArt.assetFor(quiet, shape))
        },
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (days > 0) {
            Text(
                text = "$days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        // Pip, not small: these are markers, which is the size that word means
        // in this app. Seven at 24dp is 168 of them beside a date, and the
        // streak starts arguing with the day it is counting.
        Canvas(
            Modifier.size(
                width = Stud.pip * StreakWeek,
                height = Stud.pip * (shape.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO),
            ),
        ) {
            val studPx = size.width / StreakWeek
            repeat(StreakWeek) { i ->
                drawBrickArt(
                    atlas = atlas,
                    asset = BrickArt.assetFor(if (i < lit) you.slug else quiet, shape),
                    studs = 1,
                    rows = shape.h,
                    left = i * studPx,
                    bottom = size.height,
                    studPx = studPx,
                    // Unlit days are there to be counted, not read. Full
                    // opacity on seven grey studs makes a row that competes
                    // with the question underneath it.
                    alpha = if (i < lit) 1f else 0.32f,
                )
            }
        }
    }
}

/** Seven, because a streak is measured in days and shown as a week. */
private const val StreakWeek = 7

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


@Composable
private fun FieldLabel(text: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrickDot(slug = BrickColor.of(accent.toArgbLong()).slug, studSize = Stud.pip)
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


