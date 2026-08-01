package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.model.ALL_TAGS
import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.model.DictionaryTag
import de.coldtea.verborum.core.localization.strings

/** Picks one of the supported languages. Read-only text field plus a menu, so typing cannot stray. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageDropdown(
    label: String,
    selected: SupportedLanguage?,
    onSelect: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.displayName.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = VerborumIcons.ChevronDown,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconMedium),
                )
            },
            shape = Shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { isExpanded = true },
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.heightIn(max = Dimens.sheetMaxHeight),
        ) {
            SupportedLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onSelect(language)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

/** The tag catalogue as toggleable chips; selected codes are what gets stored. */
@Composable
internal fun TagSelector(
    selectedCodes: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = strings.tags,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(
            // A tall catalogue would otherwise push the save button off a phone screen.
            modifier = Modifier.fillMaxWidth().heightIn(max = TagListMaxHeight),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            items(ALL_TAGS.chunked(TagsPerRow)) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                ) {
                    row.forEach { tag ->
                        TagChip(
                            tag = tag,
                            isSelected = tag.code in selectedCodes,
                            onClick = { onToggle(tag.code) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(TagsPerRow - row.size) { Column(modifier = Modifier.weight(1f)) {} }
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: DictionaryTag,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.pill,
        color = if (isSelected) accent.copy(alpha = SelectedAlpha) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = Dimens.border,
            color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Text(
            text = tag.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        )
    }
}

private val TagListMaxHeight = 160.dp

private const val TagsPerRow = 3
private const val SelectedAlpha = 0.12f
