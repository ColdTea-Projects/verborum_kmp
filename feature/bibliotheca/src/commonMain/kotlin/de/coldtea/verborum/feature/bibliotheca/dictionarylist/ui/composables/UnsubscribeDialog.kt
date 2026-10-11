package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import de.coldtea.verborum.core.designsystem.theme.scriptedText
import de.coldtea.verborum.core.localization.strings

/**
 * Confirmation for removing a downloaded dictionary.
 *
 * Worth asking because of what the message says and the action does not: the **practice progress
 * goes too**. The dictionary itself can be downloaded again from the Forum, so this is not
 * destructive in the way deleting your own is — but the levels are local, and nothing brings them
 * back.
 */
@Composable
internal fun UnsubscribeDialog(
    dictionaryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // The name is another user's text, in whatever script they study; on web the sentence around
        // it may have no face that can draw it.
        title = { Text(scriptedText(strings.unsubscribeDialogTitle(dictionaryName))) },
        text = { Text(strings.unsubscribeDialogMessage) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = strings.dictionaryOptionsUnsubscribe,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
