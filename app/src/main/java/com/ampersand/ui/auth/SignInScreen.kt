package com.ampersand.ui.auth

import com.ampersand.ui.theme.Alpha
import com.ampersand.ui.theme.Inset
import com.ampersand.ui.theme.Space
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

/** Signed-out screen: brick wordmark (same as the launcher icon), tagline, and Google sign-in. */
@Composable
fun SignInScreen(
    busy: Boolean,
    error: String?,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    onPreview: () -> Unit = {},
) {
    Box(modifier.fillMaxSize()) {
      BrickWall(you = BrickColor.RED, them = BrickColor.BLUE, alpha = Alpha.faint)

      Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = Inset.screen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark()

        Spacer(Modifier.height(Space.xl))
        Text(
            text = "Answers open when you both finish.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Space.xxl))
        AmpButton(
            label = if (busy) "Signing in" else "Continue with Google",
            slug = BrickColor.RED.slug,
            enabled = !busy,
            onClick = onSignIn,
        )

        if (error != null) {
            Spacer(Modifier.height(Space.l))
            // Selectable, so a failure report can be copied out whole.
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(Space.xl))
        SectionLabel("One question a day, for two people")

        Spacer(Modifier.height(Space.xxl))
        com.ampersand.ui.components.IconAction(
            com.ampersand.ui.components.UiIcon.NEXT,
            "Skip for now and look around",
            onClick = onPreview,
        )
      }
    }
}

/** The app wordmark, same as the splash. */
@Composable
private fun Wordmark() {
    BrickMark(you = BrickColor.RED, them = BrickColor.BLUE, stud = 38.dp, playful = true)
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
