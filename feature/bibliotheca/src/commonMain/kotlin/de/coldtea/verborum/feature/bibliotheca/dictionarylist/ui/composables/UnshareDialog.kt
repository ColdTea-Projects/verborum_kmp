package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import de.coldtea.verborum.core.designsystem.theme.scriptedText
import de.coldtea.verborum.core.localization.strings

/**
 * Confirmation for taking a dictionary out of the Forum.
 *
 * The switch does not show the half that matters: **everyone who downloaded it loses access**. Going
 * the other way needs no dialog — nothing is lost by sharing.
 */
@Composable
internal fun UnshareDialog(
    dictionaryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(scriptedText(strings.unshareDialogTitle(dictionaryName))) },
        text = { Text(strings.unshareDialogMessage) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = strings.unshareDialogConfirm,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
