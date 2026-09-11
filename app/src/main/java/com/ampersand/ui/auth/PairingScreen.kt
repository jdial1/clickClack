package com.ampersand.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.inkOnBrick
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.AmpField
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.theme.AmpersandTheme

/**
 * Signed in, but on your own.
 *
 * Both directions on one screen, because there is no way for the app to know
 * which of you will move first and asking ("do you have a code?") is a question
 * neither person can answer until one of them has acted.
 */
@Composable
fun PairingScreen(
    name: String,
    code: String?,
    busy: Boolean,
    error: String?,
    onCreateInvite: () -> Unit,
    onRedeem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf("") }

    Column(
        modifier
            // fillMaxSize, not fillMaxWidth: a scrolling column sized to its
            // content leaves the window background showing below it, as a hard
            // horizontal edge across the screen.
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "Hello $name.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Click Clack needs two people. Pair with yours to start.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))
        SectionLabel("Invite them")
        Spacer(Modifier.height(12.dp))

        if (code == null) {
            AmpButton(
                label = if (busy) "Making a code" else "Make a code",
                slug = BrickColor.RED.slug,
                enabled = !busy,
                onClick = onCreateInvite,
            )
        } else {
            CodePlate(code)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Read it to them, or send it. It works once.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(36.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(28.dp))

        SectionLabel("Or type theirs")
        Spacer(Modifier.height(12.dp))
        AmpField(
            value = entered,
            onValueChange = { entered = it },
            accent = MaterialTheme.colorScheme.primary,
            placeholder = "8 characters",
        )
        Spacer(Modifier.height(14.dp))
        AmpButton(
            label = if (busy) "Joining" else "Join them",
            slug = BrickColor.BLUE.slug,
            enabled = !busy && entered.isNotBlank(),
            onClick = { onRedeem(entered) },
        )

        if (error != null) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

/**
 * The code, big, monospaced and spaced out.
 *
 * Monospace because two people are going to compare it character by character,
 * and a proportional font makes that harder than it needs to be. Selectable so
 * it can be copied into a message without being retyped.
 */
@Composable
private fun CodePlate(code: String) {
    BrickSlab(
        slugs = listOf(BrickColor.YELLOW.slug),
        studs = 12,
        height = BrickHeight.MEDIUM,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SelectionContainer {
            Text(
                text = code.toCharArray().joinToString(" "),
                fontFamily = FontFamily.Monospace,
                fontSize = 26.sp,
                letterSpacing = 1.sp,
                color = inkOnBrick(BrickColor.YELLOW.slug),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = 22.dp),
            )
        }
    }
}

@Preview(name = "Pairing", showBackground = true, heightDp = 900)
@Composable
private fun PreviewPairing() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        PairingScreen("Justin", null, false, null, {}, {})
    }
}

@Preview(name = "Pairing - code made", showBackground = true, heightDp = 900)
@Composable
private fun PreviewPairingCode() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        PairingScreen("Justin", "KX7PQ4MB", false, null, {}, {})
    }
}

@Preview(name = "Pairing - bad code", showBackground = true, heightDp = 900)
@Composable
private fun PreviewPairingError() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        PairingScreen(
            name = "Justin",
            code = null,
            busy = false,
            error = "No invite with that code. Check it and try again.",
            onCreateInvite = {},
            onRedeem = {},
        )
    }
}
