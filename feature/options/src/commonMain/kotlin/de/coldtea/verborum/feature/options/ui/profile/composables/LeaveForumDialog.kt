package de.coldtea.verborum.feature.options.ui.profile.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import de.coldtea.verborum.core.localization.LocalStrings

/**
 * Confirmation for leaving the Forum. Shared across platforms — unlike the form around it, a
 * decision this consequential wants the same deliberate second step in a browser as on a phone, and
 * `AlertDialog` is already how both targets ask (see Bibliotheca's delete confirmation).
 *
 * Worth a dialog because the consequence reaches further than
 * the switch suggests: the server makes **every** one of the user's dictionaries private, and
 * everyone who downloaded them loses access. Nothing is deleted, which the message says so that the
 * decision does not read as destructive when it is reversible.
 */
@Composable
internal fun LeaveForumDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.leaveForumDialogTitle) },
        text = { Text(strings.leaveForumDialogMessage) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = strings.leaveForumDialogConfirm,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.leaveForumDialogCancel) }
        },
    )
}
