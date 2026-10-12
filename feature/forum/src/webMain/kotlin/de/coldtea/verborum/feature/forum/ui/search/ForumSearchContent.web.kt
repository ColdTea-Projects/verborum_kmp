package de.coldtea.verborum.feature.forum.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.WebChip
import de.coldtea.verborum.core.designsystem.component.WebFieldLabel
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPanel
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.component.WebTextField
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.TagSection
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.SupportedLanguage
import de.coldtea.verborum.feature.forum.domain.MarketplaceLanguagePair
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.ui.list.label

/**
 * The search criteria as a desktop form.
 *
 * The two-step language picker is a **dialog** rather than a bottom sheet — a sheet is a phone
 * idiom, and a pointer expects a modal it can dismiss with Escape. The criteria themselves are the
 * same three, in the same order, so the two platforms stay one feature.
 */
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
        val first = state.pickedFirstLanguage

        AlertDialog(
            onDismissRequest = onDismissLanguagePicker,
            title = {
                Text(
                    first
                        ?.let { strings.forumLanguagePairPickSecond(strings.languageName(it.code)) }
                        ?: strings.forumLanguagePairPickFirst,
                )
            },
            text = {
                Column(modifier = Modifier.heightIn(max = PickerHeight).verticalScroll(rememberScrollState())) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        // The first language is never offered as its own partner: `EN-EN` is a 400.
                        SupportedLanguage.entries.filterNot { it == first }.forEach { language ->
                            WebChip(
                                label = strings.languageName(language.code),
                                isSelected = false,
                                onClick = {
                                    if (first == null) {
                                        onPickFirstLanguage(language)
                                    } else {
                                        onPickSecondLanguage(language)
                                    }
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissLanguagePicker) { Text(strings.cancel) }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ContentPane(maxWidth = ContentWidth.Web.detail) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                WebPageSpacer(Spacing.extraLarge)

                WebPageTitle(
                    title = strings.forumSearchScreenTitle,
                    subtitle = strings.forumSearchScreenSubtitle,
                )

                WebPageSpacer()

                WebPanel {
                    WebFieldLabel(strings.forumSearchUser)
                    WebTextField(
                        value = state.publisherName,
                        onValueChange = onPublisherNameChanged,
                        placeholder = strings.forumFilterUserHint,
                    )

                    if (state.isPublisherNameTooShort) {
                        Text(
                            text = strings.forumSearchUserTooShort(
                                MarketplaceListingFilter.MIN_PUBLISHER_NAME_LENGTH,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    WebPageSpacer(Spacing.large)

                    WebFieldLabel(strings.forumSearchLanguagePairs)
                    Text(
                        text = strings.forumSearchLanguagePairsHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
                    ) {
                        state.languagePairs.forEach { pair ->
                            WebChip(
                                label = "${pair.label(strings)}  ×",
                                isSelected = true,
                                onClick = { onRemoveLanguagePair(pair) },
                            )
                        }

                        if (state.canAddLanguagePair) {
                            WebChip(
                                label = strings.forumAddLanguagePair,
                                isSelected = false,
                                onClick = onAddLanguagePair,
                            )
                        }
                    }

                    WebPageSpacer(Spacing.large)

                    WebFieldLabel(strings.forumFilterTagsTitle)

                    // Grouped by section, as on the phone and in the library's own tag picker:
                    // thirty-odd tags in one wrap is a wall, and Level, Topic and Exam are three
                    // different questions.
                    TagSection.entries.forEach { section ->
                        Text(
                            text = section.title(strings),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.small),
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.extraSmall),
                        ) {
                            section.tags.forEach { tag ->
                                WebChip(
                                    label = tag.label(strings),
                                    isSelected = tag.code in state.tags,
                                    // At the cap only the selected ones still respond; the server
                                    // refuses an eleventh outright.
                                    isEnabled = state.canAddTag(tag.code),
                                    onClick = { onToggleTag(tag.code) },
                                )
                            }
                        }
                    }

                    WebPageSpacer(Spacing.large)

                    WebPrimaryButton(
                        label = strings.forumSearchScreenTitle,
                        onClick = onSearch,
                        isEnabled = state.canSearch,
                        modifier = Modifier.widthIn(max = ContentWidth.form),
                    )
                }

                WebPageSpacer(Spacing.extraLarge)
            }
        }
    }
}

/** Caps the picker so a nineteen-language list scrolls inside the dialog rather than past it. */
private val PickerHeight = 360.dp
