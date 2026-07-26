package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort

/** The search field revealed by the top bar's magnifier. */
@Composable
internal fun DictionarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = Shapes.large,
        placeholder = { Text("Search dictionaries") },
        leadingIcon = {
            Icon(
                imageVector = VerborumIcons.Search,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconMedium),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = VerborumIcons.Close,
                    contentDescription = "Clear search",
                    modifier = Modifier
                        .size(Dimens.iconMedium)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onQueryChange("") },
                )
            }
        },
        // A search field whose Return key does nothing reads as broken; filtering is live, so the
        // action just closes the keyboard.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/**
 * The scrollable chip row: From / To language, the sort order, and Clear. Each opens its own sheet
 * except Clear, which resets everything.
 */
@Composable
internal fun DictionaryFilterBar(
    fromLanguage: SupportedLanguage?,
    toLanguage: SupportedLanguage?,
    sort: DictionarySort,
    onFromClick: () -> Unit,
    onToClick: () -> Unit,
    onSortClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            text = "From: ${fromLanguage?.displayName ?: ANY_LANGUAGE}",
            onClick = onFromClick,
            trailingIcon = VerborumIcons.ChevronDown,
            isHighlighted = fromLanguage != null,
        )
        FilterChip(
            text = "To: ${toLanguage?.displayName ?: ANY_LANGUAGE}",
            onClick = onToClick,
            trailingIcon = VerborumIcons.ChevronDown,
            isHighlighted = toLanguage != null,
        )
        FilterChip(
            text = sort.label,
            onClick = onSortClick,
            leadingIcon = VerborumIcons.Sort,
            trailingIcon = VerborumIcons.ChevronDown,
        )
        FilterChip(
            text = "Clear",
            onClick = onClearClick,
            leadingIcon = VerborumIcons.Close,
        )
    }
}

@Composable
private fun FilterChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    isHighlighted: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    val contentColor = if (isHighlighted) accent else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.pill,
        color = if (isHighlighted) {
            accent.copy(alpha = HighlightAlpha)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = Dimens.border,
            color = if (isHighlighted) accent else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            leadingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
            trailingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
            }
        }
    }
}

internal const val ANY_LANGUAGE = "Any"

private const val HighlightAlpha = 0.10f
