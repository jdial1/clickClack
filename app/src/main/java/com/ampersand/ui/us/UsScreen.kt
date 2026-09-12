package com.ampersand.ui.us

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ampersand.ui.theme.ColorChoices
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.toArgbLong
import java.time.format.DateTimeFormatter

@Composable
fun UsScreen(
    state: CoupleState,
    onPickColor: (colorArgb: Long) -> Unit,
    onSetAnniversary: ((java.time.LocalDate) -> Unit)? = null,
    modifier: Modifier = Modifier,
    account: String? = null,
    onSignOut: (() -> Unit)? = null,
    onUnpair: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                value = state.streak.toString(),
                label = "Day streak",
                caption = if (state.freezesLeft > 0) {
                    "${state.freezesLeft} freeze left this week"
                } else {
                    "Freeze used this week"
                },
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "${state.syncPercent}%",
                label = "In sync",
                caption = "Guesses you both landed",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(32.dp))
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
            onPick = onPickColor,
        )
        Spacer(Modifier.height(20.dp))
        // Theirs is shown, not offered. A colour belongs to the person whose
        // bricks it paints, and the rules refuse a write to their profile
        // anyway -- so a picker here was a control that could only fail.
        PartnerColor(name = state.them.name, current = Color(state.them.colorArgb))

        Spacer(Modifier.height(20.dp))
        BlendStrip(
            you = BrickColor.of(state.you.colorArgb),
            them = BrickColor.of(state.them.colorArgb),
        )

        Spacer(Modifier.height(32.dp))
        Knowing(state)

        Spacer(Modifier.height(32.dp))
        Anniversary(state, onSetAnniversary)

        Spacer(Modifier.height(32.dp))
        FeedbackToggles(you = BrickColor.of(state.you.colorArgb))

        Spacer(Modifier.height(32.dp))
        SectionLabel("Appearance")
        Spacer(Modifier.height(10.dp))
        ThemeBricks(you = BrickColor.of(state.you.colorArgb))


        if (onSignOut != null) {
            Spacer(Modifier.height(32.dp))
            SectionLabel("Account")
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
                AmpButton(label = "Unpair", slug = "black", onClick = onUnpair)
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    BrickSlab(
        slugs = listOf(null),
        studs = 6,
        modifier = modifier,
    ) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 12.dp)
            .glassOn(null, corner = 14.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.displaySmall,
            color = inkOnBrick(null),
        )
        Spacer(Modifier.height(4.dp))
        SectionLabel(label, color = inkOnBrick(null).copy(alpha = 0.7f))
        Spacer(Modifier.height(6.dp))
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = inkOnBrick(null).copy(alpha = 0.62f),
        )
    }
    }
}

@Composable
private fun ColorRow(name: String, current: Color, onPick: (Long) -> Unit) {
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
                Box(
                    Modifier
                        .clickable { onPick(choice.argb) }
                        .padding(4.dp),
                ) {
                    ColorChip(
                        label = if (selected) "\u2022" else " ",
                        color = choice,
                        studSize = if (selected) 24.dp else 20.dp,
                    )
                }
            }
        }
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
                studSize = 13.dp,
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
                studSize = 30.dp,
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

/**
 * Shows the two colours meeting. Painted with the theme's own [DuoColors.sweep]
 * rather than a local gradient, so it shows the *actual* blend the rest of the
 * app is using -- an RGB lerp here would draw a duller midpoint than the one on
 * the hero card and quietly make the screen a liar.
 */
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

@Composable
private fun Upcoming(title: String, detail: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BrickDot(slug = quietSlug(), studSize = 13.dp, modifier = Modifier.padding(top = 3.dp))
        Spacer(Modifier.size(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Us", showBackground = true, heightDp = 900)
@Composable
private fun PreviewUs() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        UsScreen(MockCouple.state(), {}, {})
    }
}

@Preview(name = "Us - dark", showBackground = true, heightDp = 900)
@Composable
private fun PreviewUsDark() = AmpersandTheme(darkTheme = true) {
    Surface(color = MaterialTheme.colorScheme.background) {
        UsScreen(MockCouple.state(), {}, {})
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

    SectionLabel("Time together")
    Spacer(Modifier.height(6.dp))

    if (since != null) {
        Text(
            text = timeTogether(since, state.today.date),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Since ${since.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy"))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Occasion.next(state.today.date.plusDays(1), since)?.let { (occasion, day) ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Next up: ${occasion.label}, " +
                    day.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
