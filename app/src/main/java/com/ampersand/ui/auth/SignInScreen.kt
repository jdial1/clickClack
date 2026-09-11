package com.ampersand.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.ui.brick.BrickSlab
import com.ampersand.ui.brick.inkOnBricks
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.theme.AmpersandTheme

/**
 * The only screen you can reach signed out.
 *
 * Deliberately almost empty. There is nothing to explore before you are in, no
 * feature tour and no carousel: the app is one question a day for two people,
 * and it cannot say anything at all until it knows which two.
 *
 * The wordmark is laid in brick, one colour each, because that is the product
 * in one image -- and it is the same two pieces as the launcher icon, so the
 * thing you tapped is the thing that opens.
 */
@Composable
fun SignInScreen(
    busy: Boolean,
    error: String?,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark()

        Spacer(Modifier.height(28.dp))
        Text(
            text = "A question a day, for two people.\nNeither answer opens until both are in.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))
        AmpButton(
            label = if (busy) "Signing in" else "Continue with Google",
            slug = BrickColor.RED.slug,
            enabled = !busy,
            onClick = onSignIn,
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

        Spacer(Modifier.height(24.dp))
        SectionLabel("Your answers are visible to one person")
    }
}

/** The name, in the two colours it is named after. */
@Composable
private fun Wordmark() {
    val you = BrickColor.RED
    val them = BrickColor.BLUE
    BrickSlab(
        slugs = listOf(you.slug, them.slug),
        studs = 12,
        height = BrickHeight.MEDIUM,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Click Clack",
            style = MaterialTheme.typography.displaySmall,
            color = inkOnBricks(you.slug, them.slug),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 28.dp),
        )
    }
}

@Preview(name = "Sign in", showBackground = true, heightDp = 760)
@Composable
private fun PreviewSignIn() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        SignInScreen(busy = false, error = null, onSignIn = {})
    }
}

@Preview(name = "Sign in - failed", showBackground = true, heightDp = 760)
@Composable
private fun PreviewSignInError() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        SignInScreen(
            busy = false,
            error = "No Google account on this device.",
            onSignIn = {},
        )
    }
}
