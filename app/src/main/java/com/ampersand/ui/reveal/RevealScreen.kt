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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.model.Answer
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.model.Member
import com.ampersand.model.Question
import com.ampersand.ui.components.DepthDots
import com.ampersand.ui.components.PersonDot
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.today.AnswerValues
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
) {
    val youColor = Color(state.you.colorArgb)
    val themColor = Color(state.them.colorArgb)
    val score = day.score(state.you, state.them)

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
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
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
        )

        if (score != null) {
            Spacer(Modifier.height(18.dp))
            ScoreBanner(score)
        }

        Spacer(Modifier.height(24.dp))

        PersonPanel(
            owner = state.you,
            ownerColor = youColor,
            question = day.question,
            answer = day.answerFor(state.you.uid),
            guessAboutOwner = day.answerFor(state.them.uid)?.guess,
            guessLabelPrefix = state.them.name,
        )

        Spacer(Modifier.height(16.dp))

        PersonPanel(
            owner = state.them,
            ownerColor = themColor,
            question = day.question,
            answer = day.answerFor(state.them.uid),
            guessAboutOwner = day.answerFor(state.you.uid)?.guess,
            guessLabelPrefix = "You",
        )

        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(18.dp),
        ) {
            Column {
                Text(
                    "Talk about this tonight",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bookmarks the question so it shows up on the Us screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

/** One person's truth, and whether the other one called it. */
@Composable
private fun PersonPanel(
    owner: Member,
    ownerColor: Color,
    question: Question,
    answer: Answer?,
    guessAboutOwner: List<String>?,
    guessLabelPrefix: String,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(ownerColor.copy(alpha = 0.12f))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonDot(owner.name, ownerColor, size = 26)
            Spacer(Modifier.size(10.dp))
            SectionLabel("${owner.name} said")
        }
        Spacer(Modifier.height(10.dp))
        AnswerValues(answer?.values.orEmpty(), question.type)

        if (question.isScored && guessAboutOwner != null) {
            val hit = question.guessHit(guessAboutOwner, answer?.values.orEmpty())

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("$guessLabelPrefix guessed")
                Spacer(Modifier.size(8.dp))
                HitBadge(hit)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = guessAboutOwner.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HitBadge(hit: Boolean) {
    val bg = if (hit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = if (hit) "NAILED IT" else "MISSED",
            style = MaterialTheme.typography.labelSmall,
            color = Duo.inkOn(bg),
        )
    }
}

@Composable
private fun ScoreBanner(score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(2) { i ->
            BrickDot(slug = null, studSize = 14.dp, filled = i < score)
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

@Preview(name = "Reveal - dark, split", showBackground = true, heightDp = 820)
@Composable
private fun PreviewRevealDark() = AmpersandTheme(darkTheme = true) {
    val s = MockCouple.state()
    Surface(color = MaterialTheme.colorScheme.background) {
        RevealScreen(s, s.history.first { it.question.id == "t3" }, {})
    }
}
