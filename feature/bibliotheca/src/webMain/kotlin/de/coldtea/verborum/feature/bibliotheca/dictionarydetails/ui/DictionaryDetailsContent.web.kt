package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RatingCard
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.WebEyebrow
import de.coldtea.verborum.core.designsystem.component.WebOutlinedButton
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPanel
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForText
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables.WebPracticeTile
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables.SharingRow
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables.WebWordRow
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.UnshareDialog
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.UnsubscribeDialog
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.WordUi
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DeleteDictionaryDialog
import de.coldtea.verborum.core.localization.strings
import de.coldtea.verborum.core.localization.tagLabelOf

/**
 * One dictionary as a desktop page: the two practice modes as tiles, then every word in a bordered
 * panel, then the two things you can do to the dictionary itself.
 *
 * The way back lives in the shell's top bar, registered here through `RegisterTopBar` — the page
 * itself only titles its content.
 *
 * `onRefresh` goes unused — pull-to-refresh is a touch gesture, and the page re-syncs on arrival.
 */
@Composable
internal actual fun DictionaryDetailsContent(
    state: DictionaryDetailsUiState,
    @Suppress("UNUSED_PARAMETER") onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onTestClick: () -> Unit,
    onSelfPracticeClick: () -> Unit,
    onCreateWordClick: () -> Unit,
    onEditWordClick: (String) -> Unit,
    onDeleteWord: (String) -> Unit,
    onDeleteDictionary: () -> Unit,
    onUnavailableMode: (isTest: Boolean) -> Unit,
    onSharedChange: (Boolean) -> Unit,
    onConfirmUnshare: () -> Unit,
    onDismissUnshare: () -> Unit,
    onUnsubscribeClick: () -> Unit,
    onConfirmUnsubscribe: () -> Unit,
    onDismissUnsubscribe: () -> Unit,
    onRate: (Int) -> Unit,
    onRemoveRating: () -> Unit,
    modifier: Modifier,
) {
    when (val details = state.details) {
        DictionaryDetailsState.Loading -> {
            RegisterTopBar(title = strings.dictionary)
            LoadingState(modifier)
        }

        DictionaryDetailsState.Failed -> {
            RegisterTopBar(title = strings.dictionary)
            ErrorState(
                message = strings.dictionaryFailed,
                modifier = modifier,
                onRetry = onRetry,
            )
        }

        // Already on its way out — rendering anything here would only flash.
        DictionaryDetailsState.Deleted -> Unit

        is DictionaryDetailsState.Success -> {
            RegisterTopBar(
                title = details.name,
                showBackButton = true,
                backLabel = strings.backToDictionaries,
                // A user-authored name can be written in any script the canvas must be told about.
                titleFontFamily = fontFamilyForText(details.name),
            )

            var showDeleteDialog by remember { mutableStateOf(false) }

            if (showDeleteDialog) {
                DeleteDictionaryDialog(
                    dictionaryName = details.name,
                    onConfirm = {
                        showDeleteDialog = false
                        onDeleteDictionary()
                    },
                    onDismiss = { showDeleteDialog = false },
                )
            }

            if (details.isUnshareConfirmationShown) {
                UnshareDialog(
                    dictionaryName = details.name,
                    onConfirm = onConfirmUnshare,
                    onDismiss = onDismissUnshare,
                )
            }

            if (details.isUnsubscribeConfirmationShown) {
                UnsubscribeDialog(
                    dictionaryName = details.name,
                    onConfirm = onConfirmUnsubscribe,
                    onDismiss = onDismissUnsubscribe,
                )
            }

            Box(
                modifier = modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                ContentPane(maxWidth = ContentWidth.Web.detail) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        WebPageSpacer(Spacing.extraLarge)

                        WebPageTitle(
                            title = details.name,
                            // The publisher joins the subtitle only when this device knows the name.
                            subtitle = buildString {
                                append(details.languagePair)
                                append(" · ")
                                append(strings.wordCount(details.words.size))
                                details.publisherName?.let { publisher ->
                                    append(" · ")
                                    append(strings.forumPublishedBy(publisher))
                                }
                            },
                        )

                        WebPageSpacer()

                        PracticeTiles(
                            canTest = details.canTest,
                            canSelfPractice = details.canSelfPractice,
                            onTestClick = onTestClick,
                            onSelfPracticeClick = onSelfPracticeClick,
                            onUnavailableMode = onUnavailableMode,
                        )

                        val tagStrings = strings

                        if (details.tags.isNotEmpty()) {
                            Text(
                                // Stored codes, shown as words the reader knows.
                                text = details.tags.joinToString(" · ") { tagLabelOf(it, tagStrings) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.medium),
                            )
                        }

                        // Only for a download, and only once the rating is actually known.
                        if (details.isRatingVisible) {
                            WebPageSpacer()

                            RatingCard(
                                stars = details.myRating,
                                onRate = onRate,
                                onRemoveRating = onRemoveRating,
                                isUpdating = details.isRatingUpdating,
                            )
                        }

                        if (details.isSharingVisible) {
                            WebPageSpacer()

                            SharingRow(
                                isShared = details.isShared,
                                isLastShared = details.isLastShared,
                                canToggle = details.canToggleSharing,
                                onSharedChange = onSharedChange,
                            )
                        }

                        WebPageSpacer()

                        WordListPanel(
                            words = details.words,
                            onEditWordClick = onEditWordClick,
                            onDeleteWord = onDeleteWord,
                            isReadOnly = details.isImported,
                        )

                        WebPageSpacer()

                        // A download has nothing to add to: the words are its owner's.
                        if (!details.isImported) {
                            WebOutlinedButton(label = strings.addWord, onClick = onCreateWordClick)

                            WebPageSpacer(Spacing.medium)
                        }

                        // Unsubscribe for a download, Delete for an own dictionary — never both.
                        WebPrimaryButton(
                            label = if (details.isImported) {
                                strings.dictionaryOptionsUnsubscribe
                            } else {
                                strings.deleteDictionary
                            },
                            onClick = if (details.isImported) {
                                onUnsubscribeClick
                            } else {
                                { showDeleteDialog = true }
                            },
                        )

                        WebPageSpacer(Spacing.extraLarge)
                    }
                }
            }
        }
    }
}

/** The two ways to practise, as equal halves of the page's width. */
@Composable
private fun PracticeTiles(
    canTest: Boolean,
    canSelfPractice: Boolean,
    onTestClick: () -> Unit,
    onSelfPracticeClick: () -> Unit,
    onUnavailableMode: (isTest: Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        WebPracticeTile(
            label = strings.test,
            icon = VerborumIcons.Check,
            containerColor = MaterialTheme.colorScheme.primary,
            isEnabled = canTest,
            onClick = onTestClick,
            onUnavailableClick = { onUnavailableMode(true) },
            modifier = Modifier.weight(1f),
        )
        WebPracticeTile(
            label = strings.selfPractice,
            icon = VerborumIcons.Play,
            containerColor = MaterialTheme.colorScheme.secondary,
            isEnabled = canSelfPractice,
            onClick = onSelfPracticeClick,
            onUnavailableClick = { onUnavailableMode(false) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WordListPanel(
    words: List<WordUi>,
    onEditWordClick: (String) -> Unit,
    onDeleteWord: (String) -> Unit,
    isReadOnly: Boolean,
) {
    WebPanel {
        WebEyebrow(strings.wordList)

        WebPageSpacer(Spacing.medium)

        if (words.isEmpty()) {
            Text(
                text = strings.noWordsYet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // A plain column, not a lazy list: the panel sits inside the page's own scroll, and a
            // scrollable within a scrollable cannot measure itself.
            words.forEachIndexed { index, word ->
                if (index != 0) WebPageSpacer(Spacing.small)

                WebWordRow(
                    word = word,
                    onEditClick = onEditWordClick,
                    onDeleteClick = onDeleteWord,
                    isReadOnly = isReadOnly,
                )
            }
        }
    }
}
