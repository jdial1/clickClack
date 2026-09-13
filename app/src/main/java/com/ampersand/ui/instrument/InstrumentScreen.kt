package com.ampersand.ui.instrument

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.Instruments
import com.ampersand.data.Sources
import com.ampersand.model.BrickColor
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.ampersand.model.BrickHeight
import com.ampersand.model.InstrumentPage
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.theme.Motion

/**
 * Takes one instrument as paged, scrolling lists of 1-5 items, in published order (never shuffled).
 * Every item must be answered before sealing, or subscale scores are invalid.
 * Multi-pass instruments re-ask the same items under another stem in the same sitting.
 */
@Composable
fun InstrumentScreen(
    instrument: Instrument,
    you: BrickColor,
    onBack: () -> Unit,
    onSeal: (responses: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    /** Saved draft responses to resume from. */
    resume: List<Int> = emptyList(),
    /** Called on each page turn and when leaving. */
    onSaveDraft: (responses: List<Int>) -> Unit = {},
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    // One slot per response across all passes; 0 = unanswered (valid answers are 1..5).
    val responses = remember(instrument.id) {
        mutableStateListOf<Int>().apply {
            repeat(instrument.length) { i -> add(resume.getOrElse(i) { 0 }) }
        }
    }
    val pages = remember(instrument.id) { instrument.pages() }
    // Start on the first page with unanswered items.
    var page by rememberSaveable(instrument.id) {
        mutableStateOf(
            pages.indexOfFirst { p -> p.slots.any { responses[it] == 0 } }
                .coerceAtLeast(0),
        )
    }
    val here = pages[page.coerceIn(pages.indices)]

    val pageDone = here.slots.count { responses[it] > 0 }
    val ready = pageDone == here.size
    val last = here.index == pages.lastIndex
    val endsPass = last || pages[here.index + 1].pass != here.pass
    val answered = responses.count { it > 0 }

    val scroll = rememberScrollState()
    // Reset scroll to top on each page.
    LaunchedEffect(page) { scroll.scrollTo(0) }

    Column(modifier.fillMaxSize()) {
        val back: () -> Unit = {
            sounds.play(Sfx.NAV)
            feel.play(Feel.NAV)
            // Save the draft, then go to the previous page, or leave from page one.
            onSaveDraft(responses.toList())
            if (here.index > 0) page -= 1 else onBack()
        }
        // System back must match the header; otherwise it falls through to the shell and saves nothing.
        BackHandler(onBack = back)

        StickyHeader(
            instrument = instrument,
            page = here,
            pages = pages.size,
            answered = answered,
            pageDone = pageDone,
            you = you,
            onBack = back,
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp),
        ) {
            // Intro and scale note at the start of each pass.
            if (here.items.first == 0) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (instrument.passCount > 1) {
                        "Round ${here.pass + 1} of ${instrument.passCount}. ${instrument.blurb}"
                    } else {
                        instrument.blurb
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                QuietNote(scaleNote(instrument))
            }

            Spacer(Modifier.height(18.dp))

            here.items.forEachIndexed { i, item ->
                val slot = here.slots.first + i
                ItemRow(
                    item = instrument.items[item],
                    value = responses[slot],
                    accent = you,
                    onPick = { v ->
                        sounds.play(Sfx.CLICK)
                        feel.play(Feel.TICK)
                        responses[slot] = v
                    },
                )
                if (i < here.size - 1) Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(26.dp))
            AmpButton(
                label = when {
                    last -> "Finish"
                    endsPass -> "Next round"
                    else -> "Next"
                },
                slug = you.slug,
                enabled = ready,
                sfx = if (last) Sfx.SEAL else Sfx.PRESS,
                feelOnClick = if (last) Feel.SEAL else Feel.PRESS,
            ) {
                if (last) {
                    onSeal(responses.toList())
                } else {
                    onSaveDraft(responses.toList())
                    page++
                }
            }

            Spacer(Modifier.height(12.dp))
            QuietNote(
                when {
                    !ready -> "Answer every item to continue."
                    last -> "Opens when you both finish."
                    else -> "${pages.size - here.index - 1} more pages to go."
                },
            )

            // Source credit, last page only.
            if (last) {
                Sources[instrument.sourceId]?.let { source ->
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "${source.credit}. ${source.venue}. ${source.licence}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * One item in its own panel: text, pole labels on a line above, then five stops.
 * Poles don't flank the stops because long labels squeeze the tap targets.
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
            // Answered rows are more transparent.
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
        // Spread edge to edge so the end stops sit under the pole labels.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                    BrickDot(
                        slug = stopSlug(item, stop, value, accent),
                        studSize = Stud.small,
                        filled = value == stop,
                        // Statement ramps stay visible; pole-pair stops are faint until chosen.
                        dimAlpha = if (item.isPolePair) 0.28f else 0.6f,
                    )
                }
            }
        }

        // The chosen stop in words.
        if (answered) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stopLabel(item, value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Fixed header: back/leave, overall count, the current pass's stem, page number,
 * and one stud per item on this page.
 */
@Composable
private fun StickyHeader(
    instrument: Instrument,
    page: InstrumentPage,
    pages: Int,
    answered: Int,
    pageDone: Int,
    you: BrickColor,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .padding(horizontal = 24.dp)
            .padding(top = 10.dp, bottom = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text(if (page.index > 0) "Back" else "Leave") }

            if (instrument.needsProgress) {
                SectionLabel("$answered of ${instrument.length}", panel = false)
            }
        }

        Text(
            text = instrument.passes[page.pass].ifBlank { instrument.title },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (instrument.needsProgress) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Page ${page.index + 1} of $pages",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            PageStuds(done = pageDone, of = page.size, you = you)
        }
    }
}

/** One stud per item on the page, lit as you answer. */
@Composable
private fun PageStuds(done: Int, of: Int, you: BrickColor) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(of) { i ->
            val lit = i < done
            // No spring on alpha: overshooting 1.0 breaks drawImage.
            val alpha by animateFloatAsState(
                if (lit) 1f else 0.22f, Motion.glide, label = "pageStud",
            )
            BrickSlab(
                slugs = listOf(you.slug),
                studs = 1,
                height = BrickHeight.LOW,
                alpha = alpha,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Brick color for a stop. Statements use a cool-to-hot ramp starting at [quietSlug]
 * (a fixed white would vanish on pale panels). Pole pairs are neutral except the chosen stop.
 */
@Composable
private fun stopSlug(item: InstrumentItem, stop: Int, value: Int, accent: BrickColor): String? =
    if (item.isPolePair) {
        if (value == stop) accent.slug else quietSlug()
    } else {
        when (stop) {
            1 -> quietSlug()
            2 -> "blue"
            3 -> "green"
            4 -> "yellow"
            else -> "red"
        }
    }

/** Scale instructions shown at the top of the instrument. */
private fun scaleNote(instrument: Instrument): String =
    if (instrument.items.firstOrNull()?.isPolePair == true) {
        "Pick a spot between the two descriptions. The ends mean always that " +
            "side. The middle means half and half."
    } else {
        "Left means not at all like you, right means very much like you. " +
            "The middle means sometimes."
    }

/**
 * Caption for a chosen stop. Pole pairs say left/right instead of repeating the
 * pole text, which can be long enough to wrap several lines.
 */
private fun stopLabel(item: InstrumentItem, stop: Int): String =
    if (item.isPolePair) {
        when (stop) {
            1 -> "Always the left"
            2 -> "Usually the left"
            3 -> "Half and half"
            4 -> "Usually the right"
            else -> "Always the right"
        }
    } else {
        when (stop) {
            1 -> "Not at all"
            2 -> "A little"
            3 -> "Sometimes"
            4 -> "Quite a lot"
            else -> "Very much"
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
