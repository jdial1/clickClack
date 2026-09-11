package com.ampersand.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampersand.data.MockCouple
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.model.BuildPlate
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.Sculpture
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.components.brickFace
import com.ampersand.ui.theme.AmpersandTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ShortDate = DateTimeFormatter.ofPattern("d MMM")
private val LongDate = DateTimeFormatter.ofPattern("d MMMM yyyy")
private const val WeeksShown = 10
private const val DaysPerWeek = 7

/**
 * What the two of you have left behind: the sculptures, the anniversaries, and
 * every day you answered.
 *
 * This used to be the answered list with a grid above it, which is a log rather
 * than a memory. The order here is deliberate and emotional rather than
 * chronological: the thing you did not expect to see comes first, then the
 * things you made, then the shape of the habit, and only then the index.
 */
@Composable
fun HistoryScreen(
    state: CoupleState,
    onOpenDay: (DayEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byDate = remember(state.history) { state.history.associateBy { it.date } }
    val anniversaries = remember(state.history, state.today.date) { state.onThisDay() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 40.dp),
    ) {
        if (anniversaries.isNotEmpty()) {
            item {
                SectionLabel("On this day")
                Spacer(Modifier.height(12.dp))
            }
            items(anniversaries, key = { "ago_${it.date}" }) { entry ->
                OnThisDay(state, entry) { onOpenDay(entry) }
                Spacer(Modifier.height(12.dp))
            }
            item { Spacer(Modifier.height(20.dp)) }
        }

        item {
            SectionLabel("The shelf")
            Spacer(Modifier.height(12.dp))
            Shelf(state.shelf)
            Spacer(Modifier.height(32.dp))

            SectionLabel("The last ten weeks")
            Spacer(Modifier.height(16.dp))
            StudPlate(state, byDate, onOpenDay)
            Spacer(Modifier.height(32.dp))

            SectionLabel("Every day")
            Spacer(Modifier.height(8.dp))
        }

        items(state.history, key = { it.date.toString() }) { entry ->
            HistoryRow(state, entry) { onOpenDay(entry) }
        }
    }
}

/**
 * A day from six months or a year back, brought forward unasked.
 *
 * Shows both answers rather than a link to them. Making you tap to find out
 * what you said would turn the best thing on this screen into a chore, and the
 * whole value is in reading it by accident.
 */
@Composable
private fun OnThisDay(state: CoupleState, entry: DayEntry, onClick: () -> Unit) {
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)

    BrickSlab(
        slugs = listOf(you.slug, them.slug),
        studs = 12,
        height = BrickHeight.MEDIUM,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 22.dp)) {
            Text(
                text = ago(entry.date, state.today.date),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = inkOnBrick(you.slug).copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.question.text,
                style = MaterialTheme.typography.titleMedium,
                color = inkOnBrick(you.slug),
            )
            Spacer(Modifier.height(14.dp))
            Said(state.you.name, entry.answerFor(state.you.uid)?.response, you.slug)
            Spacer(Modifier.height(8.dp))
            Said(state.them.name, entry.answerFor(state.them.uid)?.response, you.slug)

            val marks = entry.reactions.values.toList()
            if (marks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = marks.joinToString(" ") { it.emoji },
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun Said(name: String, what: String?, onSlug: String?) {
    if (what == null) return
    Text(
        text = "$name: $what",
        style = MaterialTheme.typography.bodyMedium,
        color = inkOnBrick(onSlug).copy(alpha = 0.9f),
    )
}

/**
 * How long ago, in the units a person would actually use.
 *
 * Days are never mentioned: [CoupleState.onThisDay] only surfaces entries more
 * than a month old, so "42 days ago" could never come up, and "a year ago
 * today" is the phrase that makes someone stop scrolling.
 */
private fun ago(then: LocalDate, now: LocalDate): String {
    val months = ChronoUnit.MONTHS.between(then, now).toInt()
    val years = months / 12
    return when {
        years >= 2 -> "$years years ago today"
        years == 1 -> "A year ago today"
        months <= 1 -> "A month ago today"
        else -> "$months months ago today"
    }
}

/**
 * Finished sculptures, newest first, with the one you are looking at shown big.
 *
 * The large piece is the point and the strip is the index, not the other way
 * round: a row of thumbnails alone would make a week's build look like a
 * decoration rather than the thing you spent the week making.
 */
@Composable
private fun Shelf(shelf: List<BuildPlate>) {
    if (shelf.isEmpty()) {
        Text(
            text = "Nothing sealed yet. Your first sculpture lands at the end of the week.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    var selected by remember(shelf) { mutableStateOf(shelf.first()) }

    Column(Modifier.fillMaxWidth()) {
        Sculpture(selected, Modifier.fillMaxWidth())
        // A neutral course under the piece, so it stands on something rather
        // than floating in the middle of the screen.
        BrickSlab(
            slugs = listOf(null),
            modifier = Modifier.height(16.dp),
            studs = 12,
            height = BrickHeight.LOW,
        )

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Sealed ${selected.completedOn?.format(LongDate).orEmpty()}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        SectionLabel("${selected.placed.size} bricks  ·  built together")

        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shelf, key = { it.id }) { plate ->
                val on = plate.id == selected.id
                Column(
                    Modifier
                        .width(86.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selected = plate }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Sculpture(plate, Modifier.fillMaxWidth(), alpha = if (on) 1f else 0.4f)
                    Spacer(Modifier.height(6.dp))
                    SectionLabel(plate.completedOn?.format(ShortDate).orEmpty())
                }
            }
        }
    }
}

/**
 * Ten weeks as a wall of 1-stud bricks.
 *
 * Was a grid of drawn top-down studs. Nothing else in the app is seen from
 * above, so they read as a different object at every size; these are the same
 * pack bricks as everything else, one per day.
 *
 * One Canvas for all 70 cells, with hit-testing in the same coordinates rather
 * than seventy clickable boxes.
 */
@Composable
private fun StudPlate(
    state: CoupleState,
    byDate: Map<LocalDate, DayEntry>,
    onOpenDay: (DayEntry) -> Unit,
) {
    val today = state.today.date
    val start = today.minusDays((WeeksShown * DaysPerWeek - 1).toLong())
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)
    val cellShape = BrickShape(1, BrickHeight.LOW)
    val atlas = rememberBricks(
        listOf(you.asset(cellShape), them.asset(cellShape), cellShape.asset),
    )
    val plate = MaterialTheme.colorScheme.surfaceVariant

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell = maxWidth / DaysPerWeek
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(cell * 0.72f * WeeksShown)
                .clip(RoundedCornerShape(20.dp))
                .brickFace(plate, corner = 20.dp, studded = false, lip = 7.dp)
                .pointerInput(byDate, today) {
                    detectTapGestures { at ->
                        val col = (at.x / (size.width / DaysPerWeek)).toInt()
                        val row = (at.y / (size.height / WeeksShown)).toInt()
                        if (col !in 0 until DaysPerWeek || row !in 0 until WeeksShown) {
                            return@detectTapGestures
                        }
                        val date = start.plusDays((row * DaysPerWeek + col).toLong())
                        byDate[date]?.let(onOpenDay)
                    }
                },
        ) {
            val cw = size.width / DaysPerWeek
            val ch = size.height / WeeksShown
            val studPx = cw * 0.74f
            val inset = (cw - studPx) / 2f

            for (row in 0 until WeeksShown) {
                for (col in 0 until DaysPerWeek) {
                    val date = start.plusDays((row * DaysPerWeek + col).toLong())
                    if (date.isAfter(today)) continue

                    val entry = byDate[date]
                    // Alternating owner colour reads as "both of us built this";
                    // an unanswered day is the neutral brick, well faded.
                    val slug = if (entry == null) {
                        null
                    } else if ((row * DaysPerWeek + col) % 2 == 0) {
                        you.slug
                    } else {
                        them.slug
                    }
                    drawBrickArt(
                        atlas = atlas,
                        asset = BrickArt.assetFor(slug, cellShape),
                        studs = 1,
                        rows = 1,
                        left = col * cw + inset,
                        bottom = (row + 1) * ch - inset,
                        studPx = studPx,
                        alpha = if (entry == null) 0.16f else 1f,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(state: CoupleState, entry: DayEntry, onClick: () -> Unit) {
    val score = entry.score(state.you, state.them)
    val marks = entry.reactions.values.toList()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.question.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(
                    "${entry.date.format(ShortDate)}  ·  ${entry.question.category}",
                )
                if (marks.isNotEmpty()) {
                    Spacer(Modifier.size(8.dp))
                    Text(marks.joinToString(" ") { it.emoji }, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        if (score != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) { i ->
                    BrickDot(slug = null, studSize = 12.dp, filled = i < score)
                }
            }
        }
    }
}

@Preview(name = "Shelf", showBackground = true, heightDp = 1400)
@Composable
private fun PreviewHistory() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        HistoryScreen(MockCouple.state(), {})
    }
}

@Preview(name = "Shelf - dark", showBackground = true, heightDp = 1400)
@Composable
private fun PreviewHistoryDark() = AmpersandTheme(darkTheme = true) {
    Surface(color = MaterialTheme.colorScheme.background) {
        HistoryScreen(MockCouple.state(), {})
    }
}
