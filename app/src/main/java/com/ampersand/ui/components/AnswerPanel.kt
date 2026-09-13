package com.ampersand.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.ampersand.model.QuestionType
import com.ampersand.ui.theme.Duo

/**
 * One person's answer, plus the partner's guess and whether it hit. Shared by
 * the reveal and deck result screens.
 *
 * [compact] is for half-width use in [AnswerPanelPair]: tighter padding, and the
 * guess label stacked above its badge so it doesn't wrap mid-phrase.
 */
@Composable
fun AnswerPanel(
    ownerName: String,
    ownerColor: Color,
    type: QuestionType,
    answer: List<String>,
    /** What the *other* person predicted this person would say. */
    guess: List<String>?,
    guesserName: String,
    scored: Boolean,
    hit: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 20.dp else 28.dp))
            .background(ownerColor.copy(alpha = 0.12f))
            .padding(if (compact) 14.dp else 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonDot(ownerName, ownerColor, size = if (compact) 22 else 26)
            Spacer(Modifier.size(if (compact) 7.dp else 10.dp))
            SectionLabel("$ownerName said", panel = false)
        }
        Spacer(Modifier.height(10.dp))

        if (answer.isEmpty()) {
            // No answer: show a note instead of an empty panel with a badge.
            Text(
                text = "No answer.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        AnswerValues(answer, type)

        if (type == QuestionType.RANK && scored) {
            // Rankings are scored on the top pick only.
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Only the top pick counts.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (scored && guess != null) {
            Spacer(Modifier.height(16.dp))
            if (compact) {
                SectionLabel("$guesserName guessed", panel = false)
                Spacer(Modifier.height(6.dp))
                HitBadge(hit)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("$guesserName guessed", panel = false)
                    Spacer(Modifier.size(8.dp))
                    HitBadge(hit)
                }
            }
            Spacer(Modifier.height(6.dp))
            // AnswerValues keeps rank order visible.
            AnswerValues(guess, type, quiet = true)
        }
    }
}

/**
 * Both answers side by side in compact panels. [IntrinsicSize.Min] keeps the
 * two cards the same height.
 */
@Composable
fun AnswerPanelPair(
    type: QuestionType,
    scored: Boolean,
    yourName: String,
    yourColor: Color,
    yourAnswer: List<String>,
    /** Your prediction about them. */
    yourGuess: List<String>?,
    youKnewThem: Boolean,
    theirName: String,
    theirColor: Color,
    theirAnswer: List<String>,
    /** Their prediction about you. */
    theirGuess: List<String>?,
    theyKnewYou: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnswerPanel(
            ownerName = yourName,
            ownerColor = yourColor,
            type = type,
            answer = yourAnswer,
            // Their guess goes under your answer, the thing it was guessing.
            guess = theirGuess,
            guesserName = theirName,
            scored = scored,
            hit = theyKnewYou,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            compact = true,
        )
        AnswerPanel(
            ownerName = theirName,
            ownerColor = theirColor,
            type = type,
            answer = theirAnswer,
            guess = yourGuess,
            guesserName = yourName,
            scored = scored,
            hit = youKnewThem,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            compact = true,
        )
    }
}

/** Text badge for a guess: hit or miss. */
@Composable
fun HitBadge(hit: Boolean) {
    // Filled vs. muted rather than two hues: couple-derived palettes can make hues look alike.
    val bg = if (hit) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = if (hit) "GOT IT" else "MISSED",
            style = MaterialTheme.typography.labelSmall,
            color = if (hit) Duo.inkOn(bg) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Prints a stored answer: numbered lines for [QuestionType.RANK], a comma list
 * otherwise. [quiet] uses smaller italic text, for guesses.
 */
@Composable
fun AnswerValues(
    values: List<String>,
    type: QuestionType,
    quiet: Boolean = false,
) {
    val style = if (quiet) {
        MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val color = if (quiet) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    if (type == QuestionType.RANK) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            values.forEachIndexed { i, value ->
                Text("${i + 1}.  $value", style = style, color = color)
            }
        }
    } else {
        Text(values.joinToString(", "), style = style, color = color)
    }
}
