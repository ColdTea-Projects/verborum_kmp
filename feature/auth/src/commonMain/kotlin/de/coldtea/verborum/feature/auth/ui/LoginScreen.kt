package de.coldtea.verborum.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.auth.SignInFailure
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.designsystem.component.VerborumBrandMark
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel
import de.coldtea.verborum.core.localization.strings

/**
 * What to tell someone whose sign-in did not go through, in their own language.
 *
 * Resolved here rather than in the view model so it follows the app's language — and so the
 * diagnosis, which the view model logs, never reaches the screen. Every case is spelled out: a new
 * kind of failure should stop the compiler here rather than fall through to something vague.
 */
@Composable
private fun SignInFailure.message(): String = when (this) {
    is SignInFailure.Refused -> strings.signInRefused
    SignInFailure.UnverifiedRedirect -> strings.signInUnverified
    is SignInFailure.ExchangeFailed -> when (error) {
        is VerborumError.Network -> strings.signInUnreachable
        VerborumError.Unauthorized -> strings.signInRejected
        is VerborumError.Http -> strings.signInFailed
        is VerborumError.Serialization -> strings.signInFailed
        is VerborumError.Unknown -> strings.signInFailed
    }
}

/**
 * The login wall the app shell shows whenever no one is signed in. Signing in and creating an
 * account are the same Keycloak flow pointed at different endpoints, opened in the system browser —
 * there is no password field here, by design.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LoginContent(
        state = state,
        onSignIn = viewModel::signIn,
        onCreateAccount = viewModel::createAccount,
        modifier = modifier,
    )
}

@Composable
internal fun LoginContent(
    state: LoginState,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The wall renders outside the app's Scaffold, so it owns its own insets.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            VerborumBrandMark()

            Spacer(modifier = Modifier.height(Spacing.large))

            Text(
                text = "Verborum",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = strings.signInTagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.extraLarge))

            // Capped so the buttons stay a sensible size in a wide browser window instead of
            // stretching the full viewport.
            Column(
                modifier = Modifier.widthIn(max = ContentWidth.form).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onSignIn,
                    enabled = !state.isAuthenticating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Text(strings.signIn)
                }

                Spacer(modifier = Modifier.height(Spacing.small))

                OutlinedButton(
                    onClick = onCreateAccount,
                    enabled = !state.isAuthenticating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Text(strings.createAccount)
                }

                state.failure?.let { failure ->
                    Text(
                        text = failure.message(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Spacing.medium),
                    )
                }
            }
        }

        // Dim and block while the browser hands back and the code is exchanged.
        if (state.isAuthenticating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private const val ScrimAlpha = 0.25f
