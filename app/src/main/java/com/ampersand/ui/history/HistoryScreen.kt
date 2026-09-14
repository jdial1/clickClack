package com.ampersand.ui.history

import com.ampersand.ui.components.rememberPhoto
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.IconBrick
import com.ampersand.model.Kept
import com.ampersand.ui.components.UiIcon
import androidx.compose.foundation.layout.FlowRow
import com.ampersand.ui.quiz.BrickChip
import com.ampersand.model.name
import com.ampersand.model.SculptureNames
import com.ampersand.model.NameWords
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.AmpField
import com.ampersand.model.DayPips
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampersand.data.MockCouple
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.model.BuildPlate
import com.ampersand.model.Reaction
import com.ampersand.model.CoupleState
import com.ampersand.model.DayEntry
import com.ampersand.ui.brick.BrickArt
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.quietSlug
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.Sculpture
import com.ampersand.ui.brick.drawBrickArt
import com.ampersand.ui.brick.glass
import com.ampersand.ui.brick.inkOnBricks
import com.ampersand.ui.brick.rememberBricks
import com.ampersand.ui.components.drawable
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.components.arrive
import com.ampersand.ui.components.pressDepth
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.Stud
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ShortDate = DateTimeFormatter.ofPattern("d MMM")
private val LongDate = DateTimeFormatter.ofPattern("d MMMM yyyy")
private const val WeeksShown = 10
private const val DaysPerWeek = 7

/** History tab: on-this-day cards, the shelf of sculptures, the ten-week plate, then every day. */
@Composable
fun HistoryScreen(
    state: CoupleState,
    onOpenDay: (DayEntry) -> Unit,
    onNoteWeek: (plateId: String, text: String) -> Unit = { _, _ -> },
    onNameWeek: (plateId: String, words: NameWords) -> Unit = { _, _ -> },
    onOpenKept: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val byDate = remember(state.history) { state.history.associateBy { it.date } }
    val anniversaries = remember(state.history, state.today.date) { state.onThisDay() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 40.dp),
    ) {
        if (state.photos.isNotEmpty()) {
            item {
                SectionLabel("Our days")
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.photos, key = { it.id }) { p ->
                        Column(Modifier.width(150.dp).glass(inkOnBrick(structureSlug()), corner = 12.dp).padding(6.dp)) {
                            rememberPhoto(p.data)?.let {
                                androidx.compose.foundation.Image(
                                    it, contentDescription = p.label,
                                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(p.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                            Text(p.date.format(LongDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        if (state.kept.isNotEmpty()) {
            item {
                SectionLabel("Kept")
                Spacer(Modifier.height(10.dp))
            }
            val kept = state.kept.entries.sortedByDescending { Kept.dateOf(it.key) ?: java.time.LocalDate.MIN }
            items(kept.map { it.key }, key = { "kept_$it" }) { key ->
                val date = Kept.dateOf(key)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .glass(inkOnBrick(structureSlug()), corner = 12.dp)
                        .clickable { onOpenKept(key) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBrick(UiIcon.KEEP, size = 26.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            date?.format(LongDate) ?: "Deck",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(state.kept.getValue(key), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 2)
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }

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
            Shelf(state.shelf, state, onNoteWeek, onNameWeek)
            Spacer(Modifier.height(32.dp))

            SectionLabel("Every day")
            Spacer(Modifier.height(8.dp))
        }

        items(state.history, key = { it.date.toString() }) { entry ->
            HistoryRow(state, entry) { onOpenDay(entry) }
        }
    }
}

/** A past day (months or years back) with both answers shown inline. */
@Composable
private fun OnThisDay(state: CoupleState, entry: DayEntry, onClick: () -> Unit) {
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)
    // Text sits over both colors, so pick ink that reads on either.
    val ink = inkOnBricks(you.slug, them.slug)

    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    BrickSlab(
        slugs = listOf(you.slug, them.slug),
        studs = 12,
        height = BrickHeight.MEDIUM,
        modifier = Modifier
            .pressDepth(pressed)
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) { sounds.play(Sfx.TAP); feel.play(Feel.TAP); onClick() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 18.dp)
                .glass(ink)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = ago(entry.date, state.today.date),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = ink.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.question.text,
                style = MaterialTheme.typography.titleMedium,
                color = ink,
            )
            Spacer(Modifier.height(14.dp))
            Said(state.you.name, entry.answerFor(state.you.uid)?.response, ink)
            Spacer(Modifier.height(8.dp))
            Said(state.them.name, entry.answerFor(state.them.uid)?.response, ink)

            val marks = entry.reactions.values.toList()
            if (marks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ReactionMarks(marks, size = 20.dp)
            }
        }
    }
}

/** Reaction images left on a day. */
@Composable
private fun ReactionMarks(marks: List<Reaction>, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        marks.forEach {
            Image(
                painter = painterResource(it.drawable()),
                contentDescription = it.label,
                modifier = Modifier.size(size),
            )
        }
    }
}

@Composable
private fun Said(name: String, what: String?, ink: Color) {
    if (what == null) return
    Text(
        text = "$name: $what",
        style = MaterialTheme.typography.bodyMedium,
        color = ink.copy(alpha = 0.9f),
    )
}

/** "N months/years ago today". No day units: [CoupleState.onThisDay] only returns entries a month or older. */
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

/** Finished sculptures, newest first: the selected one large, the rest in a strip below. */
@Composable
private fun Shelf(
    shelf: List<BuildPlate>,
    state: CoupleState,
    onNoteWeek: (String, String) -> Unit,
    onNameWeek: (String, NameWords) -> Unit,
) {
    if (shelf.isEmpty()) {
        Text(
            text = "No sculptures yet. Your first one arrives at the end of the week.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    // Select by id so a saved note (which replaces the plate) keeps the same week selected.
    var selectedId by remember { mutableStateOf(shelf.first().id) }
    val selected = shelf.firstOrNull { it.id == selectedId } ?: shelf.first()
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    Column(Modifier.fillMaxWidth()) {
        // Not Crossfade: plates differ in height and Crossfade would size to the larger one.
        Sculpture(selected, Modifier.fillMaxWidth().arrive(selected.id, rise = 0.dp))
        // Neutral base under the sculpture.
        BrickSlab(
            slugs = listOf(null),
            modifier = Modifier.height(16.dp),
            studs = 12,
            height = BrickHeight.LOW,
        )

        Spacer(Modifier.height(14.dp))
        selected.name(state.you.uid, state.them.uid)?.let {
            Text(it, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = "Finished ${selected.completedOn?.format(LongDate).orEmpty()}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(14.dp))
        NameWeek(selected, state, onNameWeek)

        Spacer(Modifier.height(14.dp))
        WeekNotes(selected, state, onNoteWeek)

        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shelf, key = { it.id }) { plate ->
                val on = plate.id == selected.id
                Column(
                    Modifier
                        .width(86.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (!on) {
                                sounds.play(Sfx.TAP)
                                feel.play(Feel.TAP)
                                selectedId = plate.id
                            }
                        }
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

/** Each partner picks a describing word and a thing; the name shows once both have picked. */
@Composable
private fun NameWeek(plate: BuildPlate, state: CoupleState, onName: (String, NameWords) -> Unit) {
    if (plate.name(state.you.uid, state.them.uid) != null) return
    val you = BrickColor.of(state.you.colorArgb)
    val mine = plate.words[state.you.uid]

    SectionLabel("Name it")
    Spacer(Modifier.height(8.dp))
    if (mine != null) {
        Text(
            "You picked ${mine.describing} and ${mine.thing}. Waiting for ${state.them.name}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        return
    }

    var describing by remember(plate.id) { mutableStateOf<String?>(null) }
    var thing by remember(plate.id) { mutableStateOf<String?>(null) }
    Text(
        "Pick two words. ${state.them.name} picks two. The name shows when you both finish.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SculptureNames.offered(plate.id, state.you.uid, SculptureNames.Describing).forEach { w ->
            BrickChip(label = w, on = describing == w, color = you, onClick = { describing = w })
        }
    }
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SculptureNames.offered(plate.id, state.you.uid, SculptureNames.Things).forEach { w ->
            BrickChip(label = w, on = thing == w, color = you, onClick = { thing = w })
        }
    }
    Spacer(Modifier.height(10.dp))
    AmpButton(label = "Done", slug = you.slug, enabled = describing != null && thing != null, icon = UiIcon.DONE) {
        onName(plate.id, NameWords(describing!!, thing!!))
    }
}

/** One editable line from each partner about the week. Theirs shows once written. */
@Composable
private fun WeekNotes(plate: BuildPlate, state: CoupleState, onNote: (String, String) -> Unit) {
    val you = BrickColor.of(state.you.colorArgb)
    val mine = plate.notes[state.you.uid]
    val theirs = plate.notes[state.them.uid]
    var editing by remember(plate.id) { mutableStateOf(mine == null) }
    var draft by remember(plate.id) { mutableStateOf(mine.orEmpty()) }

    theirs?.let {
        Text("${state.them.name}: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
    }
    if (!editing && mine != null) {
        Text(
            "You: $mine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable { editing = true },
        )
    } else {
        AmpField(
            value = draft,
            onValueChange = { draft = it.take(140) },
            color = you,
            placeholder = "The week we...",
        )
        Spacer(Modifier.height(8.dp))
        AmpButton(label = "Save", slug = you.slug, enabled = draft.isNotBlank(), icon = UiIcon.DONE) {
            onNote(plate.id, draft.trim())
            editing = false
        }
    }
    if (theirs == null) {
        Spacer(Modifier.height(6.dp))
        Text(
            "Waiting for ${state.them.name}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Up to ten weeks (only as many as you've been going), two 1-stud bricks per day (you left, them right), lit when that person did something. See [DayPips].
 * One Canvas with manual hit-testing instead of 70 clickable boxes.
 */
@Composable
internal fun StudPlate(
    state: CoupleState,
    byDate: Map<LocalDate, DayEntry>,
    onOpenDay: (DayEntry) -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val today = state.today.date
    val weeks = remember(state.history, state.quizRuns, today) { DayPips.weeksActive(state, WeeksShown) }
    val start = today.minusDays((weeks * DaysPerWeek - 1).toLong())
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
                .height(cell * 0.72f * weeks)
                .clip(RoundedCornerShape(20.dp))
                .background(plate, RoundedCornerShape(20.dp))
                .pointerInput(byDate, today) {
                    detectTapGestures { at ->
                        val col = (at.x / (size.width / DaysPerWeek)).toInt()
                        val row = (at.y / (size.height / weeks)).toInt()
                        if (col !in 0 until DaysPerWeek || row !in 0 until weeks) {
                            return@detectTapGestures
                        }
                        val date = start.plusDays((row * DaysPerWeek + col).toLong())
                        val entry = byDate[date]
                        if (entry != null && entry.isRevealed) {
                            sounds.play(Sfx.TAP)
                            feel.play(Feel.TAP)
                            onOpenDay(entry)
                        } else {
                            // Nothing to open: haptic only, no sound.
                            feel.play(Feel.REJECT)
                        }
                    }
                },
        ) {
            val cw = size.width / DaysPerWeek
            val ch = size.height / weeks
            // Small gap within a day's pair, wider gap between days.
            val studPx = cw * 0.38f
            val gap = cw * 0.04f
            val inset = (cw - studPx * 2 - gap) / 2f

            for (row in 0 until weeks) {
                for (col in 0 until DaysPerWeek) {
                    val date = start.plusDays((row * DaysPerWeek + col).toLong())
                    if (date.isAfter(today)) continue

                    val entry = if (date == today) state.today else byDate[date]
                    listOf(state.you to you, state.them to them).forEachIndexed { side, (member, color) ->
                        val did = DayPips.didSomething(member.uid, date, entry, state.quizRuns)
                        drawBrickArt(
                            atlas = atlas,
                            asset = BrickArt.assetFor(if (did) color.slug else null, cellShape),
                            studs = 1,
                            rows = 1,
                            left = col * cw + inset + side * (studPx + gap),
                            bottom = (row + 1) * ch - inset,
                            studPx = studPx,
                            alpha = if (did) 1f else 0.16f,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(state: CoupleState, entry: DayEntry, onClick: () -> Unit) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val score = entry.score(state.you, state.them)
    val marks = entry.reactions.values.toList()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { sounds.play(Sfx.TAP); feel.play(Feel.TAP); onClick() }
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
                    ReactionMarks(marks, size = 14.dp)
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        if (score != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) { i ->
                    BrickDot(slug = quietSlug(), studSize = Stud.pip, filled = i < score)
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
