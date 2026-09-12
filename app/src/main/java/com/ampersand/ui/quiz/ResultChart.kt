package com.ampersand.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampersand.R
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.model.CrossAnswer
import com.ampersand.model.Depth
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Quiz
import com.ampersand.model.QuizResult
import com.ampersand.model.QuizTheme
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.theme.AmpersandTheme
import kotlinx.coroutines.delay

/**
 * How a finished quiz is reported: two towers, not one number.
 *
 * Every couples app in this space collapses the result to a single percentage,
 * and that percentage throws away the only genuinely interesting thing in the
 * data -- that it is usually lopsided. "We scored 60%" is a fact nobody repeats.
 * "You knew me five times and I knew you twice" is the whole conversation.
 *
 * Drawn as bricks stacked per point, because a stack of bricks is already this
 * app's unit of "a thing you accumulated", and because at these sizes -- six
 * points, maybe ten -- counting real objects is easier than reading a bar
 * against an axis.
 */
@Composable
fun CrossAnswerChart(
    result: QuizResult,
    you: BrickColor,
    them: BrickColor,
    yourName: String,
    theirName: String,
    modifier: Modifier = Modifier,
) {
    val possible = result.possible
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    // Both towers build off one counter, so they race rather than take turns --
    // the lopsided result, which is the whole reason there are two towers, is
    // only legible if the shorter one visibly stops first.
    //
    // One cue per course, not one per brick per tower: twelve clicks in a row
    // stops being a thing being built and starts being a slot machine.
    val built = remember(result.quiz.id) { mutableIntStateOf(0) }
    LaunchedEffect(result.quiz.id) {
        val tallest = maxOf(result.youKnewThem, result.theyKnewYou)
        // Keeps a long deck's build under about two thirds of a second.
        val beat = if (tallest > 6) Motion.BrickMs / 2 else Motion.BrickMs
        repeat(tallest) {
            delay(beat.toLong())
            built.intValue = it + 1
            sounds.play(Sfx.CLICK)
            feel.play(Feel.TICK)
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScoreTower(
                score = result.youKnewThem,
                built = built,
                possible = possible,
                color = you,
                // Whose *knowledge* is being measured, not whose answer.
                caption = "$yourName knew $theirName",
                modifier = Modifier.weight(1f),
            )
            ScoreTower(
                score = result.theyKnewYou,
                built = built,
                possible = possible,
                color = them,
                caption = "$theirName knew $yourName",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = result.verdict,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        result.lopsided(yourName, theirName)?.let { line ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (result.agreements > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "You gave the same answer ${result.agreements} " +
                    if (result.agreements == 1) "time." else "times.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * One person's score, as a stack.
 *
 * Every possible point is drawn; the ones not scored are the same brick left
 * faint. Showing only what was earned would make three-out-of-four and
 * three-out-of-ten look identical, which is exactly the comparison being made.
 */
@Composable
private fun ScoreTower(
    score: Int,
    /** Courses laid so far, shared with the other tower. Read inside the Canvas. */
    built: MutableIntState,
    possible: Int,
    color: BrickColor,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val shape = BrickShape(4, BrickHeight.MEDIUM)
    val asset = color.asset(shape)
    val atlas = rememberBricks(listOf(asset))

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stud = maxWidth / shape.w
            val course = stud * shape.h * BrickArt.ROW_RATIO
            val nub = stud * BrickArt.NUB_RATIO

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(course * possible.coerceAtLeast(1) + nub),
            ) {
                val studPx = size.width / shape.w
                val coursePx = studPx * shape.h * BrickArt.ROW_RATIO
                // `built` is read here, inside the draw lambda, and nowhere
                // else. A snapshot read in this scope invalidates draw only --
                // so a tower can lay six courses without recomposing, which
                // matters because this Canvas lives inside a BoxWithConstraints
                // and recomposing it re-subcomposes the whole measurement.
                val laid = minOf(built.intValue, score)
                for (i in 0 until possible.coerceAtLeast(1)) {
                    drawBrickArt(
                        atlas = atlas,
                        asset = asset,
                        studs = shape.w,
                        rows = shape.h,
                        left = 0f,
                        bottom = size.height - i * coursePx,
                        studPx = studPx,
                        alpha = if (i < laid) 1f else 0.16f,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "$score / $possible",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        SectionLabel(caption, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * The per-question breakdown under the chart.
 *
 * Both answers and both guesses, because "you got this one wrong" is useless
 * without what each of you actually said.
 */
@Composable
fun CrossAnswerRow(
    row: CrossAnswer,
    you: BrickColor,
    them: BrickColor,
    yourName: String,
    theirName: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = row.question.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Said(yourName, row.yours, you)
        Spacer(Modifier.height(4.dp))
        Said(theirName, row.theirs, them)

        if (row.question.isScored) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Hit("$yourName called it", row.youKnewThem, you)
                Hit("$theirName called it", row.theyKnewYou, them)
            }
        }
    }
}

@Composable
private fun Said(name: String, values: List<String>, color: BrickColor) {
    if (values.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Pip(color)
        Spacer(Modifier.size(8.dp))
        Text(
            text = "$name: ${values.joinToString(", ")}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A 1-stud brick, used as a colour key rather than a bullet. */
@Composable
private fun Pip(color: BrickColor) {
    val shape = BrickShape(1, BrickHeight.LOW)
    val asset = color.asset(shape)
    val atlas = rememberBricks(listOf(asset))
    val stud = 11.dp
    Canvas(Modifier.size(stud, stud * (shape.h * BrickArt.ROW_RATIO + BrickArt.NUB_RATIO))) {
        drawBrickArt(atlas, asset, shape.w, shape.h, 0f, size.height, size.width)
    }
}

/**
 * Landed or missed, in Kenney's icons rather than dingbats.
 *
 * The pack art is a white silhouette, so it tints to whatever ink the row is
 * using and works in both themes from one file.
 */
@Composable
private fun Hit(label: String, hit: Boolean, color: BrickColor) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(if (hit) R.drawable.icon_check else R.drawable.icon_cross),
            contentDescription = null,
            tint = if (hit) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (hit) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
        )
    }
}

// --- Previews ----------------------------------------------------------------

/**
 * A worked cross-answer result, used by the previews and the quiz kit.
 *
 * Deliberately lopsided -- four hits against two -- because a balanced sample
 * makes the chart look like a decoration rather than a comparison.
 */
internal fun sampleCrossResult(): QuizResult {
    fun q(id: String, text: String, vararg options: String) = Question(
        id = id,
        text = text,
        type = if (options.isEmpty()) QuestionType.GUESS_ME else QuestionType.THIS_OR_THAT,
        category = "Food",
        depth = Depth(1),
        options = options.toList(),
    )
    val quiz = Quiz(
        id = "meals",
        title = "What's for dinner",
        blurb = "Ranking the food you argue about.",
        theme = QuizTheme.FOOD,
        questions = listOf(
            q("a", "Friday night is:", "Takeaway on the sofa", "Cook it together"),
            q("b", "Pudding is:", "Always", "Only out"),
            q("c", "Which could you eat every day for a month?"),
            q("d", "Breakfast is:", "A whole event", "Coffee and go"),
            q("e", "The washing up is:", "Straight away", "In the morning"),
        ),
    )
    val yours = listOf("Cook it together", "Always", "Ramen", "Coffee and go", "Straight away")
    val theirs = listOf("Takeaway on the sofa", "Always", "Roast potatoes", "A whole event", "In the morning")
    val yourGuess = listOf("Takeaway on the sofa", "Always", "Roast potatoes", "A whole event", "Straight away")
    val theirGuess = listOf("Takeaway on the sofa", "Only out", "Pasta", "Coffee and go", "In the morning")

    return QuizResult(
        quiz = quiz,
        rows = quiz.questions.mapIndexed { i, question ->
            CrossAnswer(
                question = question,
                yours = listOf(yours[i]),
                theirs = listOf(theirs[i]),
                yourGuess = listOf(yourGuess[i]),
                theirGuess = listOf(theirGuess[i]),
            )
        },
    )
}

@Preview(name = "Cross-answer chart", showBackground = true, heightDp = 560)
@Composable
private fun PreviewChart() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(Modifier.padding(24.dp)) {
            CrossAnswerChart(
                result = sampleCrossResult(),
                you = BrickColor.RED,
                them = BrickColor.BLUE,
                yourName = "Justin",
                theirName = "Maya",
            )
        }
    }
}

@Preview(name = "Cross-answer rows", showBackground = true, heightDp = 560)
@Composable
private fun PreviewRows() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(24.dp)) {
            sampleCrossResult().rows.take(3).forEach {
                CrossAnswerRow(it, BrickColor.RED, BrickColor.BLUE, "Justin", "Maya")
            }
        }
    }
}
