package com.ampersand.ui.wildcard

import com.ampersand.ui.theme.Inset
import com.ampersand.ui.theme.Space
import androidx.compose.foundation.layout.width
import com.ampersand.ui.components.IconBrick
import com.ampersand.ui.components.UiIcon
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
 * Write your own question for your partner. It is queued for the next unanswered day,
 * not sent immediately. Only the [Wildcard.WRITABLE] types are offered.
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
            .padding(horizontal = Inset.screen),
    ) {
        Spacer(Modifier.height(Space.m))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.ampersand.ui.components.BackAction(onBack)
            SectionLabel("For ${state.them.name}")
        }

        Spacer(Modifier.height(Space.l))
        Text(
            text = "Write your own question.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(Space.xl))
        SectionLabel("Question")
        Spacer(Modifier.height(Space.m))
        AmpField(
            value = text,
            onValueChange = { text = it },
            placeholder = "What do you want to ask?",
            color = you,
        )

        Spacer(Modifier.height(Space.xl))
        SectionLabel("Type")
        Spacer(Modifier.height(Space.m))
        AmpRadioGroup(
            options = Wildcard.WRITABLE.map { it.label },
            selected = type.label,
            accent = Color(you.argb),
            onSelect = { picked ->
                type = Wildcard.WRITABLE.first { it.label == picked }
            },
        )
        Spacer(Modifier.height(Space.s))
        QuietNote(explain(type, state.them.name))

        if (needsOptions) {
            Spacer(Modifier.height(Space.xl))
            SectionLabel("Options")
            Spacer(Modifier.height(Space.m))
            AmpField(
                value = optionA,
                onValueChange = { optionA = it },
                placeholder = "Option 1",
                color = you,
            )
            Spacer(Modifier.height(Space.m))
            AmpField(
                value = optionB,
                onValueChange = { optionB = it },
                placeholder = "Option 2",
                color = you,
            )
        }

        Spacer(Modifier.height(Space.xxl))
        AmpButton(
            label = "Add question",
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

        Spacer(Modifier.height(Space.m))
        QuietNote("It will be the question on the next unanswered day.")
        Spacer(Modifier.height(Space.page))
    }
}

private fun explain(type: QuestionType, them: String): String = when (type) {
    QuestionType.REFLECT -> "You both write an answer. No score."
    QuestionType.GUESS_ME -> "You both answer and guess what $them will say. Scored."
    else -> "Two options. You each pick one."
}

@Preview(name = "Wildcard", showBackground = true, heightDp = 1000)
@Composable
private fun PreviewWildcard() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        WildcardScreen(MockCouple.state(), onBack = {}, onSubmit = { _, _, _ -> })
    }
}
