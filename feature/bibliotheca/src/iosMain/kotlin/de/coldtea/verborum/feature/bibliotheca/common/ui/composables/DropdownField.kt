package de.coldtea.verborum.feature.bibliotheca.common.ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes

/**
 * A read-only text field that opens a menu of [options] — the form's "pick one of these" control.
 *
 * Built on `ExposedDropdownMenuBox` rather than a `clickable` text field: a read-only
 * `OutlinedTextField` still consumes the touch itself, so a `Modifier.clickable` sitting on it never
 * fires and the menu never opens. `menuAnchor` is the seam that is actually wired to the field's
 * input, and it also sizes the menu to the field and keeps it clear of the keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> DropdownField(
    label: String,
    value: String,
    options: List<T>,
    // Composable because every label goes through `strings`, which reads the current UI language.
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (isExpanded) OpenRotation else ClosedRotation)

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = VerborumIcons.ChevronDown,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconMedium).rotate(chevronRotation),
                )
            },
            shape = Shapes.medium,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .pointerHoverIcon(PointerIcon.Hand),
        )

        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

private const val ClosedRotation = 0f
private const val OpenRotation = 180f
