package de.coldtea.verborum.feature.forum.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.TagSection
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.SupportedLanguage
import de.coldtea.verborum.feature.forum.domain.MarketplaceLanguagePair
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.ui.list.label

/** The search criteria as a phone screen, with Search pinned below the scrolling form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun ForumSearchContent(
    state: ForumSearchUiState,
    onAddLanguagePair: () -> Unit,
    onDismissLanguagePicker: () -> Unit,
    onPickFirstLanguage: (SupportedLanguage) -> Unit,
    onPickSecondLanguage: (SupportedLanguage) -> Unit,
    onRemoveLanguagePair: (MarketplaceLanguagePair) -> Unit,
    onToggleTag: (String) -> Unit,
    onPublisherNameChanged: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current

    if (state.isPickingPair) {
        ModalBottomSheet(
            onDismissRequest = onDismissLanguagePicker,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            val first = state.pickedFirstLanguage

            Text(
                text = first
                    ?.let { strings.forumLanguagePairPickSecond(strings.languageName(it.code)) }
                    ?: strings.forumLanguagePairPickFirst,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.small),
            )

            LazyColumn(modifier = Modifier.heightIn(max = Dimens.sheetMaxHeight)) {
                items(
                    // The first language is not offered as its own partner: `EN-EN` is a 400.
                    SupportedLanguage.entries.filterNot { it == first },
                    key = SupportedLanguage::code,
                ) { language ->
                    Text(
                        text = strings.languageName(language.code),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (first == null) {
                                    onPickFirstLanguage(language)
                                } else {
                                    onPickSecondLanguage(language)
                                }
                            }
                            .heightIn(min = Dimens.touchTarget)
                            .padding(horizontal = Spacing.large, vertical = Spacing.medium),
                    )
                }
            }
        }
    }

    ContentColumn(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(strings.forumSearchUser, null, topPadding = Spacing.small)

            OutlinedTextField(
                value = state.publisherName,
                onValueChange = onPublisherNameChanged,
                singleLine = true,
                isError = state.isPublisherNameTooShort,
                label = { Text(strings.forumFilterUserHint) },
                supportingText = {
                    if (state.isPublisherNameTooShort) {
                        Text(
                            strings.forumSearchUserTooShort(
                                MarketplaceListingFilter.MIN_PUBLISHER_NAME_LENGTH,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
            )

            SectionTitle(
                strings.forumSearchLanguagePairs,
                strings.forumSearchLanguagePairsHint,
                topPadding = Spacing.large,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
            ) {
                state.languagePairs.forEach { pair ->
                    RemovableChip(
                        label = pair.label(strings),
                        removeDescription = strings.forumRemoveLanguagePair(pair.label(strings)),
                        onRemove = { onRemoveLanguagePair(pair) },
                    )
                }

                if (state.canAddLanguagePair) {
                    AddChip(label = strings.forumAddLanguagePair, onClick = onAddLanguagePair)
                }
            }

            SectionTitle(strings.forumFilterTagsTitle, null, topPadding = Spacing.large)

            // Grouped by section rather than poured into one block: thirty-odd tags in a single
            // wrap is a wall, and Level, Topic and Exam are three different questions. The same
            // headings and the same order the library's tag picker uses.
            TagSection.entries.forEach { section ->
                Text(
                    text = section.title(strings),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.medium),
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.extraSmall),
                ) {
                    section.tags.forEach { tag ->
                        SelectableChip(
                            label = tag.label(strings),
                            isSelected = tag.code in state.tags,
                            // At the cap, only the already-selected ones still respond — the
                            // server refuses an eleventh outright.
                            isEnabled = state.canAddTag(tag.code),
                            onClick = { onToggleTag(tag.code) },
                        )
                    }
                }
            }

        }

        Button(
            onClick = onSearch,
            enabled = state.canSearch,
            shape = Shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.medium)
                .height(Dimens.buttonHeight),
        ) {
            Text(strings.forumSearchScreenTitle, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SectionTitle(title: String, hint: String?, topPadding: androidx.compose.ui.unit.Dp = Spacing.medium) {
    Column(modifier = Modifier.padding(top = topPadding)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        hint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RemovableChip(label: String, removeDescription: String, onRemove: () -> Unit) {
    Surface(
        shape = Shapes.pill,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.clickable(onClick = onRemove),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            modifier = Modifier.padding(
                start = Spacing.small,
                end = Spacing.extraSmall,
                top = Spacing.extraSmall,
                bottom = Spacing.extraSmall,
            ),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Icon(
                imageVector = VerborumIcons.Close,
                contentDescription = removeDescription,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(Dimens.iconSmall),
            )
        }
    }
}

@Composable
private fun AddChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = Shapes.pill,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        ) {
            Icon(
                imageVector = VerborumIcons.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconSmall),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = Shapes.pill,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.clickable(enabled = isEnabled, onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        )
    }
}
