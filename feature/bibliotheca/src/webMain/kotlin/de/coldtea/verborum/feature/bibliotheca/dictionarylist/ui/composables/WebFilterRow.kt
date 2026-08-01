package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.WebTextAction
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardButton
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardTextField
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.LanguageKeyboardPopup
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.LocalKeyboardController
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort
import de.coldtea.verborum.core.localization.strings

/**
 * Search, language filters and sort order in one row above the grid.
 *
 * The design shows only a search box and a sort menu; the two language filters are kept because the
 * app has them and dropping them would take a working feature away, and they fold into the same row
 * as menus rather than opening a sheet.
 *
 * All on one line while there is room for it. The three menus hold a minimum width, so on a narrow
 * window the search box is what gives — squeezed down to a few characters, which is exactly the
 * field a user needs to be able to type into. Below that width the search takes a line of its own
 * and the menus wrap underneath it.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    // The keyboard offers whichever languages the filters are set to — a search for a Greek word
    // needs a Greek keyboard, and before a filter is chosen there is nothing to offer.
    val keyboardLanguages = listOfNotNull(fromLanguage?.code, toLanguage?.code)
    val primaryLanguage = keyboardLanguages.firstOrNull().orEmpty()
    val alternativeLanguage = keyboardLanguages.getOrNull(1)

    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        SearchField(
            query = query,
            keyboardLanguage = primaryLanguage,
            alternativeKeyboardLanguage = alternativeLanguage,
            onQueryChanged = onQueryChanged,
            modifier = fieldModifier,
        )
    }

    val filters: @Composable () -> Unit = {
        LanguageMenu(
            selected = fromLanguage,
            emptyLabel = "${strings.anyLanguage} ${strings.fromLanguage.lowercase()}",
            onSelect = onFromLanguageChanged,
        )
        LanguageMenu(
            selected = toLanguage,
            emptyLabel = "${strings.anyLanguage} ${strings.toLanguage.lowercase()}",
            onSelect = onToLanguageChanged,
        )

        SortMenu(sort = sort, onSelect = onSortChanged)

        // Only earns its place when something is actually filtered.
        if (hasActiveFilters) {
            WebTextAction(label = strings.clear, onClick = onClearFilters)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth >= SingleRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                search(Modifier.weight(1f))
                filters()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                // Full width, so there is always room to type — the menus give way, not the field.
                search(Modifier.fillMaxWidth())

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    filters()
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    keyboardLanguage: String,
    alternativeKeyboardLanguage: String?,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = LocalKeyboardController.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KeyboardTextField(
            id = SearchFieldGroup,
            order = 0,
            cardId = SearchFieldGroup,
            languageCode = keyboardLanguage,
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.weight(1f),
            placeholder = strings.searchDictionaries,
            leadingIcon = VerborumIcons.Search,
            // A query is not a word surface: someone may well search a Greek dictionary by its
            // English name, so the keyboard here helps rather than restricts.
            restrictToKeyboard = false,
        )

        Box {
            KeyboardButton(group = SearchFieldGroup, languageCode = keyboardLanguage)

            if (controller.isOpenFor(SearchFieldGroup)) {
                LanguageKeyboardPopup(
                    language1 = keyboardLanguage,
                    controller = controller,
                    language2 = alternativeKeyboardLanguage,
                    isExtendedKeyboard = true,
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

/**
 * Below this the search box would be squeezed past usefulness by the three menus beside it, so it
 * takes a line of its own instead: a search field a few characters wide is not a search field.
 */
private val SingleRow = 620.dp

private const val SearchFieldGroup = "dictionary-search"
