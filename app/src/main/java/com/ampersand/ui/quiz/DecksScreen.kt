package com.ampersand.ui.quiz

import com.ampersand.ui.components.panel
import com.ampersand.ui.components.Emphasis
import com.ampersand.ui.theme.IconSize
import com.ampersand.ui.theme.Radius
import com.ampersand.ui.theme.Corner
import com.ampersand.ui.theme.Inset
import com.ampersand.ui.theme.Space
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import com.ampersand.ui.components.ThemeIcon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.brick.glass
import com.ampersand.data.BadgeWall
import com.ampersand.ui.today.ShapeTomorrow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.ampersand.model.CoupleState
import com.ampersand.model.DeckState
import com.ampersand.model.Quiz
import com.ampersand.model.QuizTheme
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play

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
            .padding(horizontal = Inset.screen),
    ) {
        Spacer(Modifier.height(Space.l))
        Text("Decks", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Space.xs))
        Text(
            "Longer sets of questions to play together.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Space.xl))
        ShapeTomorrow(state, themes, onPickTomorrow, onWrite)

        LongerTests(state, onTakeInstrument)

        Spacer(Modifier.height(Space.xxl))
        SectionLabel("Themes")
        Spacer(Modifier.height(Space.m))
        // One scrolling row. Grey cards; only the icon brick carries the theme colour.
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            items(deckThemes, key = { it.name }) { t ->
                val on = theme == t
                val inTheme = decks.filter { it.theme == t }
                val waiting = inTheme.count { state.deckState(it.id) == DeckState.WAITING_ON_YOU }
                Column(
                    Modifier
                        .width(108.dp)
                        .panel(if (on) Emphasis.SELECTED else Emphasis.RAISED)
                        .then(if (on) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, Radius.medium) else Modifier)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            sounds.play(if (on) Sfx.DESELECT else Sfx.SELECT)
                            feel.play(if (on) Feel.DESELECT else Feel.SELECT)
                            theme = if (on) null else t
                        }
                        .padding(horizontal = Space.s, vertical = Space.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ThemeIcon(t, size = IconSize.tile)
                    Spacer(Modifier.height(Space.s))
                    Text(t.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                    Text(
                        if (waiting > 0) "${inTheme.size} · $waiting your turn" else if (inTheme.size == 1) "1 deck" else "${inTheme.size} decks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.m))
        DeckList(decks = shown, state = state, onOpen = onOpenDeck)

        Spacer(Modifier.height(Space.page))
    }
}

/** Profile tests not yet finished by both of you: start, continue, or wait. Hidden once all are done. */
@Composable
private fun LongerTests(state: CoupleState, onTake: (String) -> Unit) {
    // Finished tests live on Us; only ones still in progress show here.
    val groups = remember(state.instrumentRuns, state.you, state.them) {
        BadgeWall.groups(state.instrumentRuns, state.you, state.them).filter { !it.isEarned }
    }
    if (groups.isEmpty()) return
    Spacer(Modifier.height(Space.xxl))
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    SectionLabel("Longer tests")
    Spacer(Modifier.height(Space.m))
    groups.forEach { g ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = Space.s)
                .panel()
                .then(
                    if (!g.yourTurn) Modifier
                    else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        sounds.play(Sfx.PRESS)
                        feel.play(Feel.PRESS)
                        onTake(g.instrumentId)
                    },
                )
                .padding(horizontal = Space.m, vertical = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(g.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    when {
                        !g.yourTurn -> "${state.them.name} has not finished"
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
