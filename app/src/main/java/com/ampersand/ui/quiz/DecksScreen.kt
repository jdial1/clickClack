package com.ampersand.ui.quiz

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
import com.ampersand.ui.us.ProfileBadges

/**
 * Everything you sit down and play: the decks, and the three longer tests.
 *
 * It used to sit at the bottom of Today, under the day's question, which made
 * the day's question feel like the top of a long page. Here a row of themes
 * comes first -- each brick says how many decks it holds and how many are
 * waiting on you -- then the tests, then every deck grouped by whose turn.
 */
@Composable
fun DecksScreen(
    state: CoupleState,
    decks: List<Quiz>,
    onOpenDeck: (String) -> Unit,
    onTakeInstrument: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    var theme by rememberSaveable { mutableStateOf<QuizTheme?>(null) }
    val themes = remember(decks) { QuizTheme.entries.filter { t -> decks.any { it.theme == t } } }
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
            "One question a day is the habit. These are for an evening.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(themes, key = { it.name }) { t ->
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
        ProfileBadges(state = state, onTake = onTakeInstrument)

        Spacer(Modifier.height(28.dp))
        SectionLabel(theme?.let { "${it.label} decks" } ?: "Every deck")
        DeckList(decks = shown, state = state, onOpen = onOpenDeck)

        Spacer(Modifier.height(48.dp))
    }
}
