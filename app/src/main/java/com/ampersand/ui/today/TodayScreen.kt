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

internal val DayFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d")

@Composable
fun TodayScreen(
    state: CoupleState,
    onSubmit: (values: List<String>, guess: List<String>?) -> Unit,
    onOpenReveal: () -> Unit,
    modifier: Modifier = Modifier,
    onSetMood: (Mood?, BrickColor?) -> Unit = { _, _ -> },
    onReplyToMood: (String) -> Unit = {},
    onAddPhoto: (label: String, jpegBase64: String) -> Unit = { _, _ -> },
    onOpenDay: (LocalDate) -> Unit = {},
    onAnswerDay: (LocalDate) -> Unit = {},
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
            .padding(horizontal = Inset.screen),
    ) {
        Spacer(Modifier.height(Space.m))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(day.date.format(DayFormat))
            // Days you both answered this week, not a run: a gap never breaks anything.
            StreakStuds(
                days = remember(state.history, state.today, state.plate.startedOn) {
                    (state.history + state.today).count { it.date >= state.plate.startedOn && it.isRevealed }
                },
                you = BrickColor.of(state.you.colorArgb),
            )
        }

        val author = when (state.todayAuthorUid) {
            null -> null
            state.you.uid -> state.you
            else -> state.them
        }

        // The question leads. Nothing else on Today asks for attention above it.
        Spacer(Modifier.height(Space.m))
        QuestionHero(
            why = when {
                state.capsule?.question?.id == question.id -> "Again"
                author != null -> if (author.uid == state.you.uid) "You wrote this" else "From ${author.name}"
                else -> pickedBy(state)?.let { if (it == "You") "Your pick" else "$it's pick" } ?: state.why
            },
            author = author?.let { if (it.uid == state.you.uid) "You wrote this for ${state.them.name}" else "${it.name} wrote you this" },
            authorColor = author?.let { BrickColor.of(it.colorArgb) },
            question = question,
            you = BrickColor.of(state.you.colorArgb),
            them = BrickColor.of(state.them.colorArgb),
            status = when {
                yourAnswer == null -> HeroStatus(
                    "Your turn",
                    "${state.them.name} sees your answer once they answer too.",
                )
                !day.isRevealed -> HeroStatus(
                    title = "Click.",
                    detail = "${state.them.name} has not answered yet. " + answerLine(yourAnswer),
                    waiting = true,
                )
                else -> HeroStatus("Click. Clack.", "Open it. " + answerLine(yourAnswer), onOpenReveal)
            },
        )
        // Your answer lives on the card once it's in, so the form is the only thing below it.
        if (yourAnswer == null) {
            val sounds = LocalSounds.current
            val feel = LocalFeel.current
            Spacer(Modifier.height(Space.xl))
            AnswerForm(state, you, them) { values, guess ->
                // The name doing its job: first in is the click, the answer that opens the day is the clack.
                sounds.play(if (state.them.uid in day.submitted) Sfx.CLACK else Sfx.CLICK)
                feel.play(Feel.SEAL)
                onSubmit(values, guess)
            }
        }

        // Until you have answered, the question has the screen. The rest is one tap away.
        var moreOpen by rememberSaveable { mutableStateOf(false) }
        if (yourAnswer != null || moreOpen) {
            Spacer(Modifier.height(Space.xl))
            Moods(state, onSetMood, onReplyToMood)

            val events = remember(state) { specialToday(state) }
            if (events.isNotEmpty()) {
                Spacer(Modifier.height(Space.m))
                SpecialDayCard(state, events, onAddPhoto)
            }

            val moments = remember(state.history, state.kept, state.today.date) { Moments.of(state) }
            if (moments.isNotEmpty()) {
                Spacer(Modifier.height(Space.xl))
                MomentsList(state, moments, onOpenDay, onAnswerDay)
            }
        } else {
            Spacer(Modifier.height(Space.m))
            QuietAction("More today", onClick = { moreOpen = true })
        }


        Spacer(Modifier.height(Space.page))
    }
}

/** Today's question card, on a brick slab in both partners' colors. */
@Composable
internal fun QuestionHero(
    question: Question,
    you: BrickColor,
    them: BrickColor,
    /** Why this question today, shown as a tag: "Weather", "First date", "Tasia's pick". */
    why: String? = null,
    /** "Tasia wrote you this", when one of you wrote today's question. */
    author: String? = null,
    authorColor: BrickColor? = null,
    /** Whose move it is, said on the card itself. */
    status: HeroStatus? = null,
) {
    // Text spans both colors, so pick ink that reads on the paler one (white on yellow fails).
    val ink = inkOnBricks(you.slug, them.slug)

    BrickSlab(
        slugs = listOf(you.slug, them.slug),
        studs = 12,
        height = BrickHeight.MEDIUM,
        modifier = Modifier
            .fillMaxWidth(),
    ) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = Space.l, end = Space.l, top = Space.xl, bottom = Space.l)
            // Glass must match the ink, or a yellow partner gets dark text on a dark pane.
            .glass(ink)
            .padding(horizontal = Space.l, vertical = Space.l),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = question.type.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = Alpha.secondary),
                )
                Spacer(Modifier.width(Space.s))
                DepthDots(question.depth, tint = ink)
            }
            why?.let {
                Row(
                    Modifier
                        .glass(ink, corner = Corner.medium, strength = Emphasis.RAISED.strength)
                        .padding(horizontal = Space.s, vertical = Space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuizTheme.entries.firstOrNull { t -> t.label == question.category }?.let { t ->
                        ThemeIcon(t, size = IconSize.medium)
                        Spacer(Modifier.width(Space.s))
                    }
                    Text(text = it, style = MaterialTheme.typography.labelLarge, color = ink)
                }
            }
        }

        // A written question arrives as a card from its author.
        if (author != null && authorColor != null) {
            Spacer(Modifier.height(Space.m))
            BrickPill(slug = authorColor.slug, studSize = Stud.chip, height = BrickHeight.MEDIUM) {
                Row(Modifier.padding(horizontal = Space.m, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                    IconBrick(UiIcon.WRITE, size = IconSize.medium)
                    Spacer(Modifier.width(Space.s))
                    Text(author, style = MaterialTheme.typography.titleSmall, color = inkOnBrick(authorColor.slug))
                }
            }
        }

        Spacer(Modifier.height(Space.m))
        SelectionContainer {
            Text(
                text = question.text,
                style = MaterialTheme.typography.displaySmall,
                color = ink,
            )
        }

        // Day status; tappable only when it has an onClick.
        status?.let { st ->
            val sounds = LocalSounds.current
            val feel = LocalFeel.current
            Spacer(Modifier.height(Space.l))
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(ink, corner = Corner.medium, strength = Emphasis.RAISED.strength)
                    .then(
                        if (st.onClick == null) Modifier else Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            sounds.play(Sfx.SEAL)
                            feel.play(Feel.SEAL)
                            st.onClick()
                        },
                    )
                    .padding(horizontal = Space.m, vertical = Space.m),
            ) {
                if (st.waiting) {
                    ClickThenClack(ink)
                } else {
                    Text(st.title, style = MaterialTheme.typography.titleMedium, color = ink)
                }
                Text(st.detail, style = MaterialTheme.typography.bodySmall, color = ink)
            }
        }
    }
    }
}

/** Rough "how long ago" in months or years, never days. */
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

/**
 * Seven one-stud bricks, one lit for each day this week you both answered.
 * No number and no milestone: a count that gets praised becomes one people perform for.
 */
@Composable
internal fun StreakStuds(days: Int, you: BrickColor) {
    val lit = days.coerceIn(0, StreakWeek)
    val shape = BrickShape(1, BrickHeight.LOW)
    val quiet = quietSlug()
    val atlas = rememberBricks(
        remember(you, quiet) {
            listOf(BrickArt.assetFor(you.slug, shape), BrickArt.assetFor(quiet, shape))
        },
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Pip size keeps the row small next to the date.
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
                    // Dim unlit days so the row doesn't compete with the question.
                    alpha = if (i < lit) 1f else Alpha.unlit,
                )
            }
        }
    }
}

/** Studs in the streak row. */
internal const val StreakWeek = 7

// --- Draft -------------------------------------------------------------------

/** Your answer and guess as one line for the question card. */
internal fun answerLine(answer: com.ampersand.model.Answer?): String = listOfNotNull(
    answer?.values?.takeIf { it.isNotEmpty() }?.let { "You said: ${it.joinToString(", ")}." },
    answer?.guessText?.let { "You guessed: $it." },
).joinToString(" ")

/** Answer a past day you missed. */
@Composable
fun PastAnswerScreen(
    state: CoupleState,
    day: DayEntry,
    onBack: () -> Unit,
    onSubmit: (List<String>, List<String>?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Inset.screen),
    ) {
        Spacer(Modifier.height(Space.m))
        BackAction(onBack)
        Spacer(Modifier.height(Space.s))
        SectionLabel(day.date.format(DayFormat))
        Spacer(Modifier.height(Space.m))
        Text(day.question.text, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Space.xl))
        AnswerForm(state, Color(state.you.colorArgb), Color(state.them.colorArgb), question = day.question, onSubmit = onSubmit)
        Spacer(Modifier.height(Space.page))
    }
}

/** What the question card says about the day, and where tapping it goes. */
internal data class HeroStatus(
    val title: String,
    val detail: String,
    val onClick: (() -> Unit)? = null,
    /** You are in and they are not: the title becomes the name, half made. */
    val waiting: Boolean = false,
)

/** The name doing its job: yours is made, theirs is still breathing. */
@Composable
internal fun ClickThenClack(ink: Color) {
    val breath by rememberInfiniteTransition(label = "clack").animateFloat(
        initialValue = com.ampersand.ui.theme.Alpha.unlit,
        targetValue = com.ampersand.ui.theme.Alpha.quiet,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "clack",
    )
    Row {
        Text("Click. ", style = MaterialTheme.typography.titleMedium, color = ink)
        Text("Clack.", style = MaterialTheme.typography.titleMedium, color = ink.copy(alpha = breath))
    }
}


@Composable
internal fun previewOf(questionId: String): CoupleState {
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
