package com.ampersand.ui.us

import com.ampersand.ui.components.IconAction
import com.ampersand.ui.components.UiIcon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampersand.data.BadgeWall
import com.ampersand.data.Profiles
import com.ampersand.model.Badge
import com.ampersand.model.BadgeGroup
import com.ampersand.model.BadgeRow
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.CoupleState
import com.ampersand.model.Owner

import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.brick.inkOnBricks
import com.ampersand.ui.brick.onWall
import com.ampersand.ui.brick.quietSlug
import com.ampersand.ui.components.QuietNote
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.feel.Feel
import com.ampersand.ui.feel.LocalFeel
import com.ampersand.ui.feel.play
import com.ampersand.ui.sound.LocalSounds
import com.ampersand.ui.sound.Sfx
import com.ampersand.ui.sound.play
import com.ampersand.ui.theme.Motion


/**
 * Saved results of every whole-instrument test, as badge bricks. Unclaimed slots
 * are ghost bricks; tapping an earned badge shows its full score breakdown.
 */
@Composable
fun ProfileBadges(
    state: CoupleState,
    onTake: (instrumentId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(state.instrumentRuns, state.you, state.them) {
        BadgeWall.groups(state.instrumentRuns, state.you, state.them)
    }
    var open by remember { mutableStateOf<String?>(null) }

    val you = BrickColor.of(state.you.colorArgb)
    val them = BrickColor.of(state.them.colorArgb)

    Column(modifier.fillMaxWidth()) {
        SectionLabel("Profiles")
        Spacer(Modifier.height(12.dp))

        val earned = groups.filter { it.isEarned }
        if (earned.isEmpty()) QuietNote("Take the longer tests on Decks to see results here.")
        earned.forEachIndexed { i, group ->
            if (i > 0) Spacer(Modifier.height(30.dp))
            WallGroup(
                group = group,
                you = you,
                them = them,
                openSlot = open,
                onTake = onTake,
                onTap = { badge ->
                    if (badge.isEarned) open = if (open == badge.slotId) null else badge.slotId
                },
            )
        }

    }
}

/** One test: a heading, then its badge rows or a single take button. */
@Composable
private fun WallGroup(
    group: BadgeGroup,
    you: BrickColor,
    them: BrickColor,
    openSlot: String?,
    onTap: (Badge) -> Unit,
    onTake: (String) -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val interaction = remember { MutableInteractionSource() }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().onWall(corner = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                group.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            group.rows.firstOrNull()?.badges?.firstOrNull { it.isEarned }?.let { first ->
                IconAction(UiIcon.DETAILS, "Details", onClick = { onTap(first) })
            }
        }

        Spacer(Modifier.height(12.dp))

        // One button per test, not per slot (per-slot showed six "Take it" bricks for one test).
        if (group.isEarned) {
            group.rows.forEachIndexed { i, row ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                WallRow(
                    row = row,
                    you = you,
                    them = them,
                    openSlot = openSlot,
                    onTap = onTap,
                )
            }
        } else {
            TakeIt(group = group, onTake = onTake)
        }

    }
}

/** An untaken test as one ghost brick. Disabled (but still shown) once you've done your part. */
@Composable
private fun TakeIt(
    group: BadgeGroup,
    onTake: (String) -> Unit,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val interaction = remember { MutableInteractionSource() }
    val yourTurn = group.yourTurn

    val tap = if (yourTurn) {
        Modifier.clickable(interactionSource = interaction, indication = null) {
            sounds.play(Sfx.PRESS)
            feel.play(Feel.PRESS)
            onTake(group.instrumentId)
        }
    } else {
        Modifier
    }

    BrickSlab(
        slugs = listOf(quietSlug()),
        modifier = Modifier.fillMaxWidth().then(tap),
        studs = HalfRow * 2,
        height = BrickHeight.MEDIUM,
        alpha = if (yourTurn) GhostAlpha else GhostAlpha * 0.7f,
    ) {
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Column(
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Progress for both partners; falls back to duration before anyone starts.
                Text(
                    text = group.standing.ifBlank { "${group.minutes} min each" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = if (yourTurn) 0.72f else 0.5f,
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = group.action,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = if (yourTurn) 1f else 0.55f,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WallRow(
    row: BadgeRow,
    you: BrickColor,
    them: BrickColor,
    openSlot: String?,
    onTap: (Badge) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        // Blank for single-row tests, so no redundant sub-heading.
        if (row.label.isNotBlank()) {
            SectionLabel(row.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(7.dp))
        }

        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            row.badges.forEach { badge ->
                BadgeBrick(
                    badge = badge,
                    color = when (badge.owner) {
                        Owner.YOU -> you
                        Owner.THEM -> them
                        Owner.BOTH -> you
                    },
                    other = them,
                    onTap = { onTap(badge) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Tapping either brick opens details for both, side by side for comparison.
        if (row.badges.any { it.slotId == openSlot }) {
            val earned = row.badges.filter { it.isEarned }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    earned.forEach { badge ->
                        Detail(
                            badge = badge,
                            color = if (badge.owner == Owner.THEM) them else you,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** One badge as a brick. Couple results span the row in both colors. */
@Composable
private fun BadgeBrick(
    badge: Badge,
    color: BrickColor,
    other: BrickColor,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    val interaction = remember { MutableInteractionSource() }

    val label: @Composable () -> Unit = {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (badge.isEarned) badge.who else "${badge.minutes} min",
                style = MaterialTheme.typography.labelSmall,
                color = ink(badge, color, other).copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = badge.result ?: takeLabel(badge),
                style = MaterialTheme.typography.titleSmall,
                color = ink(badge, color, other),
                textAlign = TextAlign.Center,
            )
        }
    }

    val tap = Modifier.clickable(
        interactionSource = interaction,
        indication = null,
    ) {
        sounds.play(if (badge.isEarned) Sfx.NAV else Sfx.PRESS)
        feel.play(if (badge.isEarned) Feel.NAV else Feel.PRESS)
        onTap()
    }

    // Fixed six-stud slabs, not label-sized pills, so side-by-side badges match widths.
    val body: @Composable () -> Unit = { Box(Modifier.fillMaxWidth(), Alignment.Center) { label() } }

    when {
        // Not taken yet: ghost brick.
        !badge.isEarned -> BrickSlab(
            slugs = listOf(quietSlug()),
            modifier = modifier.then(tap),
            studs = HalfRow,
            height = BrickHeight.MEDIUM,
            alpha = GhostAlpha,
            content = { body() },
        )

        // Couple result: both colors, full row.
        badge.owner == Owner.BOTH -> BrickSlab(
            slugs = listOf(color.slug, other.slug),
            modifier = modifier.then(tap),
            studs = HalfRow * 2,
            height = BrickHeight.MEDIUM,
            content = { body() },
        )

        else -> BrickSlab(
            slugs = listOf(color.slug),
            modifier = modifier.then(tap),
            studs = HalfRow,
            height = BrickHeight.MEDIUM,
            content = { body() },
        )
    }
}

/** Half the twelve-stud page, so two badges make one course. */
private const val HalfRow = 6

/** Alpha for unclaimed slots; higher values looked like a solid, taken brick in light mode. */
private const val GhostAlpha = 0.3f

@Composable
private fun ink(badge: Badge, color: BrickColor, other: BrickColor): Color = when {
    // Not inkOnBrick: at GhostAlpha the background decides contrast, so use theme ink.
    !badge.isEarned -> MaterialTheme.colorScheme.onBackground
    badge.owner == Owner.BOTH -> inkOnBricks(color.slug, other.slug)
    else -> inkOnBrick(color.slug)
}

private fun takeLabel(badge: Badge): String =
    if (badge.waitingOnThem) "Waiting" else "Take it"

/** One person's score breakdown: a labeled bar with its value per dimension. Labels sit above bars for width. */
@Composable
private fun Detail(badge: Badge, color: BrickColor, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            badge.who,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        badge.detail.forEach { bar ->
            Spacer(Modifier.height(8.dp))
            Text(
                bar.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Meter(fraction = bar.fraction, color = color, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(
                    bar.value,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

    }
}

@Composable
private fun Meter(fraction: Float, color: BrickColor, modifier: Modifier = Modifier) {
    val grown by animateFloatAsState(fraction, Motion.glide, label = "meter")
    Box(
        modifier
            .height(9.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(grown.coerceIn(0f, 1f))
                .height(9.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(color.argb)),
        )
    }
}
