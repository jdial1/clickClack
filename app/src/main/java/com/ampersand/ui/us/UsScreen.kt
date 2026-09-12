package com.ampersand.ui.us

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
import com.ampersand.ui.brick.BrickDot
import com.ampersand.ui.brick.quietSlug
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.glassOn
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.ColorChip
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.components.brickFace
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.ui.theme.ColorChoices
import com.ampersand.ui.theme.Duo
import com.ampersand.ui.theme.toArgbLong

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
        Anniversary(state, onSetAnniversary)

        Spacer(Modifier.height(32.dp))
        SoundToggle(you = BrickColor.of(state.you.colorArgb))


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
 * The one switch worth having on this screen.
 *
 * Toggled before the confirming click plays, so turning sound *on* is audible;
 * the other order would silently enable it and feel broken.
 */
@Composable
private fun SoundToggle(you: BrickColor) {
    val sounds = LocalSounds.current
    val on = sounds?.enabled ?: false
    val accent = you.slug

    BrickSlab(
        slugs = listOf(if (on) accent else null),
        studs = 12,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                sounds?.let {
                    it.enabled = !it.enabled
                    if (it.enabled) it.play(Sfx.SELECT)
                }
            },
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
                if (on) "Sound on" else "Sound off",
                style = MaterialTheme.typography.titleSmall,
                color = inkOnBrick(if (on) accent else null),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Clicks, snaps and the seal.",
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
