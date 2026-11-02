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
    onDeleteAccount: (suspend () -> Result<Unit>)? = null,
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
            .padding(horizontal = Inset.screen),
    ) {
        Spacer(Modifier.height(Space.l))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            com.ampersand.ui.components.BackAction(onBack)
            Spacer(Modifier.width(Space.m))
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(Space.xxl))
        RelationshipProfile(
            state, onSetProfile, onSetRelationship, onSetAnniversary, onSetTopicHidden, onSetTopicInterest, onSetHomeZip,
            onSetFollowing,
        )

        Spacer(Modifier.height(Space.xl))
        SectionLabel("Your colors")
        Spacer(Modifier.height(Space.s))
        Text(
            "Shared things use both your colors.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.l))
        ColorRow(
            name = state.you.name,
            current = Color(state.you.colorArgb),
            taken = BrickColor.of(state.them.colorArgb),
            takenBy = state.them.name,
            onPick = onPickColor,
        )
        Spacer(Modifier.height(Space.xl))
        // Read-only: Firestore rules refuse writes to the partner's profile.
        PartnerColor(name = state.them.name, current = Color(state.them.colorArgb))

        Spacer(Modifier.height(Space.xxl))
        FeedbackToggles(you = BrickColor.of(state.you.colorArgb))

        Spacer(Modifier.height(Space.xxl))
        SectionLabel("Appearance")
        Spacer(Modifier.height(Space.m))
        ThemeBricks(you = BrickColor.of(state.you.colorArgb))

        if (state.specialDates.isNotEmpty()) {
            Spacer(Modifier.height(Space.xxl))
            SectionLabel("Your dates")
            Spacer(Modifier.height(Space.m))
            state.specialDates.sortedBy { it.date.dayOfYear }.forEach { d ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = Space.s)
                        .panel()
                        .padding(horizontal = Space.m, vertical = Space.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(d.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            "${d.kind.label}  ·  ${d.date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconAction(UiIcon.CLOSE, "Remove", onClick = { onRemoveSpecialDate(d.id) })
                }
            }
        }


        Spacer(Modifier.height(Space.xxl))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBrick(UiIcon.REMINDER)
            Spacer(Modifier.width(Space.s))
            SectionLabel("Daily reminder")
        }
        Spacer(Modifier.height(Space.m))
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

        Spacer(Modifier.height(Space.xl))
        SectionLabel("Quiet hours")
        Spacer(Modifier.height(Space.s))
        Text(
            "No notifications in these hours. Anything that comes in waits and arrives together.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Space.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
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

        Spacer(Modifier.height(Space.xxl))
        Credits()

        if (onSignOut != null) {
            Spacer(Modifier.height(Space.xxl))
            // Collapsed by default so sign out and unpair aren't hit by accident.
            var advanced by rememberSaveable { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { advanced = !advanced }
                    .padding(vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Advanced", Modifier.weight(1f))
                IconAction(UiIcon.SHOW_MORE, if (advanced) "Hide" else "Show", turned = advanced)
            }
            AnimatedVisibility(visible = advanced) {
            Column {
            Spacer(Modifier.height(Space.m))
            if (account != null) {
                Text(
                    text = account,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.m))
            }
            Text(
                "Version ${com.ampersand.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.l))

            SectionLabel("Question sources")
            Spacer(Modifier.height(Space.m))
            QuestionSource.entries.forEach { source ->
                SwitchSlab(
                    on = source !in state.sourcesOff,
                    title = { source.label + if (it) " on" else " off" },
                    subtitle = source.detail,
                    you = BrickColor.of(state.you.colorArgb),
                ) { onSetSource(source, source in state.sourcesOff) }
                Spacer(Modifier.height(Space.s))
            }

            Spacer(Modifier.height(Space.l))
            SectionLabel("Topics")
            Spacer(Modifier.height(Space.m))
            AmpButton(label = "Reset topics and sports", slug = "black") {
                state.yourHiddenTopics.forEach { onSetTopicHidden(it, false) }
                state.yourInterests.forEach { onSetTopicInterest(it, false) }
                onSetFollowing(emptySet())
            }

            val done = com.ampersand.data.BadgeWall.groups(state.instrumentRuns, state.you, state.them).filter { !it.yourTurn }
            if (done.isNotEmpty()) {
                Spacer(Modifier.height(Space.l))
                SectionLabel("Retake a test")
                Spacer(Modifier.height(Space.m))
                done.forEach { g ->
                    // Compact glass rows, not full bricks: six tall buttons were a screen of bricks.
                    IconAction(
                        UiIcon.RETAKE,
                        g.title,
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = Space.s)
                            .panel()
                            .padding(horizontal = Space.m, vertical = Space.s),
                        onClick = { onRetake(g.instrumentId) },
                    )
                }
            }

            Spacer(Modifier.height(Space.l))
            // Black (structure color), not the user's color.
            AmpButton(label = "Sign out", slug = "black", icon = UiIcon.SIGN_OUT, onClick = onSignOut)

            if (onUnpair != null) {
                Spacer(Modifier.height(Space.xxl))
                SectionLabel("${state.you.name} and ${state.them.name}")
                Spacer(Modifier.height(Space.s))
                Text(
                    "Unpairing ends it for both of you. Nothing is deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.m))
                // Two taps, since unpairing affects both phones.
                var confirming by remember { mutableStateOf(false) }
                AmpButton(
                    label = if (confirming) "Tap again to unpair" else "Unpair",
                    slug = "black",
                    onClick = { if (confirming) onUnpair() else confirming = true },
                )
            }

            if (onDeleteAccount != null) {
                Spacer(Modifier.height(Space.xxl))
                SectionLabel("Delete account")
                Spacer(Modifier.height(Space.s))
                Text(
                    "Deletes your account, answers, messages and photos for good. Your pairing ends, and ${state.them.name} keeps only their own answers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.m))
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                var confirming by remember { mutableStateOf(false) }
                var deleting by remember { mutableStateOf(false) }
                var failed by remember { mutableStateOf<String?>(null) }
                AmpButton(
                    label = when {
                        deleting -> "Deleting"
                        confirming -> "Tap again to delete for good"
                        else -> "Delete my account"
                    },
                    slug = "black",
                    enabled = !deleting,
                    onClick = {
                        if (!confirming) { confirming = true; return@AmpButton }
                        deleting = true
                        failed = null
                        scope.launch {
                            onDeleteAccount().onFailure { failed = it.message ?: "Couldn't delete your account. Try again." }
                            deleting = false
                        }
                    },
                )
                failed?.let {
                    Spacer(Modifier.height(Space.s))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            }
            }
        }

        Spacer(Modifier.height(Space.page))
    }
}

/** Source credits. Required: CC BY and MIT sources must be credited where users can see them. */
@Composable
internal fun Credits() {
    SectionLabel("Credits")
    Spacer(Modifier.height(Space.m))
    Sources.all.forEach { source ->
        Column(Modifier.fillMaxWidth().padding(bottom = Space.m)) {
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
internal fun ColorRow(
    name: String,
    current: Color,
    taken: BrickColor,
    takenBy: String,
    onPick: (Long) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonDot(name, current, size = 26)
            Spacer(Modifier.size(Space.m))
            Text(name, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Space.m))
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
                        .padding(Space.xs),
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
        Spacer(Modifier.height(Space.s))
        Text(
            "${taken.label} is taken by $takenBy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


/** Separate sound and vibration toggles; each confirms itself with the other channel when needed. */
@Composable
internal fun FeedbackToggles(you: BrickColor) {
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

    Spacer(Modifier.height(Space.m))

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
internal fun ThemeBricks(you: BrickColor) {
    val appearance = LocalAppearance.current
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val current = appearance?.mode ?: ThemeMode.SYSTEM

    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        ThemeMode.entries.forEach { mode ->
            val on = mode == current
            val lift by animateFloatAsState(if (on) 1f else 0f, Motion.glide, label = "themeLift")

            BrickPill(
                slug = if (on) you.slug else structureSlug(),
                studSize = Stud.chip,
                height = BrickHeight.MEDIUM,
                alpha = Alpha.rise(Alpha.quiet, lift),
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
                    modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s),
                )
            }
        }
    }
}

/** "12 am" through "11 pm", indexed by hour. */
internal val HourLabels = (0..23).map { h ->
    val twelve = if (h % 12 == 0) 12 else h % 12
    "$twelve ${if (h < 12) "am" else "pm"}"
}

/** One full-width brick that is either lit or not. */
@Composable
internal fun SwitchSlab(
    on: Boolean,
    title: (Boolean) -> String,
    subtitle: String,
    you: BrickColor,
    onToggle: () -> Unit,
) {
    ActionRow(title(on), subtitle, slug = if (on) you.slug else null, onClick = onToggle)
}

/** One brick course in both partners' colors. */
@Composable
internal fun BlendStrip(you: BrickColor, them: BrickColor) {
    // No content, so the height must be set explicitly.
    BrickSlab(
        slugs = listOf(you.slug, them.slug),
        studs = 12,
        modifier = Modifier.height(34.dp),
    )
}

/** Partner's color, read-only. */
@Composable
internal fun PartnerColor(name: String, current: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PersonDot(name, current, size = 28)
        Spacer(Modifier.size(Space.m))
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
