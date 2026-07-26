package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
        title = { Text("Delete dictionary") },
        text = { Text("“$dictionaryName” and all of its words will be deleted.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
