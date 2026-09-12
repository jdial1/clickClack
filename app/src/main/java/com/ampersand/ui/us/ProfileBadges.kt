package com.ampersand.ui.us

import androidx.compose.animation.AnimatedVisibility
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
import com.ampersand.data.Instruments
import com.ampersand.data.Sources
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
 * The results of every whole-instrument sitting, kept.
 *
 * A number that appears once and scrolls away is not worth a six-minute
 * sitting, so the badges live here permanently. They are bricks because the app
 * has exactly one visual language, and an unclaimed slot is a ghost brick
 * because an empty spot is an invitation this app already knows how to make.
 *
 * Tapping an earned badge opens the shape behind it rather than just the
 * headline -- four letters with their margins, five love-language scores rather
 * than only the loudest. A four-letter type that hides how close an axis ran is
 * claiming a precision the instrument does not have.
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
        Spacer(Modifier.height(6.dp))
        Text(
            "Sat down together, answered whole, kept.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        groups.forEachIndexed { i, group ->
            if (i > 0) Spacer(Modifier.height(30.dp))
            WallGroup(
                group = group,
                you = you,
                them = them,
                openSlot = open,
                onTap = { badge ->
                    if (badge.isEarned) {
                        open = if (open == badge.slotId) null else badge.slotId
                    } else {
                        onTake(badge.instrumentId)
                    }
                },
            )
        }

        Spacer(Modifier.height(18.dp))
        QuietNote("Nothing fills until you have both finished.")
    }
}

/**
 * One test, and everything it produced.
 *
 * The heading carries the explanation because a badge on its own is a result
 * with no question attached to it -- "Acts of service" means nothing in April
 * to somebody who answered in January. It is also what makes three courses
 * legible as one sitting rather than as three tests sharing a word.
 */
@Composable
private fun WallGroup(
    group: BadgeGroup,
    you: BrickColor,
    them: BrickColor,
    openSlot: String?,
    onTap: (Badge) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().onWall(corner = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    group.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = when {
                        group.isEarned -> "Done"
                        group.waitingOnThem -> "Waiting on them"
                        else -> "${group.minutes} min"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                group.about,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        group.rows.forEach { row ->
            Spacer(Modifier.height(12.dp))
            WallRow(
                row = row,
                you = you,
                them = them,
                openSlot = openSlot,
                onTap = onTap,
            )
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
        // Empty on a test that produces a single course: a sub-heading above
        // one row that restates the group's own name is noise.
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

        row.badges.firstOrNull { it.slotId == openSlot }?.let { badge ->
            AnimatedVisibility(visible = true) {
                Detail(
                    badge = badge,
                    color = when (badge.owner) {
                        Owner.THEM -> them
                        else -> you
                    },
                )
            }
        }
    }
}

/**
 * One badge, as a brick.
 *
 * A couple result spans the row and is painted in both colours, the same way
 * every other shared surface in the app is -- it is not either person's result
 * and should not be either person's colour.
 */
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

    // Slabs rather than pills, and six studs rather than a hugged width. A pill
    // sizes itself to its label, which is right for a chip and wrong here: two
    // badges side by side came out at whatever width "Time" and "Acts of
    // service" happened to need, and a wall whose courses are all different
    // lengths does not read as a wall. Six and six make the twelve the page is
    // already built on, so an individual badge and a couple badge share a stud
    // size and the rows stack like brickwork.
    val body: @Composable () -> Unit = { Box(Modifier.fillMaxWidth(), Alignment.Center) { label() } }

    when {
        // Not taken yet: a ghost brick, not a missing one. The wall reads as
        // having a hole in it rather than as being short a row.
        !badge.isEarned -> BrickSlab(
            slugs = listOf(quietSlug()),
            modifier = modifier.then(tap),
            studs = HalfRow,
            height = BrickHeight.MEDIUM,
            alpha = GhostAlpha,
            content = { body() },
        )

        // Not either person's result, so not either person's colour.
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

/**
 * How faint an unclaimed slot is.
 *
 * Low enough to read as a hole rather than as a filled grey brick -- at the
 * first value the quiet brick came through as solid charcoal in light mode and
 * the row looked taken.
 */
private const val GhostAlpha = 0.3f

@Composable
private fun ink(badge: Badge, color: BrickColor, other: BrickColor): Color = when {
    // Not `inkOnBrick`: that answers for a *solid* brick, and at
    // [GhostAlpha] the ground shows through and decides the contrast. White on
    // a 30%-black brick over a pale wall is unreadable; the theme's own ink is
    // correct on either ground by construction.
    !badge.isEarned -> MaterialTheme.colorScheme.onBackground
    badge.owner == Owner.BOTH -> inkOnBricks(color.slug, other.slug)
    else -> inkOnBrick(color.slug)
}

private fun takeLabel(badge: Badge): String =
    if (badge.waitingOnThem) "Waiting" else "Take it"

/**
 * The shape behind the headline.
 *
 * Every bar is labelled with the number it stands for, because the whole reason
 * this exists is that a headline word is a rounding of a continuum and the app
 * should not pretend otherwise.
 */
@Composable
private fun Detail(badge: Badge, color: BrickColor) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        badge.detail.forEachIndexed { i, bar ->
            if (i > 0) Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    bar.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(118.dp),
                )
                Meter(fraction = bar.fraction, color = color, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text(
                    bar.value,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        badge.note?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Sources[Instruments[badge.instrumentId]?.sourceId]?.let { source ->
            Spacer(Modifier.height(10.dp))
            Text(
                "${source.credit} · ${source.licence}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
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
