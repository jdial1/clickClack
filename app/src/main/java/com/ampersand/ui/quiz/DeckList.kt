package com.ampersand.ui.quiz

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.BrickPill
import com.ampersand.ui.brick.glassOn
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
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.theme.Radius
import com.ampersand.ui.theme.Stud

/**
 * The whole pool, grouped by what it wants from you.
 *
 * Every deck is always available -- nothing is locked, nothing is rationed --
 * but "available" and "in no particular order" are different things. Sorted by
 * filename, the deck your partner finished on Sunday and is waiting on you for
 * looks exactly like the six neither of you has opened, and the only way to
 * find it is to remember it exists. Grouped by state, the list answers "what
 * should we do next" on its own.
 *
 * Finished decks stay in the list rather than disappearing into a history
 * screen, because a finished deck is not spent: the result is the reason you
 * played it, and it should be one tap from where you played it.
 */
@Composable
fun DeckList(
    decks: List<Quiz>,
    state: CoupleState,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Deliberately not saveable. A filter is a way of looking at the shelf for a
    // minute, not a setting: coming back to Today to find two thirds of the pool
    // hidden, with no memory of having hidden it, is worse than picking the
    // bricks again.
    var themes by remember { mutableStateOf(emptySet<QuizTheme>()) }
    var types by remember { mutableStateOf(emptySet<QuestionType>()) }
    var sorting by remember { mutableStateOf(false) }

    // A deck matches a type if it *contains* one, because a type belongs to a
    // question and the thing being filtered is a deck. "Decks with something to
    // drag in them" is the question someone is actually asking.
    val shown = remember(decks, themes, types) {
        decks.filter { deck ->
            (themes.isEmpty() || deck.theme in themes) &&
                (types.isEmpty() || deck.questions.any { it.type in types })
        }
    }

    // Grouped and ordered once per change rather than filtered four times in
    // the layout.
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
            QuietNote("No deck has all of that in it. Try fewer bricks.")
            return@Column
        }

        DeckState.entries.forEach { group ->
            val inGroup = grouped[group].orEmpty()
            if (inGroup.isEmpty()) return@forEach

            Spacer(Modifier.height(18.dp))
            SectionLabel(
                when (group) {
                    // Naming the person is the difference between a label and a
                    // nudge, and this is the only group that is asking for
                    // something.
                    DeckState.WAITING_ON_YOU -> "${state.them.name} is waiting on you"
                    DeckState.WAITING_ON_THEM -> "Waiting on ${state.them.name}"
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
 * Two rows of bricks, for two different questions about a deck.
 *
 * Categories wear the app's own colours, because here a category *is* a
 * colour: the deck rows below are already yellow for everyday and blue for
 * deep, so the brick you press is the brick you are looking for. Question types
 * have no colour of their own and get the plain one instead -- they are a fact
 * about the shape of a deck rather than its subject, and dressing them like
 * categories would imply a choice between the two rows when they actually
 * stack: spicy *and* something to put in order.
 *
 * Folded away until asked for. Sixteen bricks above the list is a control
 * panel, and the list is the point -- most days nobody wants to sort anything.
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
            label = if (active == 0) "Sort" else "Sort  ·  $active",
            on = open || active > 0,
            color = null,
            onClick = onToggleOpen,
        )
        if (active > 0) {
            // The count doubles as the way out. A separate "clear" brick would
            // be a seventeenth brick to say the thing this already says.
            Text(
                text = "$matching of $total  ·  clear",
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
    SectionLabel("What it asks of you")
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

/**
 * One brick you can press in.
 *
 * Drawn with [brickFace] rather than laid with pack art: these are labelled in
 * words of wildly different lengths and the real bricks only come in 1, 2, 4
 * and 6 studs, so a run of them would either clip or lie about its width.
 *
 * Pressed-in is the selected state, which is already what a chosen answer looks
 * like in a question. A chip that lit up instead would be a second word for the
 * same idea.
 */
@Composable
internal fun BrickChip(
    label: String,
    on: Boolean,
    color: BrickColor?,
    onClick: () -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    // A category keeps its colour whether or not it is pressed, so the row
    // reads as the colours you are hunting for; on and off is brightness, the
    // same as the nav bar and the reaction row. Real art has no third state
    // between a colour and grey, and faking one was the drawn brick's job.
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = inkOnBrick(color?.slug),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/**
 * Newest first, by whichever run made the deck land in this group.
 *
 * "Waiting on you" sorts by *their* submission, because that is the thing that
 * put it there and the one they are counting the days on. "Waiting on them"
 * sorts by yours, for the mirror reason. Finished sorts by whichever of the two
 * came last, which is the moment the result appeared.
 *
 * Not-started keeps pool order. Those runs do not exist, so there is nothing to
 * sort by, and the order the decks are written in is a curated one worth
 * keeping rather than replacing with alphabetical.
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
    val slug = deck.theme.brick.slug
    val ink = inkOnBrick(slug)

    val detail = when (group) {
        DeckState.WAITING_ON_YOU -> "${deck.size} questions  ·  they have already played"
        DeckState.WAITING_ON_THEM -> "Yours is in. Nothing opens until theirs is."
        DeckState.FINISHED -> finishedDetail(deck, state)
        DeckState.NOT_STARTED -> "${deck.size} questions  ·  about ${deck.minutes} min"
    }

    BrickSlab(
        slugs = listOf(slug),
        studs = 12,
        height = BrickHeight.MEDIUM,
        // Half-lit while it is out of your hands. Everything else is available
        // to open right now, and a finished deck still has its result in it.
        alpha = if (group == DeckState.WAITING_ON_THEM) 0.5f else 1f,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(deck.id) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 24.dp, bottom = 16.dp)
                .glassOn(slug, corner = 14.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

/**
 * A finished deck says how it went, not just that it happened.
 *
 * The two scores rather than a combined one, for the same reason the chart
 * shows two towers: the lopsided ones are the interesting ones, and averaging
 * them is what hides that.
 */
@Composable
private fun finishedDetail(deck: Quiz, state: CoupleState): String {
    val mine = state.myRun(deck.id) ?: return "Finished"
    val theirs = state.theirRun(deck.id) ?: return "Finished"
    val result = QuizResult.of(deck, mine, theirs)
    if (result.possible == 0) return "Finished  ·  nothing to score"
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
