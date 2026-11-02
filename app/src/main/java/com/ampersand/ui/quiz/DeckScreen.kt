package com.ampersand.ui.quiz

import com.ampersand.ui.theme.Alpha
import com.ampersand.ui.theme.Inset
import com.ampersand.ui.theme.Space
import com.ampersand.ui.components.AnswerValues
import com.ampersand.ui.components.IconAction
import com.ampersand.model.Kept
import com.ampersand.ui.components.IconBrick
import com.ampersand.ui.components.UiIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.data.QuizJson
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.CoupleState
import com.ampersand.model.Question
import com.ampersand.model.Quiz
import com.ampersand.model.QuizResult
import com.ampersand.model.label
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.glassOn
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.arrive
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.today.QuestionInput
import com.ampersand.ui.today.rememberDraft
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import com.ampersand.model.ProgressFloor

/**
 * Plays one deck a question per page, then shows the result once both have submitted.
 * No going back to earlier questions; system back leaves the deck.
 */
@Composable
fun DeckScreen(
    state: CoupleState,
    quiz: Quiz,
    onBack: () -> Unit,
    onSubmit: (answers: Map<String, List<String>>, guesses: Map<String, List<String>>) -> Unit,
    modifier: Modifier = Modifier,
    onKeep: (String, String?) -> Unit = { _, _ -> },
) {
    val mine = state.myRun(quiz.id)
    val theirs = state.theirRun(quiz.id)

    when {
        mine != null && theirs != null -> DeckResult(
            result = QuizResult.of(quiz, mine, theirs),
            state = state,
            onBack = onBack,
            modifier = modifier,
            onKeep = onKeep,
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

    // Keyed on the question so the next question starts blank.
    val mine = rememberDraft(question)
    val guess = rememberDraft(question)

    // Longer decks pin the header above the scroll; short ones keep it inline.
    val sticky = quiz.size > ProgressFloor

    Column(modifier.fillMaxSize()) {
        if (sticky) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = Alpha.strong))
                    .padding(horizontal = Inset.screen)
                    .padding(top = Space.m, bottom = Space.m),
            ) {
                DeckHeader(quiz, index, you, onBack)
            }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Inset.screen),
        ) {
        if (!sticky) {
            Spacer(Modifier.height(Space.m))
            DeckHeader(quiz, index, you, onBack)
        }

        Spacer(Modifier.height(Space.xl))
        BrickSlab(
            slugs = listOf(quiz.theme.brick.slug),
            studs = 12,
            height = BrickHeight.MEDIUM,
            // Slide in on each new question; no exit animation.
            modifier = Modifier.fillMaxWidth().arrive(index, slide = 16.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Space.l, end = Space.l, top = Space.xl, bottom = Space.l)
                    .glassOn(quiz.theme.brick.slug)
                    .padding(horizontal = Space.l, vertical = Space.m),
            ) {
                // Question type and category.
                Text(
                    text = "${question.type.label.uppercase()}  ·  ${question.category.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = inkOnBrick(quiz.theme.brick.slug).copy(alpha = Alpha.secondary),
                )
                Spacer(Modifier.height(Space.m))
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = inkOnBrick(quiz.theme.brick.slug),
                )
            }
        }

        Spacer(Modifier.height(Space.xl))
        SectionLabel("Your answer")
        Spacer(Modifier.height(Space.m))
        Box(Modifier.arrive(index, index = 1, slide = 16.dp)) {
            QuestionInput(question, mine, you, forPartner = false, state = state)
        }

        if (question.isScored) {
            Spacer(Modifier.height(Space.xl))
            SectionLabel("Guess ${state.them.name}'s answer")
            Spacer(Modifier.height(Space.m))
            Box(Modifier.arrive(index, index = 2, slide = 16.dp)) {
                QuestionInput(question, guess, them, forPartner = true, state = state)
            }
        }

        val ready = mine.value.isComplete(question) &&
            (!question.isScored || guess.value.isComplete(question))

        // Open decks: once you've answered this one, see what they said.
        val theirRun = state.theirRun(quiz.id)
        if (!quiz.sealed && ready) {
            theirRun?.answers?.get(question.id)?.let { theirs ->
                Spacer(Modifier.height(Space.xl))
                SectionLabel("${state.them.name} said")
                Spacer(Modifier.height(Space.s))
                AnswerValues(theirs, question.type)
            }
        }

        Spacer(Modifier.height(Space.xxl))
        val last = index == quiz.size - 1

        AmpButton(
            label = if (last) "Done" else "Next",
            icon = if (last) UiIcon.DONE else UiIcon.NEXT,
            slug = you.slug,
            enabled = ready,
            // Last question uses the seal sound and haptic.
            sfx = if (last) Sfx.SEAL else Sfx.RELEASE,
            feelOnClick = if (last) Feel.SEAL else Feel.RELEASE,
        ) {
            answers[question.id] = mine.value.values(question)
            if (question.isScored) guesses[question.id] = guess.value.values(question)
            if (last) onSubmit(answers.toMap(), guesses.toMap()) else index++
        }

        Spacer(Modifier.height(Space.page))
        }
    }
}

/** Leave button, question count, and progress row. */
@Composable
private fun DeckHeader(quiz: Quiz, index: Int, you: BrickColor, onBack: () -> Unit) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.ampersand.ui.components.QuietAction("Leave", onClick = onBack)
        SectionLabel("${index + 1} of ${quiz.size}", panel = false)
    }
    Spacer(Modifier.height(Space.m))
    Progress(done = index, total = quiz.size, color = you)
}

/** Shown after you submit a deck and before your partner has. */
@Composable
private fun DeckWaiting(quiz: Quiz, theirName: String, onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = Inset.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Space.page))
        BrickSlab(
            slugs = listOf(quiz.theme.brick.slug),
            studs = 4,
            height = BrickHeight.MEDIUM,
            modifier = Modifier.width(120.dp),
        )
        Spacer(Modifier.height(Space.xl))
        Text(
            text = "Done",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Space.m))
        Text(
            text = "You finished ${quiz.title}. $theirName has not finished yet. Opens when you both finish.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.xxl))
        AmpButton(label = "Back", slug = BrickColor.BLUE.slug, icon = UiIcon.BACK, onClick = onBack)
    }
}

@Composable
private fun DeckResult(
    result: QuizResult,
    state: CoupleState,
    onBack: () -> Unit,
    modifier: Modifier,
    onKeep: (String, String?) -> Unit,
) {
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Inset.screen),
    ) {
        Spacer(Modifier.height(Space.m))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            com.ampersand.ui.components.BackAction(onBack)
            val key = Kept.deck(result.quiz.id)
            val isKept = key in state.kept
            IconAction(UiIcon.KEEP, if (isKept) "Kept" else "Keep", onClick = {
                onKeep(key, if (isKept) null else result.quiz.title)
            })
        }

        Spacer(Modifier.height(Space.s))
        Text(
            text = result.quiz.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(Space.xl))
        CrossAnswerChart(
            result = result,
            you = you,
            them = them,
            yourName = state.you.name,
            theirName = state.them.name,
        )

        Spacer(Modifier.height(Space.xxl))
        SectionLabel("Question by question")
        result.rows.forEachIndexed { i, row ->
            CrossAnswerRow(row, you, them, state.you.name, state.them.name, number = i + 1)
        }

        Spacer(Modifier.height(Space.xl))
        Text(
            text = "Now go and ask them about it.",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Space.page))
    }
}

/**
 * One brick per question, lit up to the current one.
 * No height set: BrickSlab rounds up to whole courses and would overdraw neighbors.
 */
@Composable
private fun Progress(done: Int, total: Int, color: BrickColor) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        repeat(total) { i ->
            val lit = i <= done
            // No spring on alpha: overshooting 1.0 breaks drawImage.
            val alpha by animateFloatAsState(
                if (lit) 1f else 0.22f, Motion.glide, label = "progressLit",
            )
            val press by animateFloatAsState(
                if (lit) 1f else 0f, Motion.settle, label = "progressPress",
            )
            BrickSlab(
                slugs = listOf(color.slug),
                studs = 1,
                height = BrickHeight.LOW,
                alpha = alpha,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { translationY = (1f - press) * -6.dp.toPx() },
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
