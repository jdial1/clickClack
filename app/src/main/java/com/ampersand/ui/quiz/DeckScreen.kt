package com.ampersand.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampersand.data.MockCouple
import com.ampersand.data.QuizJson
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.CoupleState
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Quiz
import com.ampersand.model.QuizResult
import com.ampersand.model.label
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.glassOn
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.today.QuestionInput
import com.ampersand.ui.today.rememberDraft

/**
 * Playing one deck: a question at a time, then the cross-answer chart.
 *
 * One question per screen rather than a scrolling list. A deck is five or six
 * questions each asked twice -- your answer and your guess at theirs -- and a
 * dozen inputs on one page is a form to be endured. One at a time also means
 * the answer you just gave is off screen before you answer the next, which is
 * the point: you are supposed to say the first thing, not tune a set of
 * responses against each other.
 *
 * There is no back step *within* a deck, and the system back leaves the deck
 * rather than rewinding it. The seal already says answers cannot be edited
 * after submitting, and letting you revise question three while looking at
 * question four is the same thing in miniature.
 */
@Composable
fun DeckScreen(
    state: CoupleState,
    quiz: Quiz,
    onBack: () -> Unit,
    onSubmit: (answers: Map<String, List<String>>, guesses: Map<String, List<String>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mine = state.myRun(quiz.id)
    val theirs = state.theirRun(quiz.id)

    when {
        // Both in: the deck is done and this is the payoff.
        mine != null && theirs != null -> DeckResult(
            result = QuizResult.of(quiz, mine, theirs),
            state = state,
            onBack = onBack,
            modifier = modifier,
        )

        mine != null -> DeckWaiting(quiz, state.them.name, onBack, modifier)

        else -> DeckPlayer(state, quiz, onBack, onSubmit, modifier)
    }
}

@Composable
private fun DeckPlayer(
    state: CoupleState,
    quiz: Quiz,
    onBack: () -> Unit,
    onSubmit: (Map<String, List<String>>, Map<String, List<String>>) -> Unit,
    modifier: Modifier,
) {
    var index by remember(quiz.id) { mutableStateOf(0) }
    val answers = remember(quiz.id) { mutableMapOf<String, List<String>>() }
    val guesses = remember(quiz.id) { mutableMapOf<String, List<String>>() }

    val question = quiz.questions[index]
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)

    // Keyed on the question, so moving on genuinely clears the form rather than
    // carrying the previous answer forward as a suggestion.
    val mine = rememberDraft(question)
    val guess = rememberDraft(question)

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
                Text("Leave")
            }
            SectionLabel("${index + 1} of ${quiz.size}")
        }

        Spacer(Modifier.height(10.dp))
        Progress(done = index, total = quiz.size, color = you)

        Spacer(Modifier.height(20.dp))
        BrickSlab(
            slugs = listOf(quiz.theme.brick.slug),
            studs = 12,
            height = BrickHeight.MEDIUM,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 18.dp)
                    .glassOn(quiz.theme.brick.slug)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                // What kind of question this is, and what it is about. Both
                // change how you answer: "put in order" is a different job from
                // "pick one", and knowing the category stops a light question
                // reading as a loaded one.
                Text(
                    text = "${question.type.label.uppercase()}  ·  ${question.category.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = inkOnBrick(quiz.theme.brick.slug).copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = inkOnBrick(quiz.theme.brick.slug),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Your answer")
        Spacer(Modifier.height(10.dp))
        QuestionInput(question, mine, you, forPartner = false, state = state)

        if (question.isScored) {
            Spacer(Modifier.height(26.dp))
            SectionLabel("What ${state.them.name} will say")
            Spacer(Modifier.height(10.dp))
            QuestionInput(question, guess, them, forPartner = true, state = state)
        }

        Spacer(Modifier.height(28.dp))
        val ready = mine.value.isComplete(question) &&
            (!question.isScored || guess.value.isComplete(question))
        val last = index == quiz.size - 1

        AmpButton(
            label = if (last) "Done" else "Next",
            slug = you.slug,
            enabled = ready,
        ) {
            answers[question.id] = mine.value.values(question)
            if (question.isScored) guesses[question.id] = guess.value.values(question)
            if (last) onSubmit(answers.toMap(), guesses.toMap()) else index++
        }

        Spacer(Modifier.height(40.dp))
    }
}

/** Your side is in; theirs is not. The seal, applied to a whole deck. */
@Composable
private fun DeckWaiting(quiz: Quiz, theirName: String, onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))
        // The deck's own colour, in its own brick. The identity was an emoji.
        BrickSlab(
            slugs = listOf(quiz.theme.brick.slug),
            studs = 4,
            height = BrickHeight.MEDIUM,
            modifier = Modifier.width(120.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Click.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "You have finished ${quiz.title}. Nothing opens until $theirName has too.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))
        AmpButton(label = "Back", slug = BrickColor.BLUE.slug, onClick = onBack)
    }
}

@Composable
private fun DeckResult(
    result: QuizResult,
    state: CoupleState,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text("Back")
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = result.quiz.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(24.dp))
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

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Now go and ask them about it.",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(48.dp))
    }
}

/**
 * How far through, in bricks rather than a bar: one brick per question.
 *
 * No height is set. BrickSlab rounds up to whole courses and ignores an
 * incoming height constraint, so asking for a 10dp strip gets a taller slab
 * that draws over its neighbour.
 */
@Composable
private fun Progress(done: Int, total: Int, color: BrickColor) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total) { i ->
            BrickSlab(
                slugs = listOf(color.slug),
                studs = 1,
                height = BrickHeight.LOW,
                alpha = if (i <= done) 1f else 0.22f,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// --- Previews ----------------------------------------------------------------

private fun sampleDeck(): Quiz = QuizJson.parse(
    """
    {
      "id": "preview", "title": "Not serious", "theme": "PLAYFUL",
      "blurb": "Nothing here matters.",
      "questions": [
        {"id": "p1", "text": "Superpower:", "type": "THIS_OR_THAT",
         "options": ["Fly", "Be invisible"]},
        {"id": "p2", "text": "Which would I survive longest without?", "type": "PICK_ONE",
         "options": ["My phone", "Coffee", "Music", "Sitting down"]}
      ]
    }
    """.trimIndent(),
)

@Preview(name = "Deck - playing", showBackground = true, heightDp = 900)
@Composable
private fun PreviewDeck() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        DeckScreen(MockCouple.state(), sampleDeck(), {}, { _, _ -> })
    }
}
