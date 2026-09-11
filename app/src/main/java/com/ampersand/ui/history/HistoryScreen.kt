package com.ampersand.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.components.brickFace
import com.ampersand.ui.theme.AmpersandTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ShortDate = DateTimeFormatter.ofPattern("d MMM")
private const val WeeksShown = 10
private const val DaysPerWeek = 7

/**
 * Every answered day as a dot. Ten weeks of them read as a shape rather than a
 * list, which is the point: the artifact you are building is the pattern.
 */
@Composable
fun HistoryScreen(
    state: CoupleState,
    onOpenDay: (DayEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byDate = remember(state.history) { state.history.associateBy { it.date } }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 24.dp, end = 24.dp, top = 16.dp, bottom = 40.dp,
        ),
    ) {
        item {
            SectionLabel("The last ten weeks")
            Spacer(Modifier.height(16.dp))
            StudPlate(state, byDate, onOpenDay)
            Spacer(Modifier.height(32.dp))
            SectionLabel("Answered")
            Spacer(Modifier.height(8.dp))
        }

        items(state.history, key = { it.date.toString() }) { entry ->
            HistoryRow(state, entry) { onOpenDay(entry) }
        }
    }
}

/**
 * Ten weeks as a baseplate.
 *
 * Was a grid of flat circles. Same silhouette, but drawn as studs it stops being
 * a heat map and becomes the thing the rest of the app is made of: answered days
 * are studs standing proud, missed days are empty sockets, and the shape you
 * have built over ten weeks is legible at a glance.
 *
 * One Canvas for all 70 cells, with hit-testing done in the same coordinates
 * rather than by seventy clickable boxes.
 */
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
            SectionLabel(
                "${entry.date.format(ShortDate)}  ·  ${entry.question.category}",
            )
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

@Preview(name = "History", showBackground = true, heightDp = 900)
@Composable
private fun PreviewHistory() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        HistoryScreen(MockCouple.state(), {})
    }
}

@Preview(name = "History - dark", showBackground = true, heightDp = 900)
@Composable
private fun PreviewHistoryDark() = AmpersandTheme(darkTheme = true) {
    Surface(color = MaterialTheme.colorScheme.background) {
        HistoryScreen(MockCouple.state(), {})
    }
}
