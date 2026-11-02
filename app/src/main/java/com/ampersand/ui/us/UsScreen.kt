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
            .padding(horizontal = Inset.screen),
    ) {
        Spacer(Modifier.height(Space.l))
        CoupleHeader(state, onOpenSettings)

        Spacer(Modifier.height(Space.l))
        BlendStrip(you = you, them = them)

        Spacer(Modifier.height(Space.xxl))
        YouTwo(state)
        Spacer(Modifier.height(Space.m))
        val byDate = remember(state.history) { state.history.associateBy { it.date } }
        com.ampersand.ui.history.StudPlate(state, byDate, onOpenDay)

        Spacer(Modifier.height(Space.xxl))
        ProfileBadges(state = state, onTake = { onTakeInstrument?.invoke(it) })

        Spacer(Modifier.height(Space.xxl))
        // Plans: dates coming up, and things to do someday.
        SectionLabel("Plans")
        Spacer(Modifier.height(Space.m))
        var someday by rememberSaveable { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            BrickChip(label = "Coming up", on = !someday, color = you, icon = UiIcon.DATES, onClick = { someday = false })
            BrickChip(label = "Someday", on = someday, color = you, onClick = { someday = true })
        }
        Spacer(Modifier.height(Space.m))
        if (someday) {
            BucketSection(state, onAddToBucket, onTickBucket, onRemoveFromBucket)
        } else {
            DatesSection(state, onAddSpecialDate)
        }

        Spacer(Modifier.height(Space.page))
    }
}

/** Both names, how long, and the way to the settings. */
@Composable
internal fun CoupleHeader(state: CoupleState, onOpenSettings: () -> Unit) {
    val sounds = LocalSounds.current
    val feel = LocalFeel.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                "${state.you.name} & ${state.them.name}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Space.xs))
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
        Spacer(Modifier.width(Space.m))
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
            IconAction(UiIcon.SETTINGS, "Settings", Modifier.padding(horizontal = Space.m, vertical = Space.hair), color = inkOnBrick(structureSlug()))
        }
    }
}

/** You two: days you both answered, questions you wrote and guesses that landed, each one number for the pair. */
@Composable
internal fun YouTwo(state: CoupleState) {
    val together = SideBySide.of(state)
    val weeks = DayPips.weeksActive(state)
    val youInk = Color(state.you.colorArgb)
    val themInk = Color(state.them.colorArgb)

    Column(
        Modifier
            .fillMaxWidth()
            .panel()
            .padding(horizontal = Space.m, vertical = Space.m),
    ) {
        Text("You two", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Space.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.you.name, style = MaterialTheme.typography.labelLarge, color = youInk)
            Text("  ·  ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(state.them.name, style = MaterialTheme.typography.labelLarge, color = themInk)
        }
        Spacer(Modifier.height(Space.m))
        Row(Modifier.fillMaxWidth()) {
            // One number each, for the pair: none of this is a contest.
            listOf(
                together.daysTogether to (if (weeks == 1) "Days together this week" else "Days together, $weeks weeks"),
                together.questionsWritten to "Questions written",
                state.knowing.landed to "Guesses landed",
            ).forEach { (n, label) ->
                Column(Modifier.weight(1f)) {
                    Text("$n", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
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
