package com.ampersand.ui.reveal

import com.ampersand.model.Chats
import com.ampersand.model.Kept
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.IconAction
import androidx.compose.foundation.layout.width
import com.ampersand.ui.components.IconBrick
import com.ampersand.ui.components.UiIcon
import com.ampersand.ui.today.ShapeTomorrow
import com.ampersand.ui.today.DidYouTalk
import com.ampersand.model.QuizTheme
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.model.BrickColor
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.Reaction
import com.ampersand.ui.components.AmpReactionRow
import com.ampersand.ui.components.AnswerPanelPair
import com.ampersand.ui.components.arrive
import com.ampersand.ui.components.DepthDots
import com.ampersand.ui.components.PartnerReaction
import com.ampersand.ui.components.QuietNote
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.quietSlug
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.today.agoIn
import com.ampersand.ui.theme.Stud
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

private val DayFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d")

/** A revealed day: both answers side by side, plus one reaction each. */
@Composable
fun RevealScreen(
    state: CoupleState,
    day: DayEntry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onReact: (Reaction?) -> Unit = {},
    /** Pass false if this day was already opened, to skip the entrance animation. */
    animateIn: Boolean = true,
    onOpened: () -> Unit = {},
    onTalkedAbout: (java.time.LocalDate) -> Unit = {},
    onNote: (String) -> Unit = {},
    onOpenChat: (String) -> Unit = {},
    onKeep: (String, String?) -> Unit = { _, _ -> },
    themes: List<QuizTheme> = emptyList(),
    onPickTomorrow: (String) -> Unit = {},
    onWrite: () -> Unit = {},
) {
    val youColor = Color(state.you.colorArgb)
    val themColor = Color(state.them.colorArgb)
    val score = day.score(state.you, state.them)
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    // Keyed on date, not DayEntry: an incoming reaction changes equality and would restart the animation.
    LaunchedEffect(day.date, animateIn) {
        if (!animateIn) return@LaunchedEffect
        // One sound per staggered element after the question.
        repeat(3) { i ->
            delay(Motion.StaggerMs.toLong())
            sounds.play(if (i % 2 == 0) Sfx.CLICK else Sfx.CLACK)
            feel.play(Feel.TICK)
        }
        onOpened()
    }

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
            TextButton(
                onClick = { sounds.play(Sfx.NAV); feel.play(Feel.NAV); onBack() },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) { IconBrick(UiIcon.BACK, size = 20.dp); Spacer(Modifier.width(6.dp)); Text("Back") }
            }
            val key = Kept.day(day.date)
            val isKept = key in state.kept
            IconAction(UiIcon.KEEP, if (isKept) "Kept" else "Keep", onClick = {
                onKeep(key, if (isKept) null else day.question.text)
            })
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(day.date.format(DayFormat))
                DepthDots(day.question.depth)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = day.question.text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.arrive(day.date, index = 0, enabled = animateIn),
        )

        if (score != null) {
            Spacer(Modifier.height(18.dp))
            Box(Modifier.arrive(day.date, index = 1, enabled = animateIn)) {
                ScoreBanner(score)
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(Modifier.arrive(day.date, index = 2, enabled = animateIn)) {
            AnswerPanelPair(
                type = day.question.type,
                scored = day.question.isScored,
                yourName = state.you.name,
                yourColor = youColor,
                yourAnswer = day.answerFor(state.you.uid)?.values.orEmpty(),
                yourGuess = day.answerFor(state.you.uid)?.guess,
                youKnewThem = day.question.guessHit(
                    day.answerFor(state.you.uid)?.guess,
                    day.answerFor(state.them.uid)?.values.orEmpty(),
                ),
                theirName = state.them.name,
                theirColor = themColor,
                theirAnswer = day.answerFor(state.them.uid)?.values.orEmpty(),
                theirGuess = day.answerFor(state.them.uid)?.guess,
                theyKnewYou = day.question.guessHit(
                    day.answerFor(state.them.uid)?.guess,
                    day.answerFor(state.you.uid)?.values.orEmpty(),
                ),
            )
        }

        // Capsule days only, after reveal: the earlier answers to the same question.
        state.capsule?.takeIf { it.question.id == day.question.id && day.isRevealed }?.let { then ->
            Spacer(Modifier.height(30.dp))
            Box(Modifier.arrive(day.date, index = 3, enabled = animateIn)) {
                Column(Modifier.fillMaxWidth()) {
                    SectionLabel("And ${agoIn(then.date, day.date)}")
                    Spacer(Modifier.height(10.dp))
                    AnswerPanelPair(
                        type = then.question.type,
                        scored = then.question.isScored,
                        yourName = state.you.name,
                        yourColor = youColor,
                        yourAnswer = then.answerFor(state.you.uid)?.values.orEmpty(),
                        yourGuess = then.answerFor(state.you.uid)?.guess,
                        youKnewThem = then.question.guessHit(
                            then.answerFor(state.you.uid)?.guess,
                            then.answerFor(state.them.uid)?.values.orEmpty(),
                        ),
                        theirName = state.them.name,
                        theirColor = themColor,
                        theirAnswer = then.answerFor(state.them.uid)?.values.orEmpty(),
                        theirGuess = then.answerFor(state.them.uid)?.guess,
                        theyKnewYou = then.question.guessHit(
                            then.answerFor(state.them.uid)?.guess,
                            then.answerFor(state.you.uid)?.values.orEmpty(),
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))
        SectionLabel("React")
        Spacer(Modifier.height(10.dp))
        Box(Modifier.arrive(day.date, index = 3, enabled = animateIn)) {
            AmpReactionRow(
                selected = day.reactionFor(state.you.uid),
                color = BrickColor.of(state.you.colorArgb),
                onSelect = onReact,
            )
        }

        Spacer(Modifier.height(16.dp))
        val theirs = day.reactionFor(state.them.uid)
        if (theirs != null) {
            PartnerReaction(
                name = state.them.name,
                reaction = theirs,
                color = BrickColor.of(state.them.colorArgb),
            )
        } else {
            QuietNote("${state.them.name} hasn't reacted yet.")
        }

        if (day.isRevealed) {
            // One conversation per day: as many lines and emotes as you like.
            Spacer(Modifier.height(24.dp))
            SectionLabel("Talk about it")
            Spacer(Modifier.height(8.dp))
            val chatId = Chats.day(day.date)
            val preview = state.chatPreviews[chatId]
            val line = preview?.let { (if (it.lastByUid == state.you.uid) "You" else state.them.name) + ": " + it.lastText }
                ?: day.notes[state.them.uid]?.let { "${state.them.name}: $it" } ?: day.notes[state.you.uid]?.let { "You: $it" }
            line?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 2)
                Spacer(Modifier.height(8.dp))
            }
            AmpButton(
                label = if (line == null) "Start a conversation" else "Open conversation",
                slug = null,
                icon = UiIcon.WRITE,
            ) { onOpenChat(chatId) }
        }

        Spacer(Modifier.height(24.dp))
        DidYouTalk(state, day, onTalkedAbout)

        if (day.date == state.today.date) {
            Spacer(Modifier.height(30.dp))
            ShapeTomorrow(state, themes, onPickTomorrow, onWrite)
        }
        Spacer(Modifier.height(40.dp))
    }
}


@Composable
private fun ScoreBanner(score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(2) { i ->
            BrickDot(slug = quietSlug(), studSize = Stud.pip, filled = i < score)
            Spacer(Modifier.size(6.dp))
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = when (score) {
                2 -> "You both guessed right."
                1 -> "One of you guessed right."
                else -> "Neither of you guessed right."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- Previews ----------------------------------------------------------------

@Preview(name = "Reveal - both right", showBackground = true, heightDp = 820)
@Composable
private fun PreviewRevealHit() = AmpersandTheme {
    val s = MockCouple.state()
    Surface(color = MaterialTheme.colorScheme.background) {
        RevealScreen(s, s.history.first { it.question.id == "g4" }, {})
    }
}

@Preview(name = "Reveal - reflect", showBackground = true, heightDp = 820)
@Composable
private fun PreviewRevealReflect() = AmpersandTheme {
    val s = MockCouple.state()
    Surface(color = MaterialTheme.colorScheme.background) {
        RevealScreen(s, s.history.first { it.question.id == "r2" }, {})
    }
}

/** Both partners have reacted. */
@Preview(name = "Reveal - reacted", showBackground = true, heightDp = 1100)
@Composable
private fun PreviewRevealReacted() = AmpersandTheme {
    val s = MockCouple.state()
    val day = s.history.first { it.question.id == "g4" }
    Surface(color = MaterialTheme.colorScheme.background) {
        RevealScreen(
            state = s,
            day = day.copy(
                reactions = mapOf(
                    s.you.uid to Reaction.KNEW_IT,
                    s.them.uid to Reaction.LAUGH,
                ),
            ),
            onBack = {},
        )
    }
}

@Preview(name = "Reveal - dark, split", showBackground = true, heightDp = 820)
@Composable
private fun PreviewRevealDark() = AmpersandTheme(darkTheme = true) {
    val s = MockCouple.state()
    Surface(color = MaterialTheme.colorScheme.background) {
        RevealScreen(s, s.history.first { it.question.id == "t3" }, {})
    }
}
