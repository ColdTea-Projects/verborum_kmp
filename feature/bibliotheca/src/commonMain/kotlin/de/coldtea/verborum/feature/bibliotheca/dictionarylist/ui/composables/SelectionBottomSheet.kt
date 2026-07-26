package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Spacing

/** One row of a [SelectionBottomSheet]. */
internal data class SelectionOption(
    val label: String,
    val isSelected: Boolean,
    val onSelect: () -> Unit,
)

/**
 * A titled single-choice list in a modal sheet: the selected row is bold and check-marked. Backs the
 * From/To language filters and the sort order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionBottomSheet(
    title: String,
    options: List<SelectionOption>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Opens fully so the long language list is usable without a drag first.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.large)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = Spacing.large,
                    end = Spacing.large,
                    top = Spacing.small,
                    bottom = Spacing.small,
                ),
            )
            LazyColumn(
                // Caps the height so a long list scrolls while a short one still wraps.
                modifier = Modifier.fillMaxWidth().heightIn(max = Dimens.sheetMaxHeight),
            ) {
                items(options, key = SelectionOption::label) { option ->
                    SelectionRow(option = option, onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(option: SelectionOption, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable {
                option.onSelect()
                onDismiss()
            }
            .padding(horizontal = Spacing.large, vertical = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (option.isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (option.isSelected) {
            Icon(
                imageVector = VerborumIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconMedium),
            )
        }
    }
}
