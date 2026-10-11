package de.coldtea.verborum.feature.options.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.feature.options.ui.profile.model.ProfileForm
import de.coldtea.verborum.feature.options.ui.profile.model.ProfileState
import kotlinx.coroutines.flow.map
import org.koin.compose.viewmodel.koinViewModel

/**
 * The user's profile: the email it signed in with (read-only), the name other people see in the
 * Forum, and the Forum terms. Accepting the terms *with* a name is what makes someone a member —
 * there is no separate "join" action anywhere in the app.
 */
@Composable
internal fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    RegisterTopBar(title = strings.optionsProfile, backLabel = strings.options)

    // Both outcomes land here; the form keeps the user's drafts either way, so a rejected update is
    // a message rather than a lost edit.
    ShowSnackbarMessages(
        remember(viewModel, strings) {
            viewModel.effects.map { effect ->
                when (effect) {
                    ProfileEffect.Updated -> strings.profileUpdated
                    ProfileEffect.UpdateFailed -> strings.profileUpdateError
                    is ProfileEffect.ServerMessage -> effect.detail
                }
            }
        },
    )

    when (val current = state) {
        ProfileState.Loading -> LoadingState(modifier)

        is ProfileState.Success -> ProfileContent(
            form = current.form,
            onDisplayNameChange = viewModel::changeDisplayName,
            onAgreementChange = viewModel::changeAgreement,
            onUpdate = viewModel::update,
            onConfirmLeave = viewModel::confirmLeaveForum,
            onDismissLeave = viewModel::dismissLeaveConfirmation,
            modifier = modifier,
        )

        ProfileState.Failed -> ErrorState(
            message = strings.profileUpdateError,
            modifier = modifier,
            onRetry = viewModel::refresh,
        )
    }
}

/**
 * The per-platform half.
 *
 * iOS keeps the Android design: a scrolling column of outlined fields with the Update button pinned
 * to the bottom edge, where a thumb reaches it. Web lays the same form out as a bounded page panel —
 * a 56dp button spanning a desktop window would read as a banner, not a control.
 */
@Composable
internal expect fun ProfileContent(
    form: ProfileForm,
    onDisplayNameChange: (String) -> Unit,
    onAgreementChange: (Boolean) -> Unit,
    onUpdate: () -> Unit,
    onConfirmLeave: () -> Unit,
    onDismissLeave: () -> Unit,
    modifier: Modifier,
)
