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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.ampersand.model.BrickHeight
import com.ampersand.model.InstrumentPage
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.theme.Motion

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
    /** What you had answered when you last left, if you did. */
    resume: List<Int> = emptyList(),
    /** Called as each page is turned, and on the way out. */
    onSaveDraft: (responses: List<Int>) -> Unit = {},
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    // One slot per response across every pass. 0 means unanswered, which is
    // outside the 1..5 the instrument accepts, so it can never be mistaken for
    // a real answer the way a defaulted 3 could.
    val responses = remember(instrument.id) {
        mutableStateListOf<Int>().apply {
            repeat(instrument.length) { i -> add(resume.getOrElse(i) { 0 }) }
        }
    }
    val pages = remember(instrument.id) { instrument.pages() }
    // Open on the first page with something still to answer, so resuming lands
    // where you stopped rather than making you page past work you have done.
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
    // Each page opens at its own top. Carrying the previous page's scroll into
    // a fresh set of ten rows lands you in the middle of questions you have not
    // seen, which reads as having skipped some.
    LaunchedEffect(page) { scroll.scrollTo(0) }

    Column(modifier.fillMaxSize()) {
        // Fixed, outside the scroll. The point of a progress header is that it
        // is there while you are working, and one that scrolls off the top is
        // only ever visible at the moment you did not need it.
        StickyHeader(
            instrument = instrument,
            page = here,
            pages = pages.size,
            answered = answered,
            pageDone = pageDone,
            you = you,
            onBack = {
                sounds.play(Sfx.NAV)
                feel.play(Feel.NAV)
                // Back means the previous page while there is one. Leaving from
                // page three and losing twenty answers is not what anybody
                // reaching for that word intends.
                // Saved on the way out too, not only on a page turn. Backing
                // out of page one after answering six is still six answers.
                onSaveDraft(responses.toList())
                if (here.index > 0) page-- else onBack()
            },
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp),
        ) {
            // Only where the framing actually changes: at the top of the
            // sitting, and again when a second pass re-asks the same items
            // under a different stem.
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
                    last -> "Seal it"
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
                    !ready -> "Every one of them, or the arithmetic behind it means nothing."
                    last -> "Nothing opens until you have both finished."
                    else -> "Answered. ${pages.size - here.index - 1} more to go."
                },
            )

            // Only on the last page. A credit line under every page of a
            // four-page sitting is the same sentence printed four times.
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
        // Spread, not centred. Centred stops float in the middle of the row
        // with the two anchors marooned at the edges, and nothing says which
        // end means which -- the first stop has to sit under "Not like me" or
        // the labels above it are decoration.
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
                        // A ramp has to be visible before it is chosen from --
                        // that is the whole point of colouring it. A pole
                        // pair's unchosen stops are just markers and stay faint.
                        dimAlpha = if (item.isPolePair) 0.28f else 0.6f,
                    )
                }
            }
        }

        // What you just said, in words. Five identical bricks record a number
        // and show nothing back, so a row you answered and a row you answered
        // differently look the same on a scroll back up before sealing.
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
 * The bar that does not scroll away.
 *
 * Three things, in the order you need them: the way out, where you are in the
 * whole sitting, and what this page is even asking. The last matters most on a
 * two-pass instrument, where the same twenty items come round again under a
 * different stem -- answering round two while reading round one's heading
 * would produce a valid-looking set of numbers that measures nothing.
 *
 * The studs are one per item on *this* page rather than on the whole sitting.
 * Thirty-two studs across a phone is a texture, not a count, and the number
 * beside it already carries the total. What the row is for is the near
 * question: how many more before this page will let you on.
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

/**
 * One stud per item on the page, lit as you answer.
 *
 * The same object the deck screen counts questions with, so a sitting and a
 * deck report progress in the same language rather than each inventing one.
 */
@Composable
private fun PageStuds(done: Int, of: Int, you: BrickColor) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(of) { i ->
            val lit = i < done
            // Glide, not a bouncy spring: an overshoot above 1.0 goes straight
            // into drawImage as an alpha and is a documented trap in this repo.
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
 * What colour a stop is drawn in.
 *
 * On a statement the five stops are a ramp -- how much of this is you -- so
 * they run cool to hot and stay coloured whether or not they are picked. A row
 * of five identical grey bricks gives no clue which way it counts; a row that
 * visibly warms from left to right explains the scale before anybody reads the
 * anchors. `filled` does the rest: 28% for the four you did not choose, full
 * for the one you did.
 *
 * The first stop is [quietSlug] rather than a palette colour because it means
 * *none of this*, and because a fixed neutral is the pack's near-white brick,
 * which vanishes on a pale panel. The remaining four are every colour the pack
 * ships, in the only order that reads as increasing.
 *
 * A pole pair gets none of this. Its five stops are a position between two
 * descriptions, not an amount, and the middle means half and half -- so warming
 * toward one end would assert that one of the two is *more*, which is the one
 * thing the item is careful not to say.
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

/** How to read the five stops on this instrument, said once at the top. */
private fun scaleNote(instrument: Instrument): String =
    if (instrument.items.firstOrNull()?.isPolePair == true) {
        "Five steps between the two descriptions. The far ends mean that side " +
            "is always you; the middle one means half and half."
    } else {
        "Five steps, from not at all like you on the left to very much like " +
            "you on the right. The middle one means sometimes."
    }

/**
 * What one of the five stops means, in words.
 *
 * The two kinds of item mean genuinely different things by the same five
 * positions, which is the whole reason this is worth saying out loud. On a
 * statement the scale is *how true of you* -- the middle is "sometimes". On a
 * pole pair it is *how far between two descriptions* -- the middle is "half
 * and half", which is the one instruction Jorgenson's own sheet spells out.
 *
 * Pole pairs say left and right rather than naming the pole, because the poles
 * run to nine words ("when describing an event, will tell people what
 * happened") and a caption that wraps to three lines under every answered row
 * is worse than no caption.
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
