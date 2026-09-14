package com.ampersand.ui.auth

import androidx.compose.foundation.layout.Row
import com.ampersand.ui.components.UiIcon
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.ampersand.auth.PendingJoin
import com.ampersand.auth.InviteCode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.ampersand.ui.components.BrickWall
import com.ampersand.ui.components.AmpField
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.theme.AmpersandTheme

/** Signed in but unpaired: create an invite code or redeem your partner's, on one screen. */
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
    var entered by remember { mutableStateOf(PendingJoin.code?.let(InviteCode::display).orEmpty()) }
    val context = LocalContext.current
    // A join link opened while this screen is up fills the code in.
    androidx.compose.runtime.LaunchedEffect(PendingJoin.code) { PendingJoin.code?.let { entered = InviteCode.display(it) } }

    Box(modifier.fillMaxSize()) {
      BrickWall(you = BrickColor.RED, them = BrickColor.BLUE, alpha = 0.12f)

      Column(
        Modifier
            // fillMaxSize, or the window background shows below the content as a hard edge.
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Hello $name.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Connect with your partner to start.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(30.dp))
        SectionLabel("Invite your partner")
        Spacer(Modifier.height(12.dp))

        if (code == null) {
            AmpButton(
                label = if (busy) "Creating code" else "Create invite code",
                slug = BrickColor.RED.slug,
                enabled = !busy,
                onClick = onCreateInvite,
            )
        } else {
            CodePlate(code)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AmpButton(label = "Share", slug = null, icon = UiIcon.NEXT, modifier = Modifier.weight(1f)) {
                    val text = "Join me on Click Clack. Our code is ${InviteCode.display(code)}.\n${InviteCode.link(code)}"
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, "Share your code"))
                }
                AmpButton(label = "Copy", slug = null, icon = UiIcon.DONE, modifier = Modifier.weight(1f)) {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("Click Clack code", InviteCode.display(code)))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Single use. Expires in 1 hour.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(30.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(24.dp))

        SectionLabel("Or enter their code")
        Spacer(Modifier.height(12.dp))
        AmpField(
            value = entered,
            onValueChange = { entered = it },
            // No user color exists yet; blue matches the button below.
            color = BrickColor.BLUE,
            placeholder = "Like Wobbly Castle 427",
        )
        Spacer(Modifier.height(14.dp))
        AmpButton(
            label = if (busy) "Joining" else "Join",
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

        Spacer(Modifier.height(32.dp))
      }
    }
}

/** The invite code, large and monospaced for easy comparison, and selectable for copying. */
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
                text = InviteCode.display(code),
                fontSize = 26.sp,
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
        PairingScreen("Justin", "WOBBLYCASTLE427", false, null, {}, {})
    }
}

@Preview(name = "Pairing - light", showBackground = true, heightDp = 900)
@Composable
private fun PreviewPairingLight() = AmpersandTheme(darkTheme = false) {
    Surface(color = MaterialTheme.colorScheme.background) {
        PairingScreen("J D", "SLEEPYWAFFLE318", false, null, {}, {})
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
