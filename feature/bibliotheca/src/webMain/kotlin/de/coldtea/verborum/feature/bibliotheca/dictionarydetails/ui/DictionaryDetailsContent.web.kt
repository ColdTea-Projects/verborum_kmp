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
import de.coldtea.verborum.core.designsystem.component.LocalNavigateBack
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.WebBackLink
import de.coldtea.verborum.core.designsystem.component.WebEyebrow
import de.coldtea.verborum.core.designsystem.component.WebOutlinedButton
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPanel
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables.WebPracticeTile
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables.WebWordRow
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.WordUi
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DeleteDictionaryDialog

/**
 * One dictionary as a desktop page: the two practice modes as tiles, then every word in a bordered
 * panel, then the two things you can do to the dictionary itself.
 *
 * The page still calls `RegisterTopBar`, even though the web shell draws no top bar: an empty title
 * is how a destination tells the shell it wants no chrome at all, so a registration is what keeps
 * the sidebar in place.
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
    modifier: Modifier,
) {
    val navigateBack = LocalNavigateBack.current

    when (val details = state.details) {
        DictionaryDetailsState.Loading -> {
            RegisterTopBar(title = "Dictionary")
            LoadingState(modifier)
        }

        DictionaryDetailsState.Failed -> {
            RegisterTopBar(title = "Dictionary")
            ErrorState(
                message = "This dictionary could not be loaded.",
                modifier = modifier,
                onRetry = onRetry,
            )
        }

        // Already on its way out — rendering anything here would only flash.
        DictionaryDetailsState.Deleted -> Unit

        is DictionaryDetailsState.Success -> {
            RegisterTopBar(title = details.name, showBackButton = true)

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

            Box(
                modifier = modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                ContentPane(maxWidth = ContentWidth.Web.detail) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        WebPageSpacer(Spacing.extraLarge)

                        WebBackLink(label = "Back to dictionaries", onClick = navigateBack)

                        WebPageSpacer(Spacing.small)

                        WebPageTitle(
                            title = details.name,
                            subtitle = "${details.languagePair} · ${wordCountLabel(details.words.size)}",
                        )

                        WebPageSpacer()

                        PracticeTiles(
                            canTest = details.canTest,
                            canSelfPractice = details.canSelfPractice,
                            onTestClick = onTestClick,
                            onSelfPracticeClick = onSelfPracticeClick,
                            onUnavailableMode = onUnavailableMode,
                        )

                        if (details.tags.isNotEmpty()) {
                            Text(
                                text = details.tags.joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.medium),
                            )
                        }

                        WebPageSpacer()

                        WordListPanel(
                            words = details.words,
                            onEditWordClick = onEditWordClick,
                            onDeleteWord = onDeleteWord,
                        )

                        WebPageSpacer()

                        WebOutlinedButton(label = "+ Add Word", onClick = onCreateWordClick)

                        WebPageSpacer(Spacing.medium)

                        WebPrimaryButton(
                            label = "Delete Dictionary",
                            onClick = { showDeleteDialog = true },
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
            label = "Test",
            icon = VerborumIcons.Check,
            containerColor = MaterialTheme.colorScheme.primary,
            isEnabled = canTest,
            onClick = onTestClick,
            onUnavailableClick = { onUnavailableMode(true) },
            modifier = Modifier.weight(1f),
        )
        WebPracticeTile(
            label = "Self Practice",
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
) {
    WebPanel {
        WebEyebrow("Word list")

        WebPageSpacer(Spacing.medium)

        if (words.isEmpty()) {
            Text(
                text = "No words yet. Add the first one below.",
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
                )
            }
        }
    }
}
