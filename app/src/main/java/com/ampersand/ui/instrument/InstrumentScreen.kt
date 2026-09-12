package com.ampersand.ui.instrument

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.Instruments
import com.ampersand.data.MockCouple
import com.ampersand.data.Sources
import com.ampersand.model.BrickColor
import com.ampersand.model.CoupleState
import com.ampersand.model.Instrument
import com.ampersand.model.InstrumentItem
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.quietSlug
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.QuietNote
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Stud

/**
 * One sitting with one whole instrument.
 *
 * Deliberately not the deck screen. A deck is one question at a time because
 * each one deserves a think; an instrument is thirty-two either-ors that take
 * four minutes precisely *because* you answer them quickly and without
 * deliberating, which is what a scrolling list affords and a one-at-a-time
 * carousel actively prevents.
 *
 * The order is the published order and is never shuffled. Every item must be
 * answered before it can be sealed, because a subscale with a hole in it
 * produces a number that looks like a score and is not one.
 *
 * A multi-pass instrument asks the same items again under a second stem. The
 * love languages research measures what makes you feel loved and what you
 * actually do, and the gap between your first pass and their second is the
 * entire finding -- so the two passes run back to back, in one sitting, or not
 * at all.
 */
@Composable
fun InstrumentScreen(
    instrument: Instrument,
    you: BrickColor,
    onBack: () -> Unit,
    onSeal: (responses: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    // One slot per response across every pass. 0 means unanswered, which is
    // outside the 1..5 the instrument accepts, so it can never be mistaken for
    // a real answer the way a defaulted 3 could.
    val responses = remember(instrument.id) {
        mutableStateListOf<Int>().apply { repeat(instrument.length) { add(0) } }
    }
    var pass by remember(instrument.id) { mutableStateOf(0) }

    val from = pass * instrument.items.size
    val thisPass = responses.subList(from, from + instrument.items.size)
    val done = thisPass.count { it > 0 }
    val ready = done == instrument.items.size
    val last = pass == instrument.passCount - 1

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
            ) { Text("Back") }
            SectionLabel("$done of ${instrument.items.size}")
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = instrument.passes[pass].ifBlank { instrument.title },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (instrument.passCount > 1) {
                "Round ${pass + 1} of ${instrument.passCount}. ${instrument.blurb}"
            } else {
                instrument.blurb
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(22.dp))

        instrument.items.forEachIndexed { i, item ->
            ItemRow(
                item = item,
                value = thisPass[i],
                accent = you,
                onPick = { v ->
                    sounds.play(Sfx.CLICK)
                    feel.play(Feel.TICK)
                    responses[from + i] = v
                },
            )
            if (i < instrument.items.lastIndex) Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(26.dp))
        AmpButton(
            label = if (last) "Seal it" else "Next round",
            slug = you.slug,
            enabled = ready,
            sfx = if (last) Sfx.SEAL else Sfx.PRESS,
            feelOnClick = if (last) Feel.SEAL else Feel.PRESS,
        ) {
            if (last) onSeal(responses.toList()) else pass++
        }

        Spacer(Modifier.height(12.dp))
        QuietNote(
            if (ready) "Nothing opens until you have both finished."
            else "Every one of them, or the arithmetic behind it means nothing.",
        )

        Sources[instrument.sourceId]?.let { source ->
            Spacer(Modifier.height(18.dp))
            Text(
                text = "${source.credit}. ${source.venue}.\n${source.licence}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

/**
 * One item and its five stops.
 *
 * The poles sit on their own line above the stops rather than either side of
 * them. Flanking was the obvious layout and it does not survive real items:
 * "prefer to take multiple choice test" against "prefer essay answers" leaves
 * about seventy dp in the middle, which squeezed the stops down to something
 * you could neither see nor reliably hit. Full width for the words, full width
 * for the taps, and both get enough room.
 *
 * Every row is its own panel. The wall behind it has a seam and a run of studs
 * every course, and a line of small grey type crossing those is a line of type
 * with a stripe through it.
 */
@Composable
private fun ItemRow(
    item: InstrumentItem,
    value: Int,
    accent: BrickColor,
    onPick: (Int) -> Unit,
) {
    val answered = value > 0
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            // Answered rows recede. What is left to do should be what carries
            // down a list of thirty-two.
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = if (answered) 0.55f else 0.88f),
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        item.text?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Pole(item.low, Modifier.weight(1f), TextAlign.Start, item.isPolePair)
            Pole(item.high, Modifier.weight(1f), TextAlign.End, item.isPolePair)
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (1..5).forEach { stop ->
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                        ) { onPick(stop) }
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                ) {
                    // The quiet brick, not the neutral one: a `null` slug draws
                    // the pack's near-white brick, which on a pale wall is an
                    // invisible control. This is the same call the depth pips
                    // and score markers make.
                    BrickDot(
                        slug = if (value == stop) accent.slug else quietSlug(),
                        studSize = Stud.small,
                        filled = value == stop,
                    )
                }
            }
        }
    }
}

@Composable
private fun Pole(text: String, modifier: Modifier, align: TextAlign, prominent: Boolean) {
    Text(
        text = text,
        style = if (prominent) {
            MaterialTheme.typography.bodySmall
        } else {
            MaterialTheme.typography.labelSmall
        },
        color = if (prominent) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = align,
        modifier = modifier,
    )
}

@Preview(name = "Jungian", showBackground = true, heightDp = 1400)
@Composable
private fun PreviewJungian() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        InstrumentScreen(
            instrument = Instruments.JUNGIAN,
            you = BrickColor.RED,
            onBack = {},
            onSeal = {},
        )
    }
}

@Preview(name = "Love languages", showBackground = true, heightDp = 1100)
@Composable
private fun PreviewLanguages() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        InstrumentScreen(
            instrument = Instruments.LOVE_LANGUAGES,
            you = BrickColor.BLUE,
            onBack = {},
            onSeal = {},
        )
    }
}
