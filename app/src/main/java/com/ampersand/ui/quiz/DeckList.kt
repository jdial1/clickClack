package com.ampersand.ui.quiz

import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.brick.glass
import androidx.compose.ui.draw.alpha
import com.ampersand.ui.components.ThemeIcon
import androidx.compose.foundation.layout.width
import com.ampersand.ui.components.IconBrick
import com.ampersand.ui.components.UiIcon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.data.QuizJson
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.CoupleState
import com.ampersand.model.DeckState
import com.ampersand.model.QuestionType
import com.ampersand.model.Quiz
import com.ampersand.model.QuizResult
import com.ampersand.model.QuizTheme
import com.ampersand.model.label
import com.ampersand.ui.brick.BrickPill
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.QuietNote
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.theme.Stud

/** All decks, filterable, grouped by [DeckState]. Finished decks stay listed so their results are one tap away. */
@Composable
fun DeckList(
    decks: List<Quiz>,
    state: CoupleState,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Not saveable on purpose: filters reset when you leave so decks never stay hidden.
    var themes by remember { mutableStateOf(emptySet<QuizTheme>()) }
    var types by remember { mutableStateOf(emptySet<QuestionType>()) }
    var sorting by remember { mutableStateOf(false) }

    // A deck matches a type if any of its questions has that type.
    val shown = remember(decks, themes, types) {
        decks.filter { deck ->
            (themes.isEmpty() || deck.theme in themes) &&
                (types.isEmpty() || deck.questions.any { it.type in types })
        }
    }

    val grouped = remember(shown, state.quizRuns) {
        shown.groupBy { state.deckState(it.id) }
            .mapValues { (group, inGroup) -> inGroup.sortedWith(order(group, state)) }
    }

    Column(modifier.fillMaxWidth()) {
        SortBricks(
            themes = themes,
            types = types,
            open = sorting,
            matching = shown.size,
            total = decks.size,
            onToggleOpen = { sorting = !sorting },
            onTheme = { themes = themes.toggle(it) },
            onType = { types = types.toggle(it) },
            onClear = { themes = emptySet(); types = emptySet() },
        )

        if (shown.isEmpty()) {
            Spacer(Modifier.height(20.dp))
            QuietNote("No decks match. Try fewer filters.")
            return@Column
        }

        DeckState.entries.forEach { group ->
            val inGroup = grouped[group].orEmpty()
            if (inGroup.isEmpty()) return@forEach

            Spacer(Modifier.height(18.dp))
            SectionLabel(
                when (group) {
                    DeckState.WAITING_ON_YOU -> "${state.them.name} is waiting for you"
                    DeckState.WAITING_ON_THEM -> "Waiting for ${state.them.name}"
                    else -> group.heading
                },
            )
            Spacer(Modifier.height(10.dp))

            inGroup.forEach { deck ->
                DeckRow(deck, group, state, onOpen)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value

/**
 * Collapsible filters: category chips (in each category's color) and question-type chips (plain).
 * The two rows combine with AND.
 */
@Composable
private fun SortBricks(
    themes: Set<QuizTheme>,
    types: Set<QuestionType>,
    open: Boolean,
    matching: Int,
    total: Int,
    onToggleOpen: () -> Unit,
    onTheme: (QuizTheme) -> Unit,
    onType: (QuestionType) -> Unit,
    onClear: () -> Unit,
) {
    val active = themes.size + types.size

    Spacer(Modifier.height(10.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrickChip(
            label = if (active == 0) "Filter" else "Filter  ·  $active",
            on = open || active > 0,
            color = null,
            icon = UiIcon.FILTER,
            onClick = onToggleOpen,
        )
        if (active > 0) {
            // Tapping the count clears all filters.
            Text(
                text = "$matching of $total  ·  Clear",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClear,
                ),
            )
        }
    }

    if (!open) return

    Spacer(Modifier.height(16.dp))
    SectionLabel("Category")
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuizTheme.entries.forEach { theme ->
            BrickChip(
                label = theme.label,
                on = theme in themes,
                color = theme.brick,
                onClick = { onTheme(theme) },
            )
        }
    }

    Spacer(Modifier.height(18.dp))
    SectionLabel("Question type")
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuestionType.entries.forEach { type ->
            BrickChip(
                label = type.label,
                on = type in types,
                color = null,
                onClick = { onType(type) },
            )
        }
    }
}

/** Toggle chip drawn as a brick pill; selected is full brightness, unselected is faded. */
@Composable
internal fun BrickChip(
    label: String,
    on: Boolean,
    color: BrickColor?,
    icon: UiIcon? = null,
    onClick: () -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    // Color stays constant; on/off is shown by alpha only.
    val lift by animateFloatAsState(if (on) 1f else 0f, Motion.glide, label = "chipLift")

    BrickPill(
        slug = color?.slug,
        studSize = Stud.chip,
        height = BrickHeight.MEDIUM,
        alpha = 0.45f + 0.55f * lift,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) {
            sounds.play(if (on) Sfx.DESELECT else Sfx.SELECT)
            feel.play(if (on) Feel.DESELECT else Feel.SELECT)
            onClick()
        },
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                IconBrick(it, size = 20.dp)
                Spacer(Modifier.width(6.dp))
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = inkOnBrick(color?.slug))
        }
    }
}

/**
 * Newest first by the run that put the deck in this group: theirs for waiting-on-you,
 * yours for waiting-on-them, the later of both for finished. Not-started keeps pool order.
 */
private fun order(group: DeckState, state: CoupleState): Comparator<Quiz> = when (group) {
    DeckState.WAITING_ON_YOU ->
        compareByDescending { state.theirRun(it.id)?.completedAtMillis ?: 0L }

    DeckState.WAITING_ON_THEM ->
        compareByDescending { state.myRun(it.id)?.completedAtMillis ?: 0L }

    DeckState.FINISHED -> compareByDescending {
        maxOf(
            state.myRun(it.id)?.completedAtMillis ?: 0L,
            state.theirRun(it.id)?.completedAtMillis ?: 0L,
        )
    }

    DeckState.NOT_STARTED -> Comparator { _, _ -> 0 }
}

@Composable
private fun DeckRow(
    deck: Quiz,
    group: DeckState,
    state: CoupleState,
    onOpen: (String) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground

    val detail = when (group) {
        DeckState.WAITING_ON_YOU -> "${deck.size} questions  ·  ${state.them.name} is done"
        DeckState.WAITING_ON_THEM -> "You're done. Opens when you both finish."
        DeckState.FINISHED -> finishedDetail(deck, state)
        DeckState.NOT_STARTED -> "${deck.size} questions  ·  about ${deck.minutes} min"
    }

    // Grey card; the icon brick carries the theme colour. Dimmed while waiting on them.
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (group == DeckState.WAITING_ON_THEM) 0.6f else 1f)
            .glass(inkOnBrick(structureSlug()), corner = 14.dp, strength = 1.4f)
            .clickable { onOpen(deck.id) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeIcon(deck.theme, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        run {
            Column(Modifier.weight(1f)) {
                Text(
                    text = deck.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ink,
                )
                Text(
                    text = "${deck.theme.label.uppercase()}  ·  $detail",
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** Both scores for a finished deck, kept separate rather than combined. */
@Composable
private fun finishedDetail(deck: Quiz, state: CoupleState): String {
    val mine = state.myRun(deck.id) ?: return "Finished"
    val theirs = state.theirRun(deck.id) ?: return "Finished"
    val result = QuizResult.of(deck, mine, theirs)
    if (result.possible == 0) return "Finished  ·  No score"
    return "You ${result.youKnewThem}  ·  ${state.them.name} ${result.theyKnewYou}" +
        "  ·  of ${result.possible}"
}

// --- Previews ----------------------------------------------------------------

private fun previewDecks(): List<Quiz> = listOf(
    """{"id":"a","title":"Do you know me","theme":"KNOW_ME","blurb":"",
       "questions":[{"id":"a1","text":"Friday is:","type":"THIS_OR_THAT",
       "options":["In","Out"]}]}""",
    """{"id":"b","title":"What's for dinner","theme":"FOOD","blurb":"",
       "questions":[{"id":"b1","text":"Pudding:","type":"THIS_OR_THAT",
       "options":["Always","Never"]}]}""",
    """{"id":"c","title":"Not serious","theme":"PLAYFUL","blurb":"",
       "questions":[{"id":"c1","text":"Fly or vanish:","type":"THIS_OR_THAT",
       "options":["Fly","Vanish"]}]}""",
).map { QuizJson.parse(it.trimIndent()) }

@Preview(name = "Deck list", showBackground = true, heightDp = 900)
@Composable
private fun PreviewDeckList() = AmpersandTheme {
    val base = MockCouple.state()
    val decks = previewDecks()
    val state = base.copy(
        quizRuns = listOf(
            com.ampersand.model.QuizRun("a", base.them.uid, mapOf("a1" to listOf("In"))),
            com.ampersand.model.QuizRun("b", base.you.uid, mapOf("b1" to listOf("Always"))),
        ),
    )
    Surface(color = MaterialTheme.colorScheme.background) {
        DeckList(decks, state, {}, Modifier.padding(horizontal = 24.dp))
    }
}
