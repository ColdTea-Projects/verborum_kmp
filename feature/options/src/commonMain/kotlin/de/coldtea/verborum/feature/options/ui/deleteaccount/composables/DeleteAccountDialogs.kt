package de.coldtea.verborum.feature.options.ui.deleteaccount.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings

/**
 * Step A (`docs/account-deletion-guide.md` §6): what is deleted, that it cannot be undone, and the
 * "I understand" checkbox that unlocks Continue. Shared across platforms like [LeaveForumDialog]: a
 * decision this consequential wants the same deliberate step in a browser as on a phone. The body
 * scrolls, so a short window or a large font never hides the checkbox or the buttons.
 */
@Composable
internal fun DeleteAccountWarningDialog(
    acknowledged: Boolean,
    onAcknowledgedChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.deleteAccountTitle) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(strings.deleteAccountIntro)

                Spacer(Modifier.height(Spacing.medium))

                Text(strings.deleteAccountDeletesHeader, fontWeight = FontWeight.SemiBold)

                listOf(
                    strings.deleteAccountItemProfile,
                    strings.deleteAccountItemDictionaries,
                    strings.deleteAccountItemVault,
                    strings.deleteAccountItemRatings,
                    strings.deleteAccountItemDevices,
                    strings.deleteAccountItemSignIn,
                ).forEach { item ->
                    Text("• $item", modifier = Modifier.padding(start = Spacing.small, top = Spacing.extraSmall))
                }

                Spacer(Modifier.height(Spacing.medium))

                Text(
                    text = strings.deleteAccountIrreversible,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )

                Spacer(Modifier.height(Spacing.medium))
                Text(strings.deleteAccountOthers)

                Spacer(Modifier.height(Spacing.medium))
                Text(strings.deleteAccountSignInNotice)

                Spacer(Modifier.height(Spacing.medium))
                Text(strings.deleteAccountNewAccount)

                Spacer(Modifier.height(Spacing.large))

                // The whole row is the touch target, as in the Forum terms.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = acknowledged, role = Role.Checkbox, onValueChange = onAcknowledgedChange),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = acknowledged, onCheckedChange = null)
                    Text(strings.deleteAccountAcknowledge, modifier = Modifier.padding(start = Spacing.medium))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue, enabled = acknowledged) {
                Text(
                    text = strings.deleteAccountContinue,
                    color = if (acknowledged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

/** Step D: the last question, shown only after a fresh sign-in as the same account. */
@Composable
internal fun DeleteAccountConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.deleteAccountConfirmTitle) },
        text = { Text(strings.deleteAccountConfirmMessage) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.delete, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

/**
 * Blocks the screen while the server call runs. It cannot be dismissed: leaving a delete half-way
 * gives no benefit and hides the result.
 */
@Composable
internal fun DeleteAccountProgressDialog() {
    val strings = LocalStrings.current

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = Shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(
                modifier = Modifier.padding(Spacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(Dimens.iconLarge))
                Text(strings.deleteAccountProgress, modifier = Modifier.padding(start = Spacing.medium))
            }
        }
    }
}
