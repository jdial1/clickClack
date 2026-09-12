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
 * One person's answer, and whether the other one called it.
 *
 * Lives here rather than on a screen because two screens need exactly this and
 * had drifted badly apart: the reveal showed the guess beside the truth, and
 * the deck result -- the screen whose whole job is scoring -- showed the two
 * answers, a pass/fail mark, and never the guess at all. The guess is the
 * interesting half. "You got this one wrong" is a fact about nothing; "you
 * thought I'd say coffee" is the conversation.
 *
 * Laid out side by side by [AnswerPanelPair], which is where the sizing
 * happens. [compact] is what a panel does about being half a phone wide:
 * tighter padding, and the guess label stacked over its verdict rather than
 * beside it, because "MAYA GUESSED  NAILED IT" does not fit on one line at
 * that width and wrapping it mid-phrase reads as broken.
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
            SectionLabel("$ownerName said")
        }
        Spacer(Modifier.height(10.dp))

        if (answer.isEmpty()) {
            // Nothing to compare against. Saying so is better than an empty
            // panel with a verdict floating under it, which is what the deck
            // result used to draw for a question nobody answered.
            Text(
                text = "Nothing recorded.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        AnswerValues(answer, type)

        if (type == QuestionType.RANK && scored) {
            // Ranking is scored on the top pick alone, and a reader looking at
            // four items and one verdict has no way to know which part was
            // being judged.
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Scored on the first pick.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (scored && guess != null) {
            Spacer(Modifier.height(16.dp))
            if (compact) {
                SectionLabel("$guesserName guessed")
                Spacer(Modifier.height(6.dp))
                HitBadge(hit)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("$guesserName guessed")
                    Spacer(Modifier.size(8.dp))
                    HitBadge(hit)
                }
            }
            Spacer(Modifier.height(6.dp))
            // Through AnswerValues, not joinToString: a ranked guess is an
            // order, and a comma list throws away the only thing it says.
            AnswerValues(guess, type, quiet = true)
        }
    }
}

/**
 * Both answers, on one row.
 *
 * Side by side rather than stacked, so the comparison is a glance rather than a
 * scroll: the two cards are the same question answered twice, and reading one
 * after the other makes them feel like two separate facts.
 *
 * [IntrinsicSize.Min] is what keeps them honest. Without it the shorter answer
 * gets a shorter card and the pair reads as one person having said less, which
 * is a claim about the people rather than about the text. Measuring both and
 * taking the taller means the cards differ only in what is written in them.
 *
 * The cost of the row is width: each side is about half a phone, so a typed
 * sentence wraps hard. That is what [AnswerPanel]'s compact mode is for, and it
 * is the trade the layout makes -- the comparison is worth more than the line
 * length.
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
            // Their guess sits under your answer, because that is what it was
            // aiming at. Pairing a guess with its own author instead would put
            // the prediction and the truth in different columns.
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

/**
 * Landed or missed, in words.
 *
 * A word rather than a tick, because a bare mark has to be learned and these
 * two do not: nobody has to be told what MISSED means. It is also the only
 * place in the app that passes judgement, and judgement reads better in the
 * app's own voice than as a symbol.
 */
@Composable
fun HitBadge(hit: Boolean) {
    // Weight, not hue. `outline` against `primary` was the pair before, and in
    // a palette built from two people's colours those land close enough that
    // the two verdicts were the same magenta pill at a glance -- on the one
    // screen whose entire job is telling them apart. A landed guess is the
    // notable event and is filled and loud; a miss is the ordinary outcome and
    // recedes. That reads at arm's length and survives any pair of colours,
    // which a red/green pair would not.
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
            text = if (hit) "NAILED IT" else "MISSED",
            style = MaterialTheme.typography.labelSmall,
            color = if (hit) Duo.inkOn(bg) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A stored answer, printed the way it was given.
 *
 * [QuestionType.RANK] is the one that cannot be flattened: the order *is* the
 * answer, and a comma-separated line says the same thing for "dancing first"
 * and "dancing last". Everything else is a set, and reads fine as a list.
 *
 * [quiet] is for an answer in a supporting role -- a guess printed under the
 * truth it was aiming at -- where the same size and weight as the real answer
 * would make the panel read as having two of them.
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
