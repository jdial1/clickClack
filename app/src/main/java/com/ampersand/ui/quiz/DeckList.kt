package com.ampersand.ui.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.ampersand.model.Quiz
import com.ampersand.model.QuizResult
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.glassOn
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.theme.AmpersandTheme

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
    // Grouped and ordered once per change rather than filtered four times in
    // the layout.
    val grouped = remember(decks, state.quizRuns) {
        decks.groupBy { state.deckState(it.id) }
            .mapValues { (group, inGroup) -> inGroup.sortedWith(order(group, state)) }
    }

    Column(modifier.fillMaxWidth()) {
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
