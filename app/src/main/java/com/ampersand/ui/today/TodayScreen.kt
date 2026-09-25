package com.ampersand.ui.today

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
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Stud
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DayFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d")

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
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))

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
        Spacer(Modifier.height(12.dp))
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
                !day.isRevealed -> HeroStatus("Waiting for ${state.them.name}", answerLine(yourAnswer))
                else -> HeroStatus("You both answered", "Tap to see answers. " + answerLine(yourAnswer), onOpenReveal)
            },
        )
        // Your answer lives on the card once it's in, so the form is the only thing below it.
        if (yourAnswer == null) {
            val sounds = LocalSounds.current
            val feel = LocalFeel.current
            Spacer(Modifier.height(24.dp))
            AnswerForm(state, you, them) { values, guess ->
                // The name doing its job: first in is the click, the answer that opens the day is the clack.
                sounds.play(if (state.them.uid in day.submitted) Sfx.CLACK else Sfx.CLICK)
                feel.play(Feel.SEAL)
                onSubmit(values, guess)
            }
        }

        Spacer(Modifier.height(20.dp))
        Moods(state, onSetMood, onReplyToMood)

        val events = remember(state) { specialToday(state) }
        if (events.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SpecialDayCard(state, events, onAddPhoto)
        }

        val moments = remember(state.history, state.kept, state.today.date) { Moments.of(state) }
        if (moments.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            MomentsList(state, moments, onOpenDay, onAnswerDay)
        }


        Spacer(Modifier.height(48.dp))
    }
}

/** Today's question card, on a brick slab in both partners' colors. */
@Composable
private fun QuestionHero(
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
            .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 18.dp)
            // Glass must match the ink, or a yellow partner gets dark text on a dark pane.
            .glass(ink)
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
                    color = ink.copy(alpha = 0.85f),
                )
                Spacer(Modifier.width(8.dp))
                DepthDots(question.depth, tint = ink)
            }
            why?.let {
                Row(
                    Modifier
                        .glass(ink, corner = 10.dp, strength = 1.6f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuizTheme.entries.firstOrNull { t -> t.label == question.category }?.let { t ->
                        ThemeIcon(t, size = 22.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(text = it, style = MaterialTheme.typography.labelMedium, color = ink)
                }
            }
        }

        // A written question arrives as a card from its author.
        if (author != null && authorColor != null) {
            Spacer(Modifier.height(12.dp))
            BrickPill(slug = authorColor.slug, studSize = Stud.chip, height = BrickHeight.MEDIUM) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBrick(UiIcon.WRITE, size = 22.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(author, style = MaterialTheme.typography.titleSmall, color = inkOnBrick(authorColor.slug))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
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
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(ink, corner = 12.dp, strength = 1.6f)
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(st.title, style = MaterialTheme.typography.titleMedium, color = ink)
                Text(st.detail, style = MaterialTheme.typography.bodySmall, color = ink)
            }
        }
    }
    }
}

/** What you are asked next: tomorrow's theme once today is open, and a question of your own. */
@Composable
internal fun ShapeTomorrow(
    state: CoupleState,
    themes: List<QuizTheme>,
    onPick: (String) -> Unit,
    onWrite: () -> Unit,
) {
    SectionLabel("Shape tomorrow")
    Spacer(Modifier.height(10.dp))
    if (state.today.isRevealed && themes.isNotEmpty()) {
        TomorrowPick(state, themes, onPick)
        Spacer(Modifier.height(14.dp))
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
        Spacer(Modifier.height(8.dp))
        QuietNote("It arrives as a card with your name on it, and ${state.them.name} gets a notification.")
    }

    if (queued.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
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

private fun count(n: Int) = if (n == 1) "One" else n.toString()

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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .clickable(interactionSource = interaction, indication = null) {
                sounds.play(Sfx.SEAL)
                feel.play(Feel.SEAL)
                onTalkedAbout(yesterday.date)
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrickDot(slug = you.slug, studSize = Stud.pip)
        Column(Modifier.weight(1f)) {
            Text(
                "Did you talk about this one?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
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
private fun pickedBy(state: CoupleState): String? {
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
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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

/**
 * Seven one-stud bricks, one lit for each day this week you both answered.
 * No number and no milestone: a count that gets praised becomes one people perform for.
 */
@Composable
private fun StreakStuds(days: Int, you: BrickColor) {
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
                    alpha = if (i < lit) 1f else 0.32f,
                )
            }
        }
    }
}

/** Studs in the streak row. */
private const val StreakWeek = 7

// --- Draft -------------------------------------------------------------------

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

@Composable
private fun AnswerForm(
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
        Spacer(Modifier.height(24.dp))
        FieldLabel(themLabel(question.type, state.them.name), themColor)
        AnswerInput(question, theirs, themColor, forPartner = true, state = state) { theirs = it }
    }

    Spacer(Modifier.height(28.dp))

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

    Spacer(Modifier.height(12.dp))
    Text(
        text = "Hidden until you both answer. You can't edit after.",
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
    QuestionType.RANK -> "Your guess at $name's order"
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

/**
 * Today's moods. Tapping yours opens a picker (face color row, faces, other bricks);
 * picking sets it and closes. Theirs is hidden when unset.
 */
@Composable
private fun Moods(state: CoupleState, onSet: (Mood?, BrickColor?) -> Unit, onReply: (String) -> Unit) {
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
            .glass(inkOnBrick(structureSlug()), corner = 12.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            state.weather?.let { weather ->
                weather.brick?.let { WeatherBrickArt(it, Modifier.size(30.dp)) }
                Spacer(Modifier.width(6.dp))
                Text("${weather.tempF}°", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        MoodChip(who = "You", entry = mine, color = you, empty = "Set mood") { picking = !picking }
        theirs?.let {
            Spacer(Modifier.width(10.dp))
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
        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            var note by rememberSaveable { mutableStateOf("") }
            fun send(text: String) {
                sounds.play(Sfx.PLACE)
                feel.play(Feel.PLACE)
                onReply(text)
                note = ""
                replying = false
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoodReplies.forEach { r -> BrickChip(label = r, on = r == myReply, color = null, onClick = { send(r) }) }
            }
            Spacer(Modifier.height(10.dp))
            AmpField(value = note, onValueChange = { note = it.take(80) }, color = you, placeholder = "Or a short note")
            Spacer(Modifier.height(8.dp))
            AmpButton(label = "Send", slug = you.slug, enabled = note.isNotBlank(), icon = UiIcon.DONE) { send(note.trim()) }
        }
    }
    AnimatedVisibility(visible = picking) {
        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            MoodTiles(Mood.entries.filter { it.kind == Mood.Kind.FACE }, you, faceColor, ::pick)
            Spacer(Modifier.height(8.dp))
            MoodTiles(Mood.entries.filter { it.kind != Mood.Kind.FACE }, you, null, ::pick)
        }
    }
}

@Composable
private fun MoodTiles(moods: List<Mood>, you: BrickColor, faceColor: BrickColor?, onPick: (Mood) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        moods.forEach { mood ->
            Column(
                Modifier
                    .width(84.dp)
                    .glass(inkOnBrick(structureSlug()), corner = 12.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(mood) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MoodArt(mood, you, faceColor = faceColor)
                Spacer(Modifier.height(4.dp))
                Text(
                    mood.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MoodChip(
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
            MoodArt(entry.mood, color, size = 30.dp, faceColor = entry.color)
            Text("$who: ${entry.mood.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
        } else {
            Text(empty, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

/** Gold used to mark a special day. */
private val SpecialGold = Color(0xFFFFC94D)

/** Your answer and guess as one line for the question card. */
private fun answerLine(answer: com.ampersand.model.Answer?): String = listOfNotNull(
    answer?.values?.takeIf { it.isNotEmpty() }?.let { "You said: ${it.joinToString(", ")}." },
    answer?.guessText?.let { "You guessed: $it." },
).joinToString(" ")

/** What makes today special: your dates and birthdays (with years), then a holiday. */
private fun specialToday(state: CoupleState): List<Pair<String, String>> {
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
private fun SpecialDayCard(state: CoupleState, events: List<Pair<String, String>>, onAddPhoto: (String, String) -> Unit) {
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
            .glass(inkOnBrick(structureSlug()), corner = 12.dp, strength = 1.4f)
            .border(2.dp, SpecialGold, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBrick(UiIcon.DATES, size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                events.forEach { (title, detail) ->
                    Text("Today: $title", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        if (photo != null) {
            rememberPhoto(photo.data)?.let {
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.Image(
                    it, contentDescription = photo.label,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        } else if (!declined) {
            Spacer(Modifier.height(8.dp))
            Text("Add a photo to remember today?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconAction(UiIcon.ADD, "Add a photo", onClick = {
                    pick.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
                IconAction(UiIcon.CLOSE, "Not now", onClick = { declined = true })
            }
        }
    }
}

/** One-tap replies to your partner's mood. */
private val MoodReplies = listOf("Thinking of you", "Here if you need me", "Proud of you", "Sending a hug", "Call you later")

/** Moments as flat rows: a missed day to answer, and one thing from before. */
@Composable
private fun MomentsList(
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
                .padding(bottom = 8.dp)
                .glass(inkOnBrick(structureSlug()), corner = 12.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    sounds.play(Sfx.NAV)
                    feel.play(Feel.NAV)
                    if (m.kind == Moment.Kind.MISSED) onAnswerDay(m.date) else onOpenDay(m.date)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBrick(
                when (m.kind) {
                    Moment.Kind.MISSED -> UiIcon.WRITE
                    Moment.Kind.KEPT -> UiIcon.KEEP
                    Moment.Kind.ON_THIS_DAY -> UiIcon.DATES
                    else -> UiIcon.DETAILS
                },
                size = 26.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(m.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(m.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 2)
                // In full, not a link: reading it by accident is the whole value.
                m.day?.let { day ->
                    listOf(state.you.uid to "You", state.them.uid to state.them.name).forEach { (uid, who) ->
                        day.answerFor(uid)?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("$who: ${it.response}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }
    }
}


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
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        IconAction(UiIcon.BACK, "Back", onClick = onBack)
        Spacer(Modifier.height(8.dp))
        SectionLabel(day.date.format(DayFormat))
        Spacer(Modifier.height(10.dp))
        Text(day.question.text, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(20.dp))
        AnswerForm(state, Color(state.you.colorArgb), Color(state.them.colorArgb), question = day.question, onSubmit = onSubmit)
        Spacer(Modifier.height(48.dp))
    }
}

/** What the question card says about the day, and where tapping it goes. */
private data class HeroStatus(val title: String, val detail: String, val onClick: (() -> Unit)? = null)


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


