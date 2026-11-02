package com.ampersand.ui.quiz

import com.ampersand.ui.theme.Alpha
import com.ampersand.ui.theme.Space
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.ampersand.ui.components.AnswerPanelPair
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
 * Quiz result as one tower for the pair: a course per scoreable question, two
 * slots per course, filled bottom-up by how many guesses landed between you.
 * Slot colours alternate by position, not by whose guess it was, so the tower
 * never says who knew whom better.
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

    // One sound per brick laid.
    val built = remember(result.quiz.id) { mutableIntStateOf(0) }
    LaunchedEffect(result.quiz.id) {
        val beat = if (result.landed > 12) Motion.BrickMs / 2 else Motion.BrickMs
        repeat(result.landed) {
            delay(beat.toLong())
            built.intValue = it + 1
            sounds.play(if (it % 2 == 0) Sfx.CLICK else Sfx.CLACK)
            feel.play(Feel.TICK)
        }
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (possible == 0) {
            Text(
                text = "Nothing to score in this one.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            return@Column
        }

        PairTower(
            landed = result.landed,
            built = built,
            courses = possible,
            you = you,
            them = them,
            modifier = Modifier.fillMaxWidth(0.6f),
        )

        Spacer(Modifier.height(Space.m))
        Text(
            text = "${result.landed} of ${result.guesses} guesses landed",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (result.agreements > 0) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = "You answered the same ${result.agreements} " +
                    if (result.agreements == 1) "time." else "times.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A stack of [courses] courses, each two 2-stud bricks wide; the first [landed] slots are solid. */
@Composable
private fun PairTower(
    landed: Int,
    /** Bricks laid so far. Read inside the Canvas. */
    built: MutableIntState,
    courses: Int,
    you: BrickColor,
    them: BrickColor,
    modifier: Modifier = Modifier,
) {
    val half = BrickShape(2, BrickHeight.MEDIUM)
    val assets = listOf(you.asset(half), them.asset(half))
    val atlas = rememberBricks(assets)

    BoxWithConstraints(modifier) {
        val stud = maxWidth / (half.w * 2)
        val course = stud * half.h * BrickArt.ROW_RATIO
        val nub = stud * BrickArt.NUB_RATIO

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(course * courses + nub),
        ) {
            val studPx = size.width / (half.w * 2)
            val coursePx = studPx * half.h * BrickArt.ROW_RATIO
            val laid = minOf(built.intValue, landed)
            for (i in 0 until courses * 2) {
                val row = i / 2
                val side = i % 2
                // Running bond: alternate courses swap which colour sits left.
                val asset = assets[(side + row) % 2]
                drawBrickArt(
                    atlas = atlas,
                    asset = asset,
                    studs = half.w,
                    rows = half.h,
                    left = side * half.w * studPx,
                    bottom = size.height - row * coursePx,
                    studPx = studPx,
                    alpha = if (i < laid) 1f else Alpha.unlit,
                )
            }
        }
    }
}

/** Per-question breakdown: answers and guesses in the same [AnswerPanelPair] the reveal uses. */
@Composable
fun CrossAnswerRow(
    row: CrossAnswer,
    you: BrickColor,
    them: BrickColor,
    yourName: String,
    theirName: String,
    modifier: Modifier = Modifier,
    /** 1-based position in the deck; shows a "Question N" label when set. */
    number: Int? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = Space.m)) {
        if (number != null) {
            SectionLabel("Question $number")
            Spacer(Modifier.height(Space.s))
        }
        Text(
            text = row.question.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Mark rows where both gave the same answer.
        if (row.question.isScored && row.agreed) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = "Same answer",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(Space.m))
        AnswerPanelPair(
            type = row.question.type,
            scored = row.question.isScored,
            yourName = yourName,
            yourColor = Color(you.argb),
            yourAnswer = row.yours,
            yourGuess = row.yourGuess,
            youKnewThem = row.youKnewThem,
            theirName = theirName,
            theirColor = Color(them.argb),
            theirAnswer = row.theirs,
            theirGuess = row.theirGuess,
            theyKnewYou = row.theyKnewYou,
        )
    }
}


// --- Previews ----------------------------------------------------------------

/** Sample result, some guesses landed and some not, for previews and the quiz kit. */
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
        Box(Modifier.padding(Space.xl)) {
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
        Column(Modifier.padding(Space.xl)) {
            sampleCrossResult().rows.take(3).forEach {
                CrossAnswerRow(it, BrickColor.RED, BrickColor.BLUE, "Justin", "Maya")
            }
        }
    }
}
