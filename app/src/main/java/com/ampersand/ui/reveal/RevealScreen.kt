package com.ampersand.ui.reveal

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.ampersand.data.MockCouple
import com.ampersand.model.Answer
import com.ampersand.model.BrickColor
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.Member
import com.ampersand.model.Question
import com.ampersand.model.Reaction
import com.ampersand.ui.components.AmpReactionRow
import com.ampersand.ui.components.AnswerPanel
import com.ampersand.ui.components.arrive
import com.ampersand.ui.components.DepthDots
import com.ampersand.ui.components.PartnerReaction
import com.ampersand.ui.components.PersonDot
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
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.Motion
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

private val DayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM")

/**
 * Both answers, side by side, each in its owner's colour.
 *
 * Deliberately not a chat. One reaction and one optional follow-up note each is
 * the whole interaction budget: the conversation is meant to happen out loud.
 */
@Composable
fun RevealScreen(
    state: CoupleState,
    day: DayEntry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onReact: (Reaction?) -> Unit = {},
    /** False once this day has already opened, so it never unpacks twice. */
    animateIn: Boolean = true,
    onOpened: () -> Unit = {},
) {
    val youColor = Color(state.you.colorArgb)
    val themColor = Color(state.them.colorArgb)
    val score = day.score(state.you, state.them)
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    // Keyed on the *date*, not on the day. DayEntry is a data class, so a
    // reaction arriving from the other phone changes its equality and would
    // restart the whole unpacking under the reader's hands.
    LaunchedEffect(day.date, animateIn) {
        if (!animateIn) return@LaunchedEffect
        // Four beats, not five: the question is already on screen when you
        // arrive, and a cue for it would be a cue for nothing.
        repeat(4) { i ->
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
                Text("Back")
            }
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
        AnswerPanel(
            ownerName = state.you.name,
            ownerColor = youColor,
            type = day.question.type,
            answer = day.answerFor(state.you.uid)?.values.orEmpty(),
            guess = day.answerFor(state.them.uid)?.guess,
            guesserName = state.them.name,
            scored = day.question.isScored,
            hit = day.question.guessHit(
                day.answerFor(state.them.uid)?.guess,
                day.answerFor(state.you.uid)?.values.orEmpty(),
            ),
        )
        }

        Spacer(Modifier.height(16.dp))

        Box(Modifier.arrive(day.date, index = 3, enabled = animateIn)) {
        AnswerPanel(
            ownerName = state.them.name,
            ownerColor = themColor,
            type = day.question.type,
            answer = day.answerFor(state.them.uid)?.values.orEmpty(),
            guess = day.answerFor(state.you.uid)?.guess,
            guesserName = "You",
            scored = day.question.isScored,
            hit = day.question.guessHit(
                day.answerFor(state.you.uid)?.guess,
                day.answerFor(state.them.uid)?.values.orEmpty(),
            ),
        )
        }

        Spacer(Modifier.height(30.dp))
        SectionLabel("Say something back")
        Spacer(Modifier.height(10.dp))
        Box(Modifier.arrive(day.date, index = 4, enabled = animateIn)) {
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
            QuietNote("${state.them.name} has not said anything back yet.")
        }

        Spacer(Modifier.height(30.dp))
        // The last word on the screen, and the only instruction the app ever
        // gives: the reaction is a receipt, not the conversation.
        Text(
            text = "Now go and ask them about it.",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(40.dp))
    }
}


@Composable
private fun ScoreBanner(score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(2) { i ->
            BrickDot(slug = quietSlug(), studSize = 14.dp, filled = i < score)
            Spacer(Modifier.size(6.dp))
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = when (score) {
                2 -> "You both called it."
                1 -> "One of you called it."
                else -> "Neither of you saw that coming."
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

/** Both of you have reacted: the row lit on your side, their label underneath. */
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
