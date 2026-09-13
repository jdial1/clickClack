package com.ampersand.ui.us

import com.ampersand.ui.components.UiIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampersand.model.BrickColor
import com.ampersand.model.League
import com.ampersand.model.Topic
import com.ampersand.model.label
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.AmpCheckboxGroup
import com.ampersand.ui.components.BrickWall

/**
 * One-time setup: which sports leagues you follow (none means no sports
 * questions), then topics to leave out. Both are editable later in Your profile.
 */
@Composable
fun InterestsSetupScreen(
    color: BrickColor,
    onDone: (following: Set<League>, leaveOut: Set<Topic>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableStateOf(0) }
    var leagues by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var leaveOut by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val accent = Color(color.argb)

    fun finish() = onDone(
        League.entries.filter { it.label in leagues }.toSet(),
        Topic.entries.filter { it.label in leaveOut }.toSet(),
    )

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier.fillMaxSize()) {
            BrickWall(you = color, them = BrickColor.BLUE, alpha = 0.12f)
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (step) {
                    0 -> {
                        Heading("Do you follow sports?", "Only the ones you pick show up in questions.")
                        Spacer(Modifier.height(24.dp))
                        AmpButton(label = "Yes", slug = color.slug) { step = 1 }
                        Spacer(Modifier.height(10.dp))
                        AmpButton(label = "Not really", slug = "black") { leagues = emptySet(); step = 2 }
                    }

                    1 -> {
                        Heading("Which ones?", "Local teams are based on your home ZIP.")
                        Spacer(Modifier.height(20.dp))
                        AmpCheckboxGroup(
                            options = League.entries.map { it.label },
                            selected = leagues,
                            accent = accent,
                            modifier = Modifier.fillMaxWidth(),
                            onToggle = { leagues = if (it in leagues) leagues - it else leagues + it },
                        )
                        Spacer(Modifier.height(20.dp))
                        AmpButton(label = "Next", slug = color.slug, icon = UiIcon.NEXT) { step = 2 }
                    }

                    else -> {
                        Heading(
                            "Any topics to skip?",
                            "If either of you skips a topic, it's skipped for both.",
                        )
                        Spacer(Modifier.height(20.dp))
                        AmpCheckboxGroup(
                            options = Topic.entries.filter { it != Topic.SPORTS }.map { it.label },
                            selected = leaveOut,
                            accent = accent,
                            modifier = Modifier.fillMaxWidth(),
                            onToggle = { leaveOut = if (it in leaveOut) leaveOut - it else leaveOut + it },
                        )
                        Spacer(Modifier.height(20.dp))
                        AmpButton(label = "Done", slug = color.slug, icon = UiIcon.DONE) { finish() }
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(title: String, detail: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}
