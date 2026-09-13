package com.ampersand.ui.us

import com.ampersand.ui.components.AmpField
import com.ampersand.ui.quiz.BrickChip
import androidx.compose.foundation.layout.FlowRow
import com.ampersand.model.BucketList
import com.ampersand.model.BucketItem
import com.ampersand.model.BucketGroup
import androidx.compose.ui.text.style.TextAlign
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
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.quietSlug
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

/**
 * The two of you: who, how long, what each of you has put in, and what is
 * coming up.
 *
 * Everything that is a setting rather than a fact about the couple -- colours,
 * sound, appearance, credits, signing out -- is one tap away behind Settings,
 * so this screen is about the people and not the phone.
 */
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
        SideBySideTable(state)

        Spacer(Modifier.height(32.dp))
        Anniversary(state, onSetAnniversary)

        Spacer(Modifier.height(32.dp))
        BucketSection(state, onAddToBucket, onTickBucket, onRemoveFromBucket)

        Spacer(Modifier.height(32.dp))
        Knowing(state)


        Spacer(Modifier.height(48.dp))
    }
}

/**
 * The list of things the two of you want to do someday.
 *
 * Add in one line, tick off with one tap. Ticking lays a brick for each of you
 * on this week's plate, which is the point: the list is the one place in the
 * app where something you said turns into something you did.
 */
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

    SectionLabel("Someday")
    Spacer(Modifier.height(6.dp))
    Text(
        "Things to do together. Ticking one off lays a brick for each of you.",
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
    AmpButton(label = "Add to the list", slug = you.slug, enabled = draft.isNotBlank()) {
        onAdd(draft.trim(), group)
        draft = ""
    }

    Spacer(Modifier.height(16.dp))
    if (todo.isEmpty() && done.isEmpty()) {
        QuietNote("Nothing on it yet.")
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
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink,
                    modifier = Modifier
                        .clickable(onClick = onRemove)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
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
                    ?: "Add the day you started, below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            Text(
                "Settings",
                style = MaterialTheme.typography.labelLarge,
                color = inkOnBrick(structureSlug()),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * The same three counts for each of you, one row each, in your own colour.
 *
 * Counts of what you each did. There is no "won" column: the app does not keep
 * score of one of you against the other.
 */
@Composable
private fun SideBySideTable(state: CoupleState) {
    val rows = listOf(state.you, state.them).map { it to SideBySide.of(state, it.uid) }
    val headers = listOf("Days", "Guesses landed", "Decks")

    SectionLabel("Side by side")
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(NameColumn))
        headers.forEach { label ->
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    rows.forEach { (member, tally) ->
        val brick = BrickColor.of(member.colorArgb)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.width(NameColumn), verticalAlignment = Alignment.CenterVertically) {
                PersonDot(member.name, Color(member.colorArgb), size = 26)
            }
            listOf(tally.daysShowedUp, tally.guessesLanded, tally.decksFinished).forEach { n ->
                Box(Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    BrickPill(slug = brick.slug, studSize = Stud.chip, height = BrickHeight.MEDIUM) {
                        Text(
                            n.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = inkOnBrick(brick.slug),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
    Text(
        "Days either of you answered or finished a deck, out of the last ten weeks.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val NameColumn = 44.dp

/**
 * Everything on the phone rather than about the two of you.
 *
 * Reached from the Us header. Leaving (sign out, unpair) stays folded away at
 * the bottom, closed by default.
 */
@Composable
fun SettingsScreen(
    state: CoupleState,
    onPickColor: (colorArgb: Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    account: String? = null,
    onSignOut: (() -> Unit)? = null,
    onUnpair: (() -> Unit)? = null,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
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
                Text(
                    "Back",
                    style = MaterialTheme.typography.labelLarge,
                    color = inkOnBrick(structureSlug()),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("Your colors")
        Spacer(Modifier.height(6.dp))
        Text(
            "Everything you share is painted in the blend of the two.",
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
        // Theirs is shown, not offered. A colour belongs to the person whose
        // bricks it paints, and the rules refuse a write to their profile
        // anyway -- so a picker here was a control that could only fail.
        PartnerColor(name = state.them.name, current = Color(state.them.colorArgb))

        Spacer(Modifier.height(32.dp))
        FeedbackToggles(you = BrickColor.of(state.you.colorArgb))

        Spacer(Modifier.height(32.dp))
        SectionLabel("Appearance")
        Spacer(Modifier.height(10.dp))
        ThemeBricks(you = BrickColor.of(state.you.colorArgb))

        Spacer(Modifier.height(32.dp))
        Credits()

        if (onSignOut != null) {
            Spacer(Modifier.height(32.dp))
            // Leaving lives behind a fold, closed by default. These are the two
            // controls on the screen nobody should meet by scrolling past.
            var advanced by rememberSaveable { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { advanced = !advanced }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Advanced", Modifier.weight(1f))
                Text(
                    if (advanced) "Hide" else "Show",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
            // Black, not the user's colour. Leaving is structure, not a choice
            // -- the same reason the nav joiners are black.
            AmpButton(label = "Sign out", slug = "black", onClick = onSignOut)

            if (onUnpair != null) {
                Spacer(Modifier.height(28.dp))
                SectionLabel("${state.you.name} and ${state.them.name}")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Unpairing ends it for both of you. Nothing you have made is deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                // Two taps. It ends the couple on both phones, and one stray
                // tap on a settings screen should not be able to do that.
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

/**
 * Where the questions and tests came from.
 *
 * Required, not decoration: the CC BY and MIT sources need their credit where
 * the people using the app can see it, and the rest are named because they
 * earned it.
 */
@Composable
private fun Credits() {
    SectionLabel("Where the questions come from")
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
                // Your partner's colour is shown with their initial and cannot
                // be picked: two people in one colour is one person on the wall.
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
            "${taken.label} is $takenBy's.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * How well you have known each other, kept.
 *
 * Every day already worked this out and then threw it away: you saw "nailed
 * it" on Tuesday and by Friday it was gone. Six months of that is the only
 * thing in the app that could say something neither of you would have noticed
 * -- that one of you is better at this, and by how much, and whether it is
 * changing.
 *
 * Two rows of studs per month rather than a percentage, for the same reason
 * the deck result is two towers: the lopsided months are the interesting ones
 * and one number hides exactly that.
 */
@Composable
private fun Knowing(state: CoupleState) {
    val knowing = state.knowing
    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)

    SectionLabel("How well you know each other")
    Spacer(Modifier.height(10.dp))

    if (knowing.isEmpty) {
        QuietNote(
            "Nothing to count yet. Every day the two of you both answer a " +
                "scored question adds one to this.",
        )
        return
    }

    Text(
        text = "${state.you.name} ${knowing.youHits}  ·  " +
            "${state.them.name} ${knowing.theyHits}  ·  of ${knowing.scored}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = verdict(state, knowing),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(18.dp))
    knowing.recent.take(6).forEach { month ->
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(
                month.month.format(MonthLabel),
                modifier = Modifier.width(64.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StudBar(month.youHits, month.scored, you)
                StudBar(month.theyHits, month.scored, them)
            }
        }
    }
}

/**
 * Who is ahead, said once, in a sentence rather than a league table.
 *
 * Deliberately generous when it is close: a scoreboard that declares a winner
 * over three guesses is a scoreboard people start playing to.
 */
@Composable
private fun verdict(state: CoupleState, knowing: com.ampersand.model.Knowing): String {
    val gap = knowing.youHits - knowing.theyHits
    val share = knowing.scored.takeIf { it > 0 } ?: return ""
    return when {
        kotlin.math.abs(gap) <= share / 10 -> "About even, which is rarer than it sounds."
        gap > 0 -> "${state.you.name} calls it more often."
        else -> "${state.them.name} calls it more often."
    }
}

/** Ten studs, filled to the share. A bar made of the thing the app is made of. */
@Composable
private fun StudBar(hits: Int, of: Int, color: BrickColor) {
    val lit = if (of == 0) 0 else ((hits * StudsInBar + of / 2) / of).coerceIn(0, StudsInBar)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(StudsInBar) { i ->
            BrickDot(
                slug = if (i < lit) color.slug else quietSlug(),
                studSize = Stud.pip,
                filled = i < lit,
            )
        }
    }
}

private const val StudsInBar = 10
private val MonthLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")

/**
 * The two switches worth having on this screen.
 *
 * Two rather than one, because the pair is wanted in every combination: a phone
 * face-down in a quiet room still wants to feel the brick land, and someone who
 * finds vibration unpleasant still wants the click. Keeping them apart also
 * closes a small dead end -- turning *sound* off could never confirm itself, for
 * the obvious reason, and now the phone answers by feel instead.
 */
@Composable
private fun FeedbackToggles(you: BrickColor) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current

    SwitchSlab(
        on = sounds?.enabled ?: false,
        title = { if (it) "Sound on" else "Sound off" },
        subtitle = "Clicks, snaps and the seal.",
        you = you,
    ) {
        sounds?.let { it.enabled = !it.enabled }
        // Toggled before the confirming click plays, so turning sound *on* is
        // audible; the other order would silently enable it and feel broken.
        if (sounds?.enabled == true) sounds.play(Sfx.SELECT)
        feel.play(if (sounds?.enabled == true) Feel.SELECT else Feel.DESELECT)
    }

    Spacer(Modifier.height(12.dp))

    SwitchSlab(
        on = feel?.enabled ?: false,
        title = { if (it) "Vibration on" else "Vibration off" },
        subtitle = "The weight of a brick going down.",
        you = you,
    ) {
        feel?.let { it.enabled = !it.enabled }
        val nowOn = feel?.enabled == true
        sounds.play(if (nowOn) Sfx.SELECT else Sfx.DESELECT)
        // Only the *on* direction can confirm itself here, the mirror of sound.
        if (nowOn) feel.play(Feel.SELECT)
    }
}

/**
 * Light, dark, or the phone's own mind.
 *
 * Three bricks rather than a switch, because there are genuinely three answers
 * and a two-way toggle has to swallow one of them. "Automatic" is not a
 * fallback here -- a phone that goes dark at sunset is a preference, and an app
 * that overrides it on the first tap is one you set back the next evening.
 */
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

/** Shows the two colours meeting: one course of both. */
@Composable
private fun BlendStrip(you: BrickColor, them: BrickColor) {
    // Was a rounded gradient bar. A real course of both colours says the same
    // thing and is made of the same bricks as everything else.
    // No content, so the slab has nothing to take its height from.
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


/** Their colour, shown. Only they can change it. */
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
                text = "Theirs to pick",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        ColorChip(label = name.take(1).uppercase(), color = BrickColor.of(current.toArgbLong()))
    }
}

/**
 * The day the two of you started, and how long that has been.
 *
 * Built from three of our own dropdowns rather than a platform date picker.
 * A Material dialog would be quicker and would look like a different app --
 * and these are the same control the questions use, so there is one less thing
 * to learn.
 */
@Composable
private fun Anniversary(state: CoupleState, onSet: ((java.time.LocalDate) -> Unit)?) {
    val since = state.anniversary

    SectionLabel("Dates")
    Spacer(Modifier.height(10.dp))

    if (since != null) {
        val nextAnniversary = since.withYear(state.today.date.year).let {
            if (it < state.today.date) it.plusYears(1) else it
        }
        DateCard(
            day = nextAnniversary,
            title = "Your anniversary",
            detail = "Since ${since.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy"))}",
            countdown = until(state.today.date, nextAnniversary),
            brick = BrickColor.of(state.you.colorArgb),
        )
        Occasion.next(state.today.date.plusDays(1), since)
            ?.takeIf { (_, day) -> day != nextAnniversary }
            ?.let { (occasion, day) ->
                Spacer(Modifier.height(10.dp))
                DateCard(
                    day = day,
                    title = occasion.label,
                    detail = "It gets its own question that day.",
                    countdown = until(state.today.date, day),
                    brick = BrickColor.of(state.them.colorArgb),
                )
            }
        return
    }

    if (onSet == null) return

    Text(
        "Tell us when you started and this day gets its own questions every year.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(14.dp))

    val months = java.time.Month.entries.map {
        it.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
    }
    val thisYear = state.today.date.year
    var day by remember { mutableStateOf<String?>(null) }
    var month by remember { mutableStateOf<String?>(null) }
    var year by remember { mutableStateOf<String?>(null) }
    val accent = MaterialTheme.colorScheme.primary

    AmpDropdown(
        options = (1..31).map { it.toString() },
        selected = day,
        accent = accent,
        placeholder = "Day",
        onSelect = { day = it },
    )
    Spacer(Modifier.height(10.dp))
    AmpDropdown(
        options = months,
        selected = month,
        accent = accent,
        placeholder = "Month",
        onSelect = { month = it },
    )
    Spacer(Modifier.height(10.dp))
    AmpDropdown(
        options = (thisYear downTo thisYear - 60).map { it.toString() },
        selected = year,
        accent = accent,
        placeholder = "Year",
        onSelect = { year = it },
    )

    Spacer(Modifier.height(14.dp))
    // Parsed rather than trusted: the thirty-first of February is selectable
    // from three independent dropdowns and is not a date.
    val parsed = runCatching {
        java.time.LocalDate.of(
            year!!.toInt(),
            months.indexOf(month!!) + 1,
            day!!.toInt(),
        )
    }.getOrNull()

    AmpButton(
        label = "Save",
        slug = BrickColor.of(state.you.colorArgb).slug,
        enabled = parsed != null && parsed <= state.today.date,
    ) { parsed?.let(onSet) }

    if (day != null && month != null && year != null && parsed == null) {
        Spacer(Modifier.height(8.dp))
        Text(
            "That is not a day.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** A date as a brick: the day and month on the left, what it is and how far off. */
@Composable
private fun DateCard(
    day: java.time.LocalDate,
    title: String,
    detail: String,
    countdown: String,
    brick: BrickColor,
) {
    BrickSlab(slugs = listOf(structureSlug()), studs = 12, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 14.dp)
                .glassOn(structureSlug(), corner = 14.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrickPill(slug = brick.slug, studSize = Stud.chip, height = BrickHeight.HIGH) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, color = inkOnBrick(brick.slug))
                    Text(day.format(ShortMonth), style = MaterialTheme.typography.labelMedium, color = inkOnBrick(brick.slug))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = inkOnBrick(structureSlug()))
                Text(detail, style = MaterialTheme.typography.bodySmall, color = inkOnBrick(structureSlug()))
                Spacer(Modifier.height(4.dp))
                Text(countdown, style = MaterialTheme.typography.labelLarge, color = inkOnBrick(structureSlug()))
            }
        }
    }
}

private val ShortMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
