package com.ampersand.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampersand.data.QuizJson
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.CoupleState
import com.ampersand.model.Quiz
import com.ampersand.model.QuizTheme
import com.ampersand.model.weight
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.SectionLabel

/**
 * Every deck that parsed, and what a finished one looks like.
 *
 * A scaffold screen, like the form kit. It exists so the **types** can be
 * judged before there is any real pool: the decks here are two small JSON
 * files, and the point is the shapes they exercise rather than the questions
 * they ask.
 *
 * Loading is deliberately not wrapped in a try/catch. A malformed deck throws
 * with the file name and the reason, and that is the behaviour you want while
 * content is being written -- a deck that silently vanishes from the list is
 * far harder to notice than one that stops the screen.
 */
@Composable
fun QuizKitScreen(state: CoupleState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val decks = remember { QuizJson.loadBundled(context) }
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "Quiz kit",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${decks.size} decks loaded from assets/quizzes. The pool arrives as " +
                "JSON in exactly this shape.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        decks.forEach { deck ->
            Spacer(Modifier.height(28.dp))
            DeckCard(deck)
        }

        Spacer(Modifier.height(36.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(24.dp))

        SectionLabel("How a finished deck reports")
        Spacer(Modifier.height(16.dp))
        val result = remember { sampleCrossResult() }
        CrossAnswerChart(
            result = result,
            you = you,
            them = them,
            yourName = state.you.name,
            theirName = state.them.name,
        )

        Spacer(Modifier.height(28.dp))
        SectionLabel("Question by question")
        result.rows.forEach {
            CrossAnswerRow(it, you, them, state.you.name, state.them.name)
        }

        Spacer(Modifier.height(56.dp))
    }
}

/** One deck: its theme header, and the shape of every question in it. */
@Composable
private fun DeckCard(deck: Quiz) {
    Column(Modifier.fillMaxWidth()) {
        BrickSlab(
            slugs = listOf(deck.theme.brick.slug),
            studs = 12,
            height = BrickHeight.MEDIUM,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(deck.theme.glyph, fontSize = 18.sp)
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = "  ${deck.theme.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = inkOnBrick(deck.theme.brick.slug).copy(alpha = 0.8f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = deck.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = inkOnBrick(deck.theme.brick.slug),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = deck.blurb,
                    style = MaterialTheme.typography.bodySmall,
                    color = inkOnBrick(deck.theme.brick.slug).copy(alpha = 0.85f),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionLabel("${deck.size} questions  ·  about ${deck.minutes} min")
        Spacer(Modifier.height(10.dp))

        deck.questions.forEach { q ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = q.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(110.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = q.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (q.options.isNotEmpty()) {
                        Text(
                            text = q.options.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
