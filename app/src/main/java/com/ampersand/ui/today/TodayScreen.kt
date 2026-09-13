package com.ampersand.ui.today

import com.ampersand.model.DayEntry
import com.ampersand.ui.theme.LocalAppearance
import com.ampersand.model.Wildcard
import com.ampersand.ui.components.WeatherBrickArt
import com.ampersand.ui.components.MoodArt
import com.ampersand.model.MoodEntry
import com.ampersand.model.Mood
import androidx.compose.foundation.layout.width
import com.ampersand.ui.brick.glassOn
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.structureSlug
import androidx.compose.runtime.saveable.rememberSaveable
import com.ampersand.model.Turn
import com.ampersand.model.InboxItem
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.data.MockQuestions
import com.ampersand.model.Answer
import com.ampersand.model.QuizTheme
import com.ampersand.model.Turns
import com.ampersand.model.Milestones
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
import com.ampersand.ui.components.PersonDot
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.glass
import com.ampersand.ui.brick.quietSlug
import com.ampersand.ui.brick.inkOnBricks
import com.ampersand.ui.theme.toArgbLong
import com.ampersand.ui.components.AnswerValues
import com.ampersand.ui.components.QuietNote
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.components.arrive
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Radius
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
    onSetMood: (Mood?, BrickColor?) -> Unit = { _, _ -> },
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
            StreakStuds(
                days = state.streak,
                you = BrickColor.of(state.you.colorArgb),
                milestone = remember(state.history, day.date) { Milestones.on(day.date, state.history) != null },
            )
        }

        Spacer(Modifier.height(8.dp))
        Moods(state, onSetMood)

        Spacer(Modifier.height(18.dp))
        QuestionHero(
            why = when {
                state.capsule?.question?.id == question.id -> "Again"
                Wildcard.isWildcard(question.id) -> "Your own"
                else -> pickedBy(state)?.let { if (it == "You") "Your pick" else "$it's pick" } ?: state.why
            },
            question = question,
            you = BrickColor.of(state.you.colorArgb),
            them = BrickColor.of(state.them.colorArgb),
            status = when {
                yourAnswer == null -> HeroStatus(
                    "Your turn",
                    "${state.them.name} sees your answer once they answer too.",
                )
                !day.isRevealed -> HeroStatus("Waiting for ${state.them.name}", "${state.them.name} has not answered yet.")
                else -> HeroStatus("You both answered", "Tap to see answers.", onOpenReveal)
            },
        )
        Spacer(Modifier.height(28.dp))

        // True only if this phone just submitted, so revisiting doesn't replay the animation.
        var justSealed by remember(day.date) { mutableStateOf(false) }

        if (yourAnswer == null) {
            AnswerForm(state, you, them) { values, guess ->
                justSealed = true
                onSubmit(values, guess)
            }
        } else {
            val sounds = LocalSounds.current
            val feel = LocalFeel.current
            // Column, not Box: a Box overlapped the waiting line with the answer.
            Column(
                Modifier.arrive(
                    key = Unit,
                    rise = 10.dp,
                    enabled = justSealed,
                    // Play the seal cue when the panel lands; the button is gone by then.
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
    /** Whose move it is, said on the card itself. */
    status: HeroStatus? = null,
) {
    // Text spans both colors, so pick ink that reads on the paler one (white on yellow fails).
    val ink = inkOnBricks(you.slug, them.slug)

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
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                    modifier = Modifier
                        .glass(ink, corner = 10.dp, strength = 1.6f)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
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
        slug = you.slug,
        onClick = onWrite,
    )

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
                yours -> "Tomorrow is ${picked!!.lowercase()}. ${state.them.name} sees it in the morning."
                picked == null -> "${state.them.name} picks tomorrow's theme."
                else -> "${state.them.name} picked tomorrow's theme. You'll see it in the morning."
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
 * Streak count plus seven one-stud bricks lit for the current week of the streak.
 * Wraps every seven days so the row never grows.
 */
@Composable
private fun StreakStuds(days: Int, you: BrickColor, milestone: Boolean = false) {
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
            // A milestone day (see Milestones.Runs) shows the count large, in your colour.
            Text(
                text = if (milestone) "$days days" else "$days",
                style = if (milestone) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                color = if (milestone) Color(you.argb) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
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

/** After you submit: shows your answer and guess, then waiting or open. */
@Composable
private fun SealedPanel(
    state: CoupleState,
    youColor: Color,
    themColor: Color,
    onOpenReveal: () -> Unit,
) {
    val day = state.today
    val yours = day.answerFor(state.you.uid)!!

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
            SectionLabel("Your answer")
        }
        Spacer(Modifier.height(10.dp))
        AnswerValues(yours.values, day.question.type)

        yours.guessText?.let { guess ->
            Spacer(Modifier.height(14.dp))
            SectionLabel("Your guess for ${state.them.name}")
            Spacer(Modifier.height(6.dp))
            Text(
                guess,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

}

/** Decks and tests one partner finished and the other hasn't, filtered by whose turn. */
@Composable
internal fun Waiting(state: CoupleState, items: List<InboxItem>, onOpen: (InboxItem) -> Unit) {
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)
    val yours = items.count { it.turn == Turn.YOURS }
    val theirs = items.size - yours
    var filter by rememberSaveable { mutableStateOf(if (yours > 0) Turn.YOURS else Turn.THEIRS) }
    val shown = items.filter { it.turn == filter }

    SectionLabel("Waiting")
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BrickChip(label = "Your turn  $yours", on = filter == Turn.YOURS, color = you, onClick = { filter = Turn.YOURS })
        BrickChip(label = "${state.them.name}'s turn  $theirs", on = filter == Turn.THEIRS, color = them, onClick = { filter = Turn.THEIRS })
    }
    Spacer(Modifier.height(12.dp))

    if (shown.isEmpty()) {
        QuietNote(if (filter == Turn.YOURS) "Nothing waiting on you." else "Nothing waiting on ${state.them.name}.")
        return
    }

    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    shown.forEach { item ->
        BrickSlab(
            slugs = listOf(structureSlug()),
            studs = 12,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    sounds.play(Sfx.NAV)
                    feel.play(Feel.NAV)
                    onOpen(item)
                },
        ) {
            val ink = inkOnBrick(structureSlug())
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 14.dp)
                    .glassOn(structureSlug(), corner = 14.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.kind.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = ink)
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = ink)
                    Text(
                        when (item.turn) {
                            Turn.YOURS -> "${state.them.name} finished. Your turn."
                            Turn.THEIRS -> "You finished. Waiting for ${state.them.name}."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ink,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    BrickDot(slug = if (item.youDone) you.slug else quietSlug(), studSize = Stud.pip, filled = item.youDone)
                    BrickDot(slug = if (item.themDone) them.slug else quietSlug(), studSize = Stud.pip, filled = item.themDone)
                }
            }
        }
    }
}

/**
 * Today's moods. Tapping yours opens a picker (face color row, faces, other bricks);
 * picking sets it and closes. Theirs is hidden when unset.
 */
@Composable
private fun Moods(state: CoupleState, onSet: (Mood?, BrickColor?) -> Unit) {
    val today = state.today.date
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)
    val mine = state.you.mood?.takeIf { it.current(today) != null }
    val theirs = state.them.mood?.takeIf { it.current(today) != null }
    var picking by rememberSaveable { mutableStateOf(false) }
    // The default face colour is set in Settings.
    val faceColor = LocalAppearance.current?.moodFace
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    fun pick(mood: Mood) {
        sounds.play(Sfx.PLACE)
        feel.play(Feel.PLACE)
        val color = faceColor.takeIf { mood.kind == Mood.Kind.FACE }
        onSet(if (mood == mine?.mood && color == mine.color) null else mood, color)
        picking = false
    }

    // One strip: the weather on the left, both moods on the right.
    Row(
        Modifier
            .fillMaxWidth()
            .glass(inkOnBrick(structureSlug()), corner = 12.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            MoodChip(who = state.them.name, entry = it, color = them, empty = "", onClick = null)
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


