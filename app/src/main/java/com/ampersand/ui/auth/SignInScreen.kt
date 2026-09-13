package com.ampersand.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.ampersand.ui.brick.BrickMark
import com.ampersand.ui.components.AmpButton
import com.ampersand.ui.components.BrickWall
import com.ampersand.ui.components.SectionLabel
import com.ampersand.ui.theme.AmpersandTheme

/**
 * The only screen you can reach signed out.
 *
 * Sits on the same brick wall as the rest of the app. Without it this was a
 * wordmark floating in an empty field, which reads as a placeholder rather than
 * the front of something -- and in light mode, where the ground is pale rather
 * than deep navy, there was nothing on screen at all between the button and the
 * bottom edge.
 *
 * One sentence of copy, not two. There is nothing to explore before you are in,
 * and the seal is the only thing worth saying up front.
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
    Box(modifier.fillMaxSize()) {
      BrickWall(you = BrickColor.RED, them = BrickColor.BLUE, alpha = 0.12f)

      Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark()

        Spacer(Modifier.height(22.dp))
        Text(
            text = "Neither answer opens until both are in.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))
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

        Spacer(Modifier.height(20.dp))
        SectionLabel("One question a day, for two people")
      }
    }
}

/** The name, in the two colours it is named after. */
@Composable
private fun Wordmark() {
    // The same mark as the splash, still. The two screens used to disagree
    // about what the app was called: a falling pair of bricks on one and a
    // twelve-stud slab with a line of type across it on the other.
    BrickMark(you = BrickColor.RED, them = BrickColor.BLUE, stud = 38.dp)
}

@Preview(name = "Sign in", showBackground = true, heightDp = 760)
@Composable
private fun PreviewSignIn() = AmpersandTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        SignInScreen(busy = false, error = null, onSignIn = {})
    }
}

@Preview(name = "Sign in - light", showBackground = true, heightDp = 760)
@Composable
private fun PreviewSignInLight() = AmpersandTheme(darkTheme = false) {
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
