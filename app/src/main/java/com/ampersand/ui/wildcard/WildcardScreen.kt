package com.ampersand.ui.wildcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.model.BrickColor
import com.ampersand.model.CoupleState
import com.ampersand.model.QuestionType
import com.ampersand.model.Wildcard
import com.ampersand.model.label
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.AmpField
import com.ampersand.ui.components.AmpRadioGroup
import com.ampersand.ui.components.QuietNote
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme

/**
 * Write a question of your own.
 *
 * The pool is a hundred and eighty-two questions and every one of them was
 * written by a stranger months ago. They are decent, and they will never once
 * ask about the thing that actually happened this week. This is the only place
 * in the app where the content comes from inside the relationship.
 *
 * It goes in a queue rather than being asked immediately. A question that
 * arrives the second it is written is a message, and the whole design of this
 * app is that there are no messages -- waiting until tomorrow is what turns it
 * back into a question.
 *
 * Three types, not eight. This gets written on a phone, in a minute, usually
 * about something specific, and a form with eight controls on it is a form
 * nobody finishes.
 */
@Composable
fun WildcardScreen(
    state: CoupleState,
    onBack: () -> Unit,
    onSubmit: (String, QuestionType, List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val you = BrickColor.of(state.you.colorArgb)

    var text by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(QuestionType.REFLECT) }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }

    val needsOptions = type == QuestionType.THIS_OR_THAT
    val ready = text.isNotBlank() &&
        (!needsOptions || (optionA.isNotBlank() && optionB.isNotBlank()))

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
            SectionLabel("For ${state.them.name}")
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "Ask them something the app would never think of.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel("The question")
        Spacer(Modifier.height(10.dp))
        AmpField(
            value = text,
            onValueChange = { text = it },
            placeholder = "What do you want to ask?",
            color = you,
        )

        Spacer(Modifier.height(26.dp))
        SectionLabel("What kind")
        Spacer(Modifier.height(10.dp))
        AmpRadioGroup(
            options = Wildcard.WRITABLE.map { it.label },
            selected = type.label,
            accent = Color(you.argb),
            onSelect = { picked ->
                type = Wildcard.WRITABLE.first { it.label == picked }
            },
        )
        Spacer(Modifier.height(8.dp))
        QuietNote(explain(type, state.them.name))

        if (needsOptions) {
            Spacer(Modifier.height(22.dp))
            SectionLabel("The two sides")
            Spacer(Modifier.height(10.dp))
            AmpField(
                value = optionA,
                onValueChange = { optionA = it },
                placeholder = "One",
                color = you,
            )
            Spacer(Modifier.height(10.dp))
            AmpField(
                value = optionB,
                onValueChange = { optionB = it },
                placeholder = "Or the other",
                color = you,
            )
        }

        Spacer(Modifier.height(28.dp))
        AmpButton(
            label = "Put it in the queue",
            slug = you.slug,
            enabled = ready,
            sfx = Sfx.SEAL,
            feelOnClick = Feel.SEAL,
        ) {
            onSubmit(
                text.trim(),
                type,
                if (needsOptions) listOf(optionA.trim(), optionB.trim()) else emptyList(),
            )
            onBack()
        }

        Spacer(Modifier.height(12.dp))
        QuietNote("It will be the question on the next day nobody has answered yet.")
        Spacer(Modifier.height(40.dp))
    }
}

private fun explain(type: QuestionType, them: String): String = when (type) {
    QuestionType.REFLECT -> "You both write an answer. Nothing is scored: this is the one that is just for reading."
    QuestionType.GUESS_ME -> "You both answer, and you both guess what $them will say. This is the one with a score in it."
    else -> "Two options, one each. Fast, and surprisingly hard to call."
}

@Preview(name = "Wildcard", showBackground = true, heightDp = 1000)
@Composable
private fun PreviewWildcard() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        WildcardScreen(MockCouple.state(), onBack = {}, onSubmit = { _, _, _ -> })
    }
}
