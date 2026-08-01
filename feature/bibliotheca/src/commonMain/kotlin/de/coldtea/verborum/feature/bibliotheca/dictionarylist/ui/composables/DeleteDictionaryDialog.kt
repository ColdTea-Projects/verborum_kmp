package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import de.coldtea.verborum.core.localization.strings

/**
 * Confirmation for the destructive delete. Both platforms expect a deliberate second step here, and
 * iOS in particular expects the destructive choice to be visibly marked.
 */
@Composable
internal fun DeleteDictionaryDialog(
    dictionaryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.deleteDictionary) },
        text = { Text(strings.deleteDictionaryWarning(dictionaryName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = strings.delete, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
