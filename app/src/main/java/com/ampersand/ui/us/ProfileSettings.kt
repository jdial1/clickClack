package com.ampersand.ui.us

import com.ampersand.ui.theme.Alpha
import com.ampersand.ui.components.ActionRow
import com.ampersand.ui.components.panel
import com.ampersand.ui.theme.Corner
import com.ampersand.ui.theme.Inset
import com.ampersand.ui.theme.Space
import kotlinx.coroutines.launch
import com.ampersand.model.DayPips
import com.ampersand.ui.components.IconAction
import com.ampersand.ui.components.IconBrick
import com.ampersand.ui.components.UiIcon
import com.ampersand.ui.notify.parseHour
import com.ampersand.ui.notify.Hours
import com.ampersand.model.QuestionSource
import com.ampersand.model.DateKind
import com.ampersand.ui.brick.glass
import com.ampersand.ui.components.AmpCheckboxGroup
import com.ampersand.model.label
import com.ampersand.model.League
import com.ampersand.model.GenderChoices
import com.ampersand.model.Dates
import com.ampersand.model.Topic
import com.ampersand.model.RelationshipStatus
import com.ampersand.ui.components.AmpField
import com.ampersand.ui.quiz.BrickChip
import androidx.compose.foundation.layout.FlowRow
import com.ampersand.model.BucketList
import com.ampersand.model.BucketItem
import com.ampersand.model.BucketGroup
import com.ampersand.model.until
import com.ampersand.model.SideBySide
import com.ampersand.data.Sources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.model.Occasion
import com.ampersand.model.timeTogether
import com.ampersand.ui.components.AmpDropdown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ampersand.model.CoupleState
import com.ampersand.ui.components.PersonDot
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.BrickPill
import com.ampersand.ui.brick.glassOn
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.structureSlug
import com.ampersand.ui.components.ColorChip
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.components.QuietNote
import com.ampersand.ui.components.pressDepth
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.LocalAppearance
import com.ampersand.ui.theme.Motion
import com.ampersand.ui.theme.ThemeMode
import com.ampersand.ui.theme.toArgbLong
import com.ampersand.ui.theme.Stud
import java.time.format.DateTimeFormatter

/** Profile sections (about you, the couple, topics) that each expand in place. All optional. */
@Composable
internal fun RelationshipProfile(
    state: CoupleState,
    onSetProfile: (String?, java.time.LocalDate?) -> Unit,
    onSetRelationship: (RelationshipStatus, java.time.LocalDate?) -> Unit,
    onSetAnniversary: ((java.time.LocalDate) -> Unit)?,
    onSetTopicHidden: (Topic, Boolean) -> Unit,
    onSetTopicInterest: (Topic, Boolean) -> Unit,
    onSetHomeZip: (String) -> Boolean,
    onSetFollowing: (Set<League>) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    fun toggle(key: String) { open = if (open == key) null else key }
    val you = BrickColor.of(state.you.colorArgb)

    SectionLabel("Your profile")
    Spacer(Modifier.height(Space.m))

    ProfileRow(
        title = "About you",
        status = listOfNotNull(state.you.gender, state.you.birthday?.format(LongDay)).joinToString("  ·  ")
            .ifEmpty { "Not added" },
        done = state.you.gender != null || state.you.birthday != null,
        open = open == "you",
    ) { toggle("you") }
    AnimatedVisibility(visible = open == "you") {
        Column(Modifier.padding(bottom = Space.l)) {
            var gender by remember { mutableStateOf(state.you.gender) }
            var birthday by remember { mutableStateOf(state.you.birthday) }
            AmpDropdown(
                options = GenderChoices,
                selected = gender,
                accent = Color(you.argb),
                placeholder = "Gender",
                onSelect = { gender = it },
            )
            Spacer(Modifier.height(Space.m))
            SectionLabel("Birthday")
            Spacer(Modifier.height(Space.s))
            DateFields(state.today.date, initial = birthday, yearsBack = 100) { birthday = it }
            Spacer(Modifier.height(Space.m))
            AmpButton(label = "Save", slug = you.slug, icon = UiIcon.DONE) {
                onSetProfile(gender, birthday)
                open = null
            }
        }
    }

    ProfileRow(
        title = "Your relationship",
        status = listOfNotNull(
            state.relationship?.label,
            state.anniversary?.let { "since ${it.format(LongDay)}" },
            state.homeZip?.let { zip ->
                "home $zip" + listOfNotNull(state.region?.label, state.localTeams).joinToString(", ").let { if (it.isEmpty()) "" else " ($it)" }
            },
        ).joinToString(", ").replaceFirstChar { it.uppercase() }.ifEmpty { "Not added" },
        done = state.relationship != null && state.anniversary != null,
        open = open == "us",
    ) { toggle("us") }
    AnimatedVisibility(visible = open == "us") {
        Column(Modifier.padding(bottom = Space.l)) {
            var status by remember { mutableStateOf(state.relationship) }
            var since by remember { mutableStateOf(state.anniversary) }
            var married by remember { mutableStateOf(state.marriedOn) }
            AmpDropdown(
                options = RelationshipStatus.entries.map { it.label },
                selected = status?.label,
                accent = Color(you.argb),
                placeholder = "Relationship status",
                onSelect = { label -> status = RelationshipStatus.entries.first { it.label == label } },
            )
            Spacer(Modifier.height(Space.m))
            SectionLabel("Home ZIP")
            Spacer(Modifier.height(Space.s))
            var zip by rememberSaveable { mutableStateOf(state.homeZip.orEmpty()) }
            var zipRefused by remember { mutableStateOf(false) }
            AmpField(
                value = zip,
                onValueChange = { zip = it.filter(Char::isDigit).take(5); zipRefused = false },
                color = you,
                placeholder = "Used for weather and local questions",
            )
            if (zipRefused) {
                Text("We don't recognize that ZIP.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(Space.m))
            SectionLabel("Together since")
            Spacer(Modifier.height(Space.s))
            DateFields(state.today.date, initial = since) { since = it }
            if (status == RelationshipStatus.MARRIED) {
                Spacer(Modifier.height(Space.m))
                SectionLabel("Married on")
                Spacer(Modifier.height(Space.s))
                DateFields(state.today.date, initial = married) { married = it }
            }
            Spacer(Modifier.height(Space.m))
            AmpButton(label = "Save", slug = you.slug, enabled = status != null || zip.length == 5, icon = UiIcon.DONE) {
                status?.let { onSetRelationship(it, married) }
                since?.takeIf { it != state.anniversary }?.let { onSetAnniversary?.invoke(it) }
                if (zip != state.homeZip.orEmpty() && zip.isNotEmpty() && !onSetHomeZip(zip)) {
                    zipRefused = true
                    return@AmpButton
                }
                open = null
            }
        }
    }

    ProfileRow(
        title = "Topics",
        status = listOfNotNull(
            state.interests.takeIf { it.isNotEmpty() }?.let { t -> "More ${t.joinToString(", ") { it.label.lowercase() }}" },
            state.hiddenTopics.takeIf { it.isNotEmpty() }?.let { t -> "leaving out ${t.joinToString(", ") { it.label.lowercase() }}" },
        ).joinToString("; ").replaceFirstChar { it.uppercase() }.ifEmpty { "All topics, no changes" },
        done = true,
        open = open == "topics",
    ) { toggle("topics") }
    AnimatedVisibility(visible = open == "topics") {
        Column(Modifier.padding(bottom = Space.l)) {
            Text(
                "More makes a topic come up more often. If either of you leaves a topic out, it's out of " +
                    "the daily question for both. Decks aren't affected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.m))
            Topic.entries.forEach { topic ->
                val hidden = topic in state.yourHiddenTopics
                val more = topic in state.yourInterests
                val theirs = when {
                    topic == Topic.SPORTS && state.following.isEmpty() && !more ->
                        "Off until one of you follows a sport below."
                    topic == Topic.HOROSCOPES && Topic.HOROSCOPES !in state.interests ->
                        "Off until one of you picks More. Uses both birthdays for your signs and Chinese zodiac animals."
                    topic == Topic.HOROSCOPES && (state.you.birthday == null || state.them.birthday == null) ->
                        "Needs both birthdays, in About you."
                    topic in state.hiddenTopics && !hidden -> "${state.them.name} left this out."
                    topic in state.interests && !more -> "${state.them.name} asked for more of this."
                    else -> null
                }
                Text(topic.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    theirs ?: topic.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.s))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    BrickChip(label = "More", on = more, color = you, onClick = {
                        if (hidden) onSetTopicHidden(topic, false)
                        onSetTopicInterest(topic, !more)
                    })
                    BrickChip(label = "As usual", on = !more && !hidden, color = you, onClick = {
                        if (more) onSetTopicInterest(topic, false)
                        if (hidden) onSetTopicHidden(topic, false)
                    })
                    BrickChip(label = "Leave out", on = hidden, color = you, onClick = {
                        if (more) onSetTopicInterest(topic, false)
                        onSetTopicHidden(topic, !hidden)
                    })
                }
                Spacer(Modifier.height(Space.l))
            }

            SectionLabel("Sports you follow")
            Spacer(Modifier.height(Space.s))
            Text(
                if (state.following.isEmpty()) "None yet, so no sports questions."
                else "You follow: ${state.following.joinToString(", ") { it.label }}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.m))
            AmpCheckboxGroup(
                options = League.entries.map { it.label },
                selected = state.yourFollowing.map { it.label }.toSet(),
                accent = Color(you.argb),
                onToggle = { label ->
                    val league = League.entries.first { it.label == label }
                    onSetFollowing(if (league in state.yourFollowing) state.yourFollowing - league else state.yourFollowing + league)
                },
            )
        }
    }
}

/** A profile row: title, status, and Add/Edit/Close. */
@Composable
internal fun ProfileRow(title: String, status: String, done: Boolean, open: Boolean, onClick: () -> Unit) {
    ActionRow(title, status, Modifier.padding(bottom = Space.m), onClick = onClick) {
        IconAction(
            icon = when {
                open -> UiIcon.CLOSE
                done -> UiIcon.WRITE
                else -> UiIcon.ADD
            },
            label = when {
                open -> "Close"
                done -> "Edit"
                else -> "Add"
            },
            color = inkOnBrick(structureSlug()),
        )
    }
}

/**
 * Yearly dates, soonest first, plus a form to add one. Birthdays, anniversary
 * and wedding come from profiles; others are added here.
 */
@Composable
internal fun DatesSection(
    state: CoupleState,
    onAdd: (String, java.time.LocalDate, DateKind) -> Unit,
) {
    val you = BrickColor.of(state.you.colorArgb)
    val upcoming = remember(state) { Dates.upcoming(state) }
    var adding by rememberSaveable { mutableStateOf(false) }

    if (upcoming.isEmpty()) {
        QuietNote("No dates yet. Add one below.")
    }
    upcoming.forEachIndexed { i, d ->
        if (i > 0) Spacer(Modifier.height(Space.s))
        DateCard(
            day = d.next,
            title = d.label,
            detail = "Since ${d.since.format(LongDay)}",
            countdown = until(state.today.date, d.next),
            brick = if (d.label.startsWith(state.them.name)) BrickColor.of(state.them.colorArgb) else you,
        )
    }
    if (upcoming.isNotEmpty()) Spacer(Modifier.height(Space.m))

    Occasion.next(state.today.date.plusDays(1), state.anniversary, state.homeState)?.let { (occasion, day) ->
        Text(
            "Next special question: ${occasion.label}, ${day.format(ShortDay)}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.m))
    }

    if (!adding) {
        AmpButton(label = "Add a date", slug = you.slug, icon = UiIcon.ADD) { adding = true }
        return
    }
    var label by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf<DateKind?>(null) }
    var date by remember { mutableStateOf<java.time.LocalDate?>(null) }
    AmpDropdown(
        options = DateKind.entries.filter { it != DateKind.HALF_BIRTHDAY }.map { it.label },
        selected = kind?.label,
        accent = Color(you.argb),
        placeholder = "Type of date",
        onSelect = { picked ->
            val k = DateKind.entries.first { it.label == picked }
            if (label.isBlank() || DateKind.entries.any { it.label == label }) label = if (k == DateKind.OTHER) "" else k.label
            kind = k
        },
    )
    Spacer(Modifier.height(Space.m))
    AmpField(value = label, onValueChange = { label = it.take(60) }, color = you, placeholder = "Name")
    Spacer(Modifier.height(Space.m))
    DateFields(state.today.date, initial = null, yearsBack = 100) { date = it }
    Spacer(Modifier.height(Space.m))
    AmpButton(label = "Save", slug = you.slug, enabled = label.isNotBlank() && date != null && kind != null, icon = UiIcon.DONE) {
        onAdd(label.trim(), date!!, kind!!)
        label = ""
        adding = false
    }
}

/**
 * Day, month and year dropdowns. Reports a date only when the three form a
 * valid date on or before today, otherwise null.
 */
@Composable
internal fun DateFields(
    today: java.time.LocalDate,
    initial: java.time.LocalDate?,
    yearsBack: Int = 60,
    onDate: (java.time.LocalDate?) -> Unit,
) {
    val months = java.time.Month.entries.map {
        it.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
    }
    var day by remember { mutableStateOf(initial?.dayOfMonth?.toString()) }
    var month by remember { mutableStateOf(initial?.let { months[it.monthValue - 1] }) }
    var year by remember { mutableStateOf(initial?.year?.toString()) }
    val accent = MaterialTheme.colorScheme.primary

    fun report() {
        // Invalid combos like Feb 31 are selectable, so parse and drop failures.
        val parsed = runCatching {
            java.time.LocalDate.of(year!!.toInt(), months.indexOf(month!!) + 1, day!!.toInt())
        }.getOrNull()?.takeIf { it <= today }
        onDate(parsed)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        AmpDropdown(
            options = (1..31).map { it.toString() }, selected = day, accent = accent, placeholder = "Day",
            modifier = Modifier.weight(1f), onSelect = { day = it; report() },
        )
        AmpDropdown(
            options = months, selected = month, accent = accent, placeholder = "Month",
            modifier = Modifier.weight(1.6f), onSelect = { month = it; report() },
        )
        AmpDropdown(
            options = (today.year downTo today.year - yearsBack).map { it.toString() }, selected = year,
            accent = accent, placeholder = "Year", modifier = Modifier.weight(1.2f), onSelect = { year = it; report() },
        )
    }
}

internal val LongDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
internal val ShortDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d")

/** A date row: day on a low brick, title, detail, and countdown. */
@Composable
internal fun DateCard(
    day: java.time.LocalDate,
    title: String,
    detail: String,
    countdown: String,
    brick: BrickColor,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        Modifier
            .fillMaxWidth()
            .panel()
            .padding(horizontal = Space.m, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrickPill(slug = brick.slug, studSize = Stud.chip, height = BrickHeight.LOW) {
            Text(
                "${day.dayOfMonth} ${day.format(ShortMonth)}",
                style = MaterialTheme.typography.labelLarge,
                color = inkOnBrick(brick.slug),
                modifier = Modifier.padding(horizontal = Space.m, vertical = Space.hair),
            )
        }
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = ink)
        }
        Spacer(Modifier.width(Space.s))
        Text(countdown, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

internal val ShortMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
