package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.composables.DropdownField
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.model.TagSection
import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.model.DictionaryTag
import de.coldtea.verborum.core.localization.strings

/** Picks one of the supported languages. Read-only text field plus a menu, so typing cannot stray. */
@Composable
internal fun LanguageDropdown(
    label: String,
    selected: SupportedLanguage?,
    onSelect: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownField(
        label = label,
        value = selected?.let { strings.languageName(it.code) }.orEmpty(),
        options = SupportedLanguage.entries,
        optionLabel = { strings.languageName(it.code) },
        onSelect = onSelect,
        modifier = modifier,
    )
}

/**
 * The tag catalogue as toggleable chips, in its three groups, behind a collapsed header.
 *
 * Level, topic and exam are different questions — how far along, about what, and for which
 * certificate — and fifty chips in one undifferentiated block is a wall rather than a choice.
 * Tagging is optional, so the catalogue stays folded away until asked for; expanded, it grows the
 * screen's own scroll rather than scrolling inside itself, which on a touch screen would fight the
 * gesture that moves the form.
 */
@Composable
internal fun TagSelector(
    selectedCodes: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (isExpanded) ExpandedRotation else CollapsedRotation)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .pointerHoverIcon(PointerIcon.Hand)
                .heightIn(min = Dimens.touchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.tags,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = strings.tagsHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = VerborumIcons.ChevronDown,
                contentDescription = if (isExpanded) strings.tagSelectorExpanded else strings.tagSelectorCollapsed,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconLarge).rotate(chevronRotation),
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                TagSection.entries.forEach { section ->
                    Text(
                        text = section.title(strings),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = Spacing.small),
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        section.tags.forEach { tag ->
                            TagChip(
                                tag = tag,
                                isSelected = tag.code in selectedCodes,
                                onClick = { onToggle(tag.code) },
                            )
                        }
                    }
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
        shape = Shapes.small,
        color = if (isSelected) accent else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = Dimens.border,
            color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Text(
            text = tag.label(strings),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
        )
    }
}

private const val CollapsedRotation = -90f
private const val ExpandedRotation = 0f
