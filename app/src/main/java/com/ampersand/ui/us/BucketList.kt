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

/** Shared bucket list. Ticking an item off lays a brick for each partner on this week's plate. */
@Composable
internal fun BucketSection(
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
    Spacer(Modifier.height(Space.m))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        BucketGroup.entries.forEach { g ->
            BrickChip(label = g.label, on = g == group, color = you, onClick = { group = g })
        }
    }
    Spacer(Modifier.height(Space.m))
    AmpField(
        value = draft,
        onValueChange = { draft = it.take(200) },
        color = you,
        placeholder = "Something to do together",
    )
    Spacer(Modifier.height(Space.m))
    AmpButton(label = "Add", slug = you.slug, enabled = draft.isNotBlank(), icon = UiIcon.ADD) {
        onAdd(draft.trim(), group)
        draft = ""
    }

    Spacer(Modifier.height(Space.l))
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
        Spacer(Modifier.height(Space.m))
        SectionLabel("Done")
        Spacer(Modifier.height(Space.s))
        done.forEach { item ->
            BucketRow(item = item, byName = null, action = null, onAction = {}, onRemove = null)
        }
    }
}

@Composable
internal fun BucketRow(
    item: BucketItem,
    byName: String?,
    action: String?,
    onAction: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    val slug = structureSlug()
    val ink = inkOnBrick(slug)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Space.m)
            .panel()
            .padding(horizontal = Space.m, vertical = Space.m),
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
            IconAction(UiIcon.CLOSE, "Remove", Modifier.padding(horizontal = Space.s), color = ink, onClick = onRemove)
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
                    modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s),
                )
            }
        }
    }
}

internal val DoneDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
