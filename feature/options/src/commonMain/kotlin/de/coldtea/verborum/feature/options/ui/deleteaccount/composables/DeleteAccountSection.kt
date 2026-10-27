package de.coldtea.verborum.feature.options.ui.deleteaccount.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.LocalSnackbarHostState
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.feature.options.ui.deleteaccount.DeleteAccountNotice
import de.coldtea.verborum.feature.options.ui.deleteaccount.DeleteAccountStep
import de.coldtea.verborum.feature.options.ui.deleteaccount.DeleteAccountViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * The destructive entry at the bottom of the profile page, apart from the form, and the whole flow
 * behind it (`docs/account-deletion-guide.md` §3). Deleting needs the network and a fresh sign-in, so
 * the button is off while offline and a started flow is withdrawn when the connection drops.
 */
@Composable
internal fun DeleteAccountSection(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DeleteAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val snackbarHost = LocalSnackbarHostState.current

    // A warning or a final dialog cannot be completed offline, so it is withdrawn.
    LaunchedEffect(isOnline) {
        if (!isOnline) viewModel.onDismiss()
    }

    val noticeText = when (state.notice) {
        DeleteAccountNotice.ReauthenticationRequired -> strings.deleteAccountErrorReauth
        DeleteAccountNotice.DifferentAccount -> strings.deleteAccountErrorDifferentAccount
        DeleteAccountNotice.SignInFailed -> strings.deleteAccountErrorSignIn
        DeleteAccountNotice.Unknown -> strings.deleteAccountErrorUnknown
        null -> null
    }
    LaunchedEffect(noticeText) {
        if (noticeText != null) {
            viewModel.onNoticeShown()
            snackbarHost.showSnackbar(message = noticeText, duration = SnackbarDuration.Short)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()

        Spacer(Modifier.height(Spacing.large))

        OutlinedButton(
            onClick = viewModel::onDeleteClick,
            enabled = isOnline && state.step == DeleteAccountStep.Idle,
            shape = Shapes.large,
            border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.buttonHeight)
                .pointerHoverIcon(PointerIcon.Hand),
        ) {
            Text(text = strings.profileDeleteAccount, style = MaterialTheme.typography.titleSmall)
        }

        Spacer(Modifier.height(Spacing.small))

        Text(
            text = strings.profileDeleteAccountHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    when (state.step) {
        DeleteAccountStep.Warning -> DeleteAccountWarningDialog(
            acknowledged = state.acknowledged,
            onAcknowledgedChange = viewModel::onAcknowledgedChange,
            onContinue = viewModel::onContinue,
            onDismiss = viewModel::onDismiss,
        )

        DeleteAccountStep.Confirm -> DeleteAccountConfirmDialog(
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismiss,
        )

        DeleteAccountStep.Deleting -> DeleteAccountProgressDialog()

        DeleteAccountStep.Idle, DeleteAccountStep.SigningIn -> Unit
    }
}
