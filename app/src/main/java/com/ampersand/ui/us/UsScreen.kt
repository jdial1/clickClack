package com.ampersand.ui.us

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

/** The couple screen: header, stats, profiles, dates, bucket list and guess history. Settings are separate. */
@Composable
fun UsScreen(
    state: CoupleState,
    onSetAnniversary: ((java.time.LocalDate) -> Unit)? = null,
    /** Open a whole-instrument sitting. Null in previews. */
    onTakeInstrument: ((String) -> Unit)? = null,
    onOpenSettings: () -> Unit = {},
    onAddToBucket: (String, BucketGroup) -> Unit = { _, _ -> },
    onTickBucket: (String) -> Unit = {},
    onRemoveFromBucket: (String) -> Unit = {},
    onAddSpecialDate: (String, java.time.LocalDate, DateKind) -> Unit = { _, _, _ -> },
    onOpenDay: (com.ampersand.model.DayEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        CoupleHeader(state, onOpenSettings)

        Spacer(Modifier.height(16.dp))
        BlendStrip(you = you, them = them)

        Spacer(Modifier.height(32.dp))
        YouTwo(state)
        Spacer(Modifier.height(12.dp))
        val byDate = remember(state.history) { state.history.associateBy { it.date } }
        com.ampersand.ui.history.StudPlate(state, byDate, onOpenDay)

        Spacer(Modifier.height(32.dp))
        ProfileBadges(state = state, onTake = { onTakeInstrument?.invoke(it) })

        Spacer(Modifier.height(32.dp))
        // Plans: dates coming up, and things to do someday.
        SectionLabel("Plans")
        Spacer(Modifier.height(10.dp))
        var someday by rememberSaveable { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrickChip(label = "Coming up", on = !someday, color = you, icon = UiIcon.DATES, onClick = { someday = false })
            BrickChip(label = "Someday", on = someday, color = you, onClick = { someday = true })
        }
        Spacer(Modifier.height(14.dp))
        if (someday) {
            BucketSection(state, onAddToBucket, onTickBucket, onRemoveFromBucket)
        } else {
            DatesSection(state, onAddSpecialDate)
        }

        Spacer(Modifier.height(48.dp))
    }
}

/** Shared bucket list. Ticking an item off lays a brick for each partner on this week's plate. */
@Composable
private fun BucketSection(
    state: CoupleState,
    onAdd: (String, BucketGroup) -> Unit,
    onTick: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val you = BrickColor.of(state.you.colorArgb)
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    var group by rememberSaveable { mutableStateOf(BucketGroup.GOALS) }
    var draft by rememberSaveable { mutableStateOf("") }
    val (todo, done) = remember(state.bucket) { BucketList.ordered(state.bucket) }

    Text(
        "Things to do together. Check one off to add a brick for each of you.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BucketGroup.entries.forEach { g ->
            BrickChip(label = g.label, on = g == group, color = you, onClick = { group = g })
        }
    }
    Spacer(Modifier.height(10.dp))
    AmpField(
        value = draft,
        onValueChange = { draft = it.take(200) },
        color = you,
        placeholder = "Something to do together",
    )
    Spacer(Modifier.height(10.dp))
    AmpButton(label = "Add", slug = you.slug, enabled = draft.isNotBlank(), icon = UiIcon.ADD) {
        onAdd(draft.trim(), group)
        draft = ""
    }

    Spacer(Modifier.height(16.dp))
    if (todo.isEmpty() && done.isEmpty()) {
        QuietNote("Nothing here yet.")
        return
    }
    todo.forEach { item ->
        BucketRow(
            item = item,
            byName = if (item.byUid == state.you.uid) "you" else state.them.name,
            action = "Done",
            onAction = {
                sounds.play(Sfx.SEAL)
                feel.play(Feel.SEAL)
                onTick(item.id)
            },
            onRemove = if (item.byUid == state.you.uid) ({ onRemove(item.id) }) else null,
        )
    }
    if (done.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        SectionLabel("Done")
        Spacer(Modifier.height(8.dp))
        done.forEach { item ->
            BucketRow(item = item, byName = null, action = null, onAction = {}, onRemove = null)
        }
    }
}

@Composable
private fun BucketRow(
    item: BucketItem,
    byName: String?,
    action: String?,
    onAction: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    val slug = structureSlug()
    val ink = inkOnBrick(slug)
    BrickSlab(slugs = listOf(slug), studs = 12, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 14.dp)
                .glassOn(slug, corner = 14.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.group.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = ink)
                Text(item.text, style = MaterialTheme.typography.titleMedium, color = ink)
                Text(
                    item.doneOn?.let { "Done ${it.format(DoneDate)}" } ?: "Added by $byName",
                    style = MaterialTheme.typography.bodySmall,
                    color = ink,
                )
            }
            if (onRemove != null) {
                IconAction(UiIcon.CLOSE, "Remove", Modifier.padding(horizontal = 8.dp), color = ink, onClick = onRemove)
            }
            if (action != null) {
                BrickPill(
                    slug = "green",
                    studSize = Stud.chip,
                    height = BrickHeight.MEDIUM,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAction,
                    ),
                ) {
                    Text(
                        action,
                        style = MaterialTheme.typography.labelLarge,
                        color = inkOnBrick("green"),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

private val DoneDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

/** Both names, how long, and the way to the settings. */
@Composable
private fun CoupleHeader(state: CoupleState, onOpenSettings: () -> Unit) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                "${state.you.name} & ${state.them.name}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                state.anniversary?.let { "Together ${timeTogether(it, state.today.date).lowercase()}" }
                    ?: "Add your anniversary in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Dates.upcoming(state).firstOrNull()?.let { next ->
                Text(
                    if (next.next == state.today.date) "${next.label} is today"
                    else "${next.label} ${until(state.today.date, next.next).replaceFirstChar { it.lowercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        BrickPill(
            slug = structureSlug(),
            studSize = Stud.chip,
            height = BrickHeight.MEDIUM,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                sounds.play(Sfx.NAV)
                feel.play(Feel.NAV)
                onOpenSettings()
            },
        ) {
            IconAction(UiIcon.SETTINGS, "Settings", Modifier.padding(horizontal = 12.dp, vertical = 2.dp), color = inkOnBrick(structureSlug()))
        }
    }
}

/** You two: days each of you showed up, right guesses and questions written, side by side in your colours. */
@Composable
private fun YouTwo(state: CoupleState) {
    val you = SideBySide.of(state, state.you.uid)
    val them = SideBySide.of(state, state.them.uid)
    val weeks = DayPips.weeksActive(state)
    val youInk = Color(state.you.colorArgb)
    val themInk = Color(state.them.colorArgb)

    Column(
        Modifier
            .fillMaxWidth()
            .glass(inkOnBrick(structureSlug()), corner = 14.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text("You two", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.you.name, style = MaterialTheme.typography.labelMedium, color = youInk)
            Text("  ·  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(state.them.name, style = MaterialTheme.typography.labelMedium, color = themInk)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf(
                (if (weeks == 1) "Days this week" else "Days, $weeks weeks") to (you.daysShowedUp to them.daysShowedUp),
                "Right guesses" to (you.guessesLanded to them.guessesLanded),
                "Questions written" to ((state.written[state.you.uid] ?: 0) to (state.written[state.them.uid] ?: 0)),
            ).forEach { (label, pair) ->
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${pair.first}", style = MaterialTheme.typography.headlineSmall, color = youInk)
                        Text(" · ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("${pair.second}", style = MaterialTheme.typography.headlineSmall, color = themInk)
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}


/** Settings, opened from the Us header. Sign out and unpair sit in a collapsed Advanced section. */
@Composable
fun SettingsScreen(
    state: CoupleState,
    onPickColor: (colorArgb: Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    account: String? = null,
    onSignOut: (() -> Unit)? = null,
    onUnpair: (() -> Unit)? = null,
    onSetAnniversary: ((java.time.LocalDate) -> Unit)? = null,
    onSetProfile: (String?, java.time.LocalDate?) -> Unit = { _, _ -> },
    onSetRelationship: (RelationshipStatus, java.time.LocalDate?) -> Unit = { _, _ -> },
    onSetTopicHidden: (Topic, Boolean) -> Unit = { _, _ -> },
    onSetTopicInterest: (Topic, Boolean) -> Unit = { _, _ -> },
    onSetFollowing: (Set<League>) -> Unit = {},
    onSetHomeZip: (String) -> Boolean = { true },
    onSetSource: (QuestionSource, Boolean) -> Unit = { _, _ -> },
    onSetQuietHours: (Int, Int) -> Unit = { _, _ -> },
    onRemoveSpecialDate: (String) -> Unit = {},
    onRetake: (String) -> Unit = {},
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BrickPill(
                slug = structureSlug(),
                studSize = Stud.chip,
                height = BrickHeight.MEDIUM,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    sounds.play(Sfx.NAV)
                    feel.play(Feel.NAV)
                    onBack()
                },
            ) {
                IconAction(UiIcon.BACK, "Back", Modifier.padding(horizontal = 12.dp, vertical = 2.dp), color = inkOnBrick(structureSlug()))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(28.dp))
        RelationshipProfile(
            state, onSetProfile, onSetRelationship, onSetAnniversary, onSetTopicHidden, onSetTopicInterest, onSetHomeZip,
            onSetFollowing,
        )

        Spacer(Modifier.height(22.dp))
        SectionLabel("Your colors")
        Spacer(Modifier.height(6.dp))
        Text(
            "Shared things use both your colors.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        ColorRow(
            name = state.you.name,
            current = Color(state.you.colorArgb),
            taken = BrickColor.of(state.them.colorArgb),
            takenBy = state.them.name,
            onPick = onPickColor,
        )
        Spacer(Modifier.height(20.dp))
        // Read-only: Firestore rules refuse writes to the partner's profile.
        PartnerColor(name = state.them.name, current = Color(state.them.colorArgb))

        Spacer(Modifier.height(32.dp))
        FeedbackToggles(you = BrickColor.of(state.you.colorArgb))

        Spacer(Modifier.height(32.dp))
        SectionLabel("Appearance")
        Spacer(Modifier.height(10.dp))
        ThemeBricks(you = BrickColor.of(state.you.colorArgb))

        if (state.specialDates.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            SectionLabel("Your dates")
            Spacer(Modifier.height(10.dp))
            state.specialDates.sortedBy { it.date.dayOfYear }.forEach { d ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .glass(inkOnBrick(structureSlug()), corner = 12.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(d.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            "${d.kind.label}  ·  ${d.date.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconAction(UiIcon.CLOSE, "Remove", onClick = { onRemoveSpecialDate(d.id) })
                }
            }
        }


        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBrick(UiIcon.REMINDER)
            Spacer(Modifier.width(8.dp))
            SectionLabel("Daily reminder")
        }
        Spacer(Modifier.height(10.dp))
        var reminder by remember {
            mutableStateOf(com.ampersand.notify.Reminder.time(context)?.let { (h, _) -> Hours.firstOrNull { parseHour(it) == h } } ?: "Off")
        }
        AmpDropdown(
            options = listOf("Off") + Hours,
            selected = reminder,
            accent = Color(state.you.colorArgb),
            onSelect = { picked ->
                reminder = picked
                if (picked == "Off") com.ampersand.notify.Reminder.cancel(context)
                else com.ampersand.notify.Reminder.schedule(context, parseHour(picked), 0)
            },
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Quiet hours")
        Spacer(Modifier.height(6.dp))
        Text(
            "No notifications in these hours. Anything that comes in waits and arrives together.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AmpDropdown(
                options = HourLabels,
                selected = HourLabels[state.quietFrom],
                accent = Color(state.you.colorArgb),
                placeholder = "From",
                modifier = Modifier.weight(1f),
                onSelect = { onSetQuietHours(HourLabels.indexOf(it), state.quietTo) },
            )
            AmpDropdown(
                options = HourLabels,
                selected = HourLabels[state.quietTo],
                accent = Color(state.you.colorArgb),
                placeholder = "Until",
                modifier = Modifier.weight(1f),
                onSelect = { onSetQuietHours(state.quietFrom, HourLabels.indexOf(it)) },
            )
        }

        Spacer(Modifier.height(32.dp))
        Credits()

        if (onSignOut != null) {
            Spacer(Modifier.height(32.dp))
            // Collapsed by default so sign out and unpair aren't hit by accident.
            var advanced by rememberSaveable { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { advanced = !advanced }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Advanced", Modifier.weight(1f))
                IconAction(UiIcon.SHOW_MORE, if (advanced) "Hide" else "Show", turned = advanced)
            }
            AnimatedVisibility(visible = advanced) {
            Column {
            Spacer(Modifier.height(10.dp))
            if (account != null) {
                Text(
                    text = account,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                "Version ${com.ampersand.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            SectionLabel("Question sources")
            Spacer(Modifier.height(10.dp))
            QuestionSource.entries.forEach { source ->
                SwitchSlab(
                    on = source !in state.sourcesOff,
                    title = { source.label + if (it) " on" else " off" },
                    subtitle = source.detail,
                    you = BrickColor.of(state.you.colorArgb),
                ) { onSetSource(source, source in state.sourcesOff) }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Topics")
            Spacer(Modifier.height(10.dp))
            AmpButton(label = "Reset topics and sports", slug = "black") {
                state.yourHiddenTopics.forEach { onSetTopicHidden(it, false) }
                state.yourInterests.forEach { onSetTopicInterest(it, false) }
                onSetFollowing(emptySet())
            }

            val done = com.ampersand.data.BadgeWall.groups(state.instrumentRuns, state.you, state.them).filter { !it.yourTurn }
            if (done.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("Retake a test")
                Spacer(Modifier.height(10.dp))
                done.forEach { g ->
                    // Compact glass rows, not full bricks: six tall buttons were a screen of bricks.
                    IconAction(
                        UiIcon.RETAKE,
                        g.title,
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .glass(inkOnBrick(structureSlug()), corner = 12.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        onClick = { onRetake(g.instrumentId) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            // Black (structure color), not the user's color.
            AmpButton(label = "Sign out", slug = "black", icon = UiIcon.SIGN_OUT, onClick = onSignOut)

            if (onUnpair != null) {
                Spacer(Modifier.height(28.dp))
                SectionLabel("${state.you.name} and ${state.them.name}")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Unpairing ends it for both of you. Nothing is deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                // Two taps, since unpairing affects both phones.
                var confirming by remember { mutableStateOf(false) }
                AmpButton(
                    label = if (confirming) "Tap again to unpair" else "Unpair",
                    slug = "black",
                    onClick = { if (confirming) onUnpair() else confirming = true },
                )
            }
            }
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

/** Source credits. Required: CC BY and MIT sources must be credited where users can see them. */
@Composable
private fun Credits() {
    SectionLabel("Credits")
    Spacer(Modifier.height(10.dp))
    Sources.all.forEach { source ->
        Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Text(
                "${source.authors} (${source.year})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                source.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                source.licence,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                source.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorRow(
    name: String,
    current: Color,
    taken: BrickColor,
    takenBy: String,
    onPick: (Long) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonDot(name, current, size = 26)
            Spacer(Modifier.size(10.dp))
            Text(name, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BrickColor.entries.forEach { choice ->
                val selected = Color(choice.argb) == current
                // Partner's color shows their initial and can't be picked.
                val theirs = choice == taken
                Box(
                    Modifier
                        .clickable(enabled = !theirs) { onPick(choice.argb) }
                        .padding(4.dp),
                ) {
                    ColorChip(
                        label = when {
                            theirs -> takenBy.take(1).uppercase()
                            selected -> "\u2022"
                            else -> " "
                        },
                        color = choice,
                        studSize = if (selected) 24.dp else 20.dp,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${taken.label} is taken by $takenBy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


/** Separate sound and vibration toggles; each confirms itself with the other channel when needed. */
@Composable
private fun FeedbackToggles(you: BrickColor) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    SwitchSlab(
        on = sounds?.enabled ?: false,
        title = { if (it) "Sound on" else "Sound off" },
        subtitle = "Taps and clicks.",
        you = you,
    ) {
        sounds?.let { it.enabled = !it.enabled }
        // Toggle first so turning sound on plays the confirming click.
        if (sounds?.enabled == true) sounds.play(Sfx.SELECT)
        feel.play(if (sounds?.enabled == true) Feel.SELECT else Feel.DESELECT)
    }

    Spacer(Modifier.height(12.dp))

    SwitchSlab(
        on = feel?.enabled ?: false,
        title = { if (it) "Vibration on" else "Vibration off" },
        subtitle = "A buzz when you tap.",
        you = you,
    ) {
        feel?.let { it.enabled = !it.enabled }
        val nowOn = feel?.enabled == true
        sounds.play(if (nowOn) Sfx.SELECT else Sfx.DESELECT)
        // Only turning vibration on can confirm by feel.
        if (nowOn) feel.play(Feel.SELECT)
    }
}

/** Theme picker: one brick per [ThemeMode] (light, dark, system). */
@Composable
private fun ThemeBricks(you: BrickColor) {
    val appearance = LocalAppearance.current
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val current = appearance?.mode ?: ThemeMode.SYSTEM

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            val on = mode == current
            val lift by animateFloatAsState(if (on) 1f else 0f, Motion.glide, label = "themeLift")

            BrickPill(
                slug = if (on) you.slug else structureSlug(),
                studSize = Stud.chip,
                height = BrickHeight.MEDIUM,
                alpha = 0.55f + 0.45f * lift,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (!on) {
                        sounds.play(Sfx.SELECT)
                        feel.play(Feel.SELECT)
                        appearance?.set(mode)
                    }
                },
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = inkOnBrick(if (on) you.slug else structureSlug()),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** "12 am" through "11 pm", indexed by hour. */
private val HourLabels = (0..23).map { h ->
    val twelve = if (h % 12 == 0) 12 else h % 12
    "$twelve ${if (h < 12) "am" else "pm"}"
}

/** One full-width brick that is either lit or not. */
@Composable
private fun SwitchSlab(
    on: Boolean,
    title: (Boolean) -> String,
    subtitle: String,
    you: BrickColor,
    onToggle: () -> Unit,
) {
    val accent = you.slug
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    BrickSlab(
        slugs = listOf(if (on) accent else null),
        studs = 12,
        modifier = Modifier
            .fillMaxWidth()
            .pressDepth(pressed)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onToggle,
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 14.dp)
                .glassOn(if (on) accent else null, corner = 14.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title(on),
                    style = MaterialTheme.typography.titleSmall,
                    color = inkOnBrick(if (on) accent else null),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = inkOnBrick(if (on) accent else null).copy(alpha = 0.75f),
                )
            }
        }
    }
}

/** One brick course in both partners' colors. */
@Composable
private fun BlendStrip(you: BrickColor, them: BrickColor) {
    // No content, so the height must be set explicitly.
    BrickSlab(
        slugs = listOf(you.slug, them.slug),
        studs = 12,
        modifier = Modifier.height(34.dp),
    )
}

@Preview(name = "Us", showBackground = true, heightDp = 900)
@Composable
private fun PreviewUs() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        UsScreen(MockCouple.state())
    }
}

@Preview(name = "Us - dark", showBackground = true, heightDp = 900)
@Composable
private fun PreviewUsDark() = AmpersandTheme(darkTheme = true) {
    Surface(color = MaterialTheme.colorScheme.background) {
        UsScreen(MockCouple.state())
    }
}


/** Partner's color, read-only. */
@Composable
private fun PartnerColor(name: String, current: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PersonDot(name, current, size = 28)
        Spacer(Modifier.size(10.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Only $name can change this",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        ColorChip(label = name.take(1).uppercase(), color = BrickColor.of(current.toArgbLong()))
    }
}

/** Profile sections (about you, the couple, topics) that each expand in place. All optional. */
@Composable
private fun RelationshipProfile(
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
    Spacer(Modifier.height(10.dp))

    ProfileRow(
        title = "About you",
        status = listOfNotNull(state.you.gender, state.you.birthday?.format(LongDay)).joinToString("  ·  ")
            .ifEmpty { "Not added" },
        done = state.you.gender != null || state.you.birthday != null,
        open = open == "you",
    ) { toggle("you") }
    AnimatedVisibility(visible = open == "you") {
        Column(Modifier.padding(bottom = 16.dp)) {
            var gender by remember { mutableStateOf(state.you.gender) }
            var birthday by remember { mutableStateOf(state.you.birthday) }
            AmpDropdown(
                options = GenderChoices,
                selected = gender,
                accent = Color(you.argb),
                placeholder = "Gender",
                onSelect = { gender = it },
            )
            Spacer(Modifier.height(12.dp))
            SectionLabel("Birthday")
            Spacer(Modifier.height(6.dp))
            DateFields(state.today.date, initial = birthday, yearsBack = 100) { birthday = it }
            Spacer(Modifier.height(12.dp))
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
        Column(Modifier.padding(bottom = 16.dp)) {
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
            Spacer(Modifier.height(12.dp))
            SectionLabel("Home ZIP")
            Spacer(Modifier.height(6.dp))
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
            Spacer(Modifier.height(12.dp))
            SectionLabel("Together since")
            Spacer(Modifier.height(6.dp))
            DateFields(state.today.date, initial = since) { since = it }
            if (status == RelationshipStatus.MARRIED) {
                Spacer(Modifier.height(12.dp))
                SectionLabel("Married on")
                Spacer(Modifier.height(6.dp))
                DateFields(state.today.date, initial = married) { married = it }
            }
            Spacer(Modifier.height(12.dp))
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
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(
                "More makes a topic come up more often. If either of you leaves a topic out, it's out of " +
                    "the daily question for both. Decks aren't affected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
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
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Spacer(Modifier.height(16.dp))
            }

            SectionLabel("Sports you follow")
            Spacer(Modifier.height(6.dp))
            Text(
                if (state.following.isEmpty()) "None yet, so no sports questions."
                else "You follow: ${state.following.joinToString(", ") { it.label }}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
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
private fun ProfileRow(title: String, status: String, done: Boolean, open: Boolean, onClick: () -> Unit) {
    val slug = structureSlug()
    val ink = inkOnBrick(slug)
    BrickSlab(
        slugs = listOf(slug),
        studs = 12,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 14.dp)
                .glassOn(slug, corner = 14.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = ink)
                Text(status, style = MaterialTheme.typography.bodySmall, color = ink)
            }
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
                color = ink,
            )
        }
    }
}

/**
 * Yearly dates, soonest first, plus a form to add one. Birthdays, anniversary
 * and wedding come from profiles; others are added here.
 */
@Composable
private fun DatesSection(
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
        if (i > 0) Spacer(Modifier.height(8.dp))
        DateCard(
            day = d.next,
            title = d.label,
            detail = "Since ${d.since.format(LongDay)}",
            countdown = until(state.today.date, d.next),
            brick = if (d.label.startsWith(state.them.name)) BrickColor.of(state.them.colorArgb) else you,
        )
    }
    if (upcoming.isNotEmpty()) Spacer(Modifier.height(12.dp))

    Occasion.next(state.today.date.plusDays(1), state.anniversary, state.homeState)?.let { (occasion, day) ->
        Text(
            "Next special question: ${occasion.label}, ${day.format(ShortDay)}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
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
    Spacer(Modifier.height(10.dp))
    AmpField(value = label, onValueChange = { label = it.take(60) }, color = you, placeholder = "Name")
    Spacer(Modifier.height(10.dp))
    DateFields(state.today.date, initial = null, yearsBack = 100) { date = it }
    Spacer(Modifier.height(12.dp))
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
private fun DateFields(
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

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

private val LongDay: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
private val ShortDay: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")

/** A date row: day on a low brick, title, detail, and countdown. */
@Composable
private fun DateCard(
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
            .glass(inkOnBrick(structureSlug()), corner = 12.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrickPill(slug = brick.slug, studSize = Stud.chip, height = BrickHeight.LOW) {
            Text(
                "${day.dayOfMonth} ${day.format(ShortMonth)}",
                style = MaterialTheme.typography.labelLarge,
                color = inkOnBrick(brick.slug),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = ink)
        }
        Spacer(Modifier.width(8.dp))
        Text(countdown, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

private val ShortMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
