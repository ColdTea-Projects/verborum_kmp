package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.WebTextAction
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort

/**
 * Search, language filters and sort order in one row above the grid.
 *
 * The design shows only a search box and a sort menu; the two language filters are kept because the
 * app has them and dropping them would take a working feature away, and they fold into the same row
 * as menus rather than opening a sheet.
 */
@Composable
internal fun WebFilterRow(
    query: String,
    fromLanguage: SupportedLanguage?,
    toLanguage: SupportedLanguage?,
    sort: DictionarySort,
    hasActiveFilters: Boolean,
    onQueryChanged: (String) -> Unit,
    onFromLanguageChanged: (SupportedLanguage?) -> Unit,
    onToLanguageChanged: (SupportedLanguage?) -> Unit,
    onSortChanged: (DictionarySort) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchField(
            query = query,
            onQueryChanged = onQueryChanged,
            modifier = Modifier.weight(1f),
        )

        LanguageMenu(
            selected = fromLanguage,
            emptyLabel = "Any from",
            onSelect = onFromLanguageChanged,
        )
        LanguageMenu(
            selected = toLanguage,
            emptyLabel = "Any to",
            onSelect = onToLanguageChanged,
        )

        SortMenu(sort = sort, onSelect = onSortChanged)

        // Only earns its place when something is actually filtered.
        if (hasActiveFilters) {
            WebTextAction(label = "Clear", onClick = onClearFilters)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(FieldHeight),
        shape = Shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = VerborumIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconSmall),
            )
            Spacer(modifier = Modifier.width(Spacing.small))

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search dictionaries…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** "Any language" plus every supported language, as a menu. */
@Composable
private fun LanguageMenu(
    selected: SupportedLanguage?,
    emptyLabel: String,
    onSelect: (SupportedLanguage?) -> Unit,
) {
    MenuField(label = selected?.displayName ?: emptyLabel) { dismiss ->
        DropdownMenuItem(
            text = { Text(emptyLabel) },
            onClick = {
                onSelect(null)
                dismiss()
            },
        )
        SupportedLanguage.entries.forEach { language ->
            DropdownMenuItem(
                text = { Text(language.displayName) },
                onClick = {
                    onSelect(language)
                    dismiss()
                },
            )
        }
    }
}

@Composable
private fun SortMenu(sort: DictionarySort, onSelect: (DictionarySort) -> Unit) {
    MenuField(label = sort.label) { dismiss ->
        DictionarySort.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                onClick = {
                    onSelect(option)
                    dismiss()
                },
            )
        }
    }
}

/** A select: the current value in a bordered box, its options in a dropdown. */
@Composable
private fun MenuField(
    label: String,
    items: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { isExpanded = true },
            modifier = Modifier.height(FieldHeight).widthIn(min = MenuMinWidth)
                .pointerHoverIcon(PointerIcon.Hand),
            shape = Shapes.medium,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = VerborumIcons.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
            }
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.heightIn(max = Dimens.sheetMaxHeight),
        ) {
            items { isExpanded = false }
        }
    }
}

private val FieldHeight = 48.dp
private val MenuMinWidth = 120.dp
