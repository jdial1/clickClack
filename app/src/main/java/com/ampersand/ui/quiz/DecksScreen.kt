package com.ampersand.ui.quiz

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.brick.glass
import com.ampersand.data.BadgeWall
import com.ampersand.ui.today.ShapeTomorrow
import com.ampersand.ui.today.Waiting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampersand.model.BrickHeight
import com.ampersand.model.CoupleState
import com.ampersand.model.DeckState
import com.ampersand.model.Quiz
import com.ampersand.model.QuizTheme
import com.ampersand.ui.brick.BrickPill
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Stud

/** Decks tab: shape tomorrow, the longer tests, then every deck with your-turn decks first. Results are on Us. */
@Composable
fun DecksScreen(
    state: CoupleState,
    decks: List<Quiz>,
    onOpenDeck: (String) -> Unit,
    modifier: Modifier = Modifier,
    themes: List<QuizTheme> = emptyList(),
    onPickTomorrow: (String) -> Unit = {},
    onTakeInstrument: (String) -> Unit = {},
    onWrite: () -> Unit = {},
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    var theme by rememberSaveable { mutableStateOf<QuizTheme?>(null) }
    val deckThemes = remember(decks) { QuizTheme.entries.filter { t -> decks.any { it.theme == t } } }
    val shown = remember(decks, theme) { theme?.let { t -> decks.filter { it.theme == t } } ?: decks }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Decks", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            "Longer sets of questions to play together.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        ShapeTomorrow(state, themes, onPickTomorrow, onWrite)

        Spacer(Modifier.height(28.dp))
        LongerTests(state, onTakeInstrument)

        Spacer(Modifier.height(28.dp))
        SectionLabel("Themes")
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(deckThemes, key = { it.name }) { t ->
                val on = theme == t
                val inTheme = decks.filter { it.theme == t }
                val waiting = inTheme.count { state.deckState(it.id) == DeckState.WAITING_ON_YOU }
                BrickPill(
                    slug = t.brick.slug,
                    studSize = Stud.chip,
                    height = BrickHeight.HIGH,
                    alpha = if (theme == null || on) 1f else 0.5f,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        sounds.play(if (on) Sfx.DESELECT else Sfx.SELECT)
                        feel.play(if (on) Feel.DESELECT else Feel.SELECT)
                        theme = if (on) null else t
                    },
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(t.label, style = MaterialTheme.typography.titleSmall, color = inkOnBrick(t.brick.slug))
                        Text(
                            if (waiting > 0) "${inTheme.size} decks, $waiting for you" else "${inTheme.size} decks",
                            style = MaterialTheme.typography.labelSmall,
                            color = inkOnBrick(t.brick.slug),
                        )
                    }
                }
            }
        }


        Spacer(Modifier.height(28.dp))
        SectionLabel(theme?.let { "${it.label} decks" } ?: "All decks")
        DeckList(decks = shown, state = state, onOpen = onOpenDeck)

        Spacer(Modifier.height(48.dp))
    }
}

/** The three profile tests: start or continue yours. Finished ones point to Us. */
@Composable
private fun LongerTests(state: CoupleState, onTake: (String) -> Unit) {
    val groups = remember(state.instrumentRuns, state.you, state.them) {
        BadgeWall.groups(state.instrumentRuns, state.you, state.them)
    }
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    SectionLabel("Longer tests")
    Spacer(Modifier.height(10.dp))
    groups.forEach { g ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .glass(inkOnBrick(structureSlug()), corner = 12.dp)
                .then(
                    if (!g.yourTurn) Modifier
                    else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        sounds.play(Sfx.PRESS)
                        feel.play(Feel.PRESS)
                        onTake(g.instrumentId)
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(g.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    when {
                        g.isEarned -> "Done. Results are on Us."
                        !g.yourTurn -> "Waiting for ${state.them.name}"
                        else -> g.standing.ifBlank { "${g.minutes} min each" }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (g.yourTurn) {
                Text(g.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}
