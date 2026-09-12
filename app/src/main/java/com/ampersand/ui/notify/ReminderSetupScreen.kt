package com.ampersand.ui.notify

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.model.BrickColor
import com.ampersand.notify.Reminder
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.AmpDropdown
import com.ampersand.ui.components.BrickWall
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.theme.AmpersandTheme

/**
 * Asked once, at setup: when should we say something.
 *
 * Placed after pairing rather than before, so the question arrives when there is
 * already a reason to answer it. Asking permission to interrupt someone before
 * they have seen what the app does is how an app gets its notifications turned
 * off forever on the first screen.
 *
 * "Not now" is a real answer and sits next to the button rather than hidden
 * behind a back gesture. The app has exactly one notification to send, and a
 * person who does not want it is not a problem to be solved.
 */
@Composable
fun ReminderSetupScreen(
    color: BrickColor,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hour by remember { mutableStateOf<String?>("8 pm") }

    // The permission only exists on API 33+. Below that a notification simply
    // posts, so the launcher is never used and the flow is identical.
    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) Reminder.schedule(context, parseHour(hour), 0) else Reminder.markAsked(context)
        onDone()
    }

    Box(modifier.fillMaxSize()) {
        BrickWall(you = color, them = BrickColor.BLUE, alpha = 0.12f)

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "One nudge a day.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "So the question does not sit there unanswered while you both wait " +
                    "for the other one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(30.dp))
            SectionLabel("What time suits")
            Spacer(Modifier.height(10.dp))
            AmpDropdown(
                options = Hours,
                selected = hour,
                accent = MaterialTheme.colorScheme.primary,
                placeholder = "Pick a time",
                onSelect = { hour = it },
            )

            Spacer(Modifier.height(24.dp))
            AmpButton(
                label = "Remind me",
                slug = color.slug,
                enabled = hour != null,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Reminder.schedule(context, parseHour(hour), 0)
                    onDone()
                }
            }

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { Reminder.markAsked(context); onDone() }) {
                Text("Not now")
            }
        }
    }
}

/**
 * Whole hours only, named the way people say them.
 *
 * A minute picker would be a third dropdown to serve a difference nobody can
 * feel: an inexact alarm is allowed to drift a few minutes anyway.
 */
private val Hours = listOf(
    "7 am", "8 am", "9 am", "12 pm", "5 pm", "6 pm",
    "7 pm", "8 pm", "9 pm", "10 pm",
)

private fun parseHour(label: String?): Int {
    val text = label ?: return 20
    val value = text.substringBefore(' ').toIntOrNull() ?: return 20
    val pm = text.endsWith("pm")
    return when {
        pm && value != 12 -> value + 12
        !pm && value == 12 -> 0
        else -> value
    }
}

@Preview(name = "Reminder setup", showBackground = true, heightDp = 760)
@Composable
private fun PreviewReminder() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        ReminderSetupScreen(color = BrickColor.RED, onDone = {})
    }
}
