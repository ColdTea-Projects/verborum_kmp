package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.strings
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.DirectionOption
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestDirection

/**
 * Asks which way round the test should go, as the step between tapping Test and the first question.
 *
 * The directions are the choices themselves rather than a selection plus a confirm button: there are
 * only two, and tapping one is the whole decision. Dismissing therefore leaves the test — a dialog
 * with nothing behind it but an empty screen has no state to return to.
 */
@Composable
internal fun TestDirectionDialog(
    options: List<DirectionOption>,
    onChoose: (TestDirection) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.chooseTestDirection) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text(
                    text = strings.testDirectionSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                options.forEach { option ->
                    OutlinedButton(
                        onClick = { onChoose(option.direction) },
                        shape = Shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.buttonHeight)
                            .pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Text(text = option.label, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
