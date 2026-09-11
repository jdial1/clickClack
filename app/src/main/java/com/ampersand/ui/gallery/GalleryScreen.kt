package com.ampersand.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.data.MockCouple
import com.ampersand.model.CoupleState
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.AmpReactionRow
import com.ampersand.ui.components.PartnerReaction
import com.ampersand.ui.components.AmpCheckboxGroup
import com.ampersand.ui.components.AmpDragRank
import com.ampersand.ui.components.AmpDropdown
import com.ampersand.ui.components.AmpField
import com.ampersand.ui.components.AmpRadioGroup
import com.ampersand.ui.components.AmpScale
import com.ampersand.ui.components.AmpSegmented
import com.ampersand.ui.components.ColorChip
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.theme.AmpersandTheme
import com.ampersand.model.BrickColor
import com.ampersand.model.Reaction

/**
 * Every control in one place, live and interactive.
 *
 * This exists so the form kit can be judged and tuned on its own, rather than
 * one control at a time through whichever question happens to be today's. It is
 * a scaffold screen, not a product screen.
 */
@Composable
fun GalleryScreen(state: CoupleState, modifier: Modifier = Modifier) {
    val you = Color(state.you.colorArgb)
    val them = Color(state.them.colorArgb)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "Form kit",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Filled surfaces, one corner radius, and the accent taking the whole control.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Section("Segmented tabs") {
            var seg by remember { mutableStateOf("Chats") }
            AmpSegmented(
                options = listOf("Chats", "Status", "Calls"),
                selected = seg,
                accent = you,
                onSelect = { seg = it },
            )
        }

        Section("Colour chips") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrickColor.entries.forEachIndexed { i, c ->
                    ColorChip(label = ('A' + i).toString(), color = c)
                }
            }
        }

        Section("Text field") {
            var single by remember { mutableStateOf("") }
            var multi by remember { mutableStateOf("We drove home the long way.") }
            AmpField(single, { single = it }, you, placeholder = "One line")
            Spacer(Modifier.height(10.dp))
            AmpField(multi, { multi = it }, them, placeholder = "Longer", minLines = 4)
        }

        Section("Radio") {
            var pick by remember { mutableStateOf<String?>("We make each other laugh") }
            AmpRadioGroup(
                options = listOf(
                    "We make each other laugh",
                    "We sort problems out fast",
                    "We give each other room",
                ),
                selected = pick,
                accent = you,
                onSelect = { pick = it },
            )
        }

        Section("Checkbox, capped at 3") {
            var picks by remember { mutableStateOf(setOf("A trip somewhere new")) }
            AmpCheckboxGroup(
                options = listOf(
                    "A trip somewhere new",
                    "Moving house",
                    "More time with friends",
                    "Getting properly fit",
                    "Less work",
                ),
                selected = picks,
                accent = them,
                max = 3,
                onToggle = { picks = if (it in picks) picks - it else picks + it },
            )
        }

        Section("Dropdown") {
            var month by remember { mutableStateOf<String?>(null) }
            AmpDropdown(
                options = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December",
                ),
                selected = month,
                accent = you,
                placeholder = "Pick a month",
                onSelect = { month = it },
            )
        }

        Section("Scale") {
            var value by remember { mutableStateOf<Int?>(4) }
            AmpScale(
                value = value,
                accent = them,
                lowLabel = "Not much",
                highLabel = "Completely",
                onSelect = { value = it },
            )
        }

        Section("Drag to order") {
            var order by remember {
                mutableStateOf(
                    listOf(
                        "Time together",
                        "Money in the bank",
                        "Career moving forward",
                        "Family close by",
                    ),
                )
            }
            Text(
                "Drag by the grip on the right.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            AmpDragRank(items = order, accent = you, onReorder = { order = it })
        }

        Section("Reactions") {
            var reaction by remember { mutableStateOf<Reaction?>(Reaction.MELT) }
            AmpReactionRow(
                selected = reaction,
                color = BrickColor.of(state.you.colorArgb),
                onSelect = { reaction = it },
            )
            Spacer(Modifier.height(16.dp))
            PartnerReaction(
                name = state.them.name,
                reaction = Reaction.LAUGH,
                color = BrickColor.of(state.them.colorArgb),
            )
        }

        Section("Buttons") {
            AmpButton("Seal it", slug = "red") {}
            Spacer(Modifier.height(12.dp))
            AmpButton("Your colour", slug = "blue") {}
            Spacer(Modifier.height(12.dp))
            AmpButton("Disabled", slug = "green", enabled = false) {}
        }

        Spacer(Modifier.height(56.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(32.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(20.dp))
    SectionLabel(title, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(14.dp))
    content()
}

@Preview(name = "Gallery", showBackground = true, heightDp = 2200)
@Composable
private fun PreviewGallery() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        GalleryScreen(MockCouple.state())
    }
}

@Preview(name = "Gallery - dark", showBackground = true, heightDp = 2200)
@Composable
private fun PreviewGalleryDark() = AmpersandTheme(darkTheme = true) {
    Surface(color = MaterialTheme.colorScheme.background) {
        GalleryScreen(MockCouple.state())
    }
}
