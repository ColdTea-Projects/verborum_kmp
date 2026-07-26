package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables.PracticeModeButton
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables.WordListItem
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.REQUIRED_WORDS_FOR_TEST
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DeleteDictionaryDialog
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** One dictionary: how to practise it, the words it holds, and how to get rid of it. */
@Composable
internal fun DictionaryDetailsScreen(
    dictionaryId: String,
    onTestClick: () -> Unit,
    onSelfPracticeClick: () -> Unit,
    onCreateWordClick: () -> Unit,
    onEditWordClick: (String) -> Unit,
    onDictionaryDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryDetailsViewModel = koinViewModel { parametersOf(dictionaryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ShowSnackbarMessages(viewModel.messages)

    // Leaving is driven by the observed state, not by the delete button: the screen goes exactly
    // once, so an async delete cannot race the back navigation, and a torn-down screen can never
    // re-register the shared header with a dictionary that no longer exists.
    LaunchedEffect(state.details) {
        if (state.details is DictionaryDetailsState.Deleted) onDictionaryDeleted()
    }

    DictionaryDetailsContent(
        state = state,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onTestClick = onTestClick,
        onSelfPracticeClick = onSelfPracticeClick,
        onCreateWordClick = onCreateWordClick,
        onEditWordClick = onEditWordClick,
        onDeleteWord = viewModel::deleteWord,
        onDeleteDictionary = viewModel::deleteDictionary,
        onUnavailableMode = viewModel::explainUnavailableMode,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DictionaryDetailsContent(
    state: DictionaryDetailsUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onTestClick: () -> Unit,
    onSelfPracticeClick: () -> Unit,
    onCreateWordClick: () -> Unit,
    onEditWordClick: (String) -> Unit,
    onDeleteWord: (String) -> Unit,
    onDeleteDictionary: () -> Unit,
    onUnavailableMode: (isTest: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            RegisterTopBar(
                title = details.name,
                subtitle = "${details.languagePair} · ${wordCountLabel(details.words.size)}",
                showBackButton = true,
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

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // The same narrow column as the list, for one shape at every size.
                        .widthIn(max = ContentWidth.column)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = Spacing.large),
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        item {
                            PracticeModes(
                                canTest = details.canTest,
                                canSelfPractice = details.canSelfPractice,
                                onTestClick = onTestClick,
                                onSelfPracticeClick = onSelfPracticeClick,
                                onUnavailableMode = onUnavailableMode,
                            )
                        }

                        if (details.tags.isNotEmpty()) {
                            item { TagsRow(tags = details.tags) }
                        }

                        item {
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            Text(
                                text = "WORDS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (details.words.isEmpty()) {
                            item {
                                Text(
                                    text = "No words yet. Add the first one below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = Spacing.medium),
                                )
                            }
                        } else {
                            items(details.words, key = { word -> word.wordId }) { word ->
                                WordListItem(
                                    word = word,
                                    onEditClick = onEditWordClick,
                                    onDeleteClick = onDeleteWord,
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onCreateWordClick,
                        shape = Shapes.large,
                        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.small)
                            .height(Dimens.buttonHeight)
                            .pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Icon(
                            imageVector = VerborumIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconSmall),
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(text = "Add word", style = MaterialTheme.typography.titleSmall)
                    }

                    Button(
                        onClick = { showDeleteDialog = true },
                        shape = Shapes.large,
                        // Destructive, so it reads as such rather than as the screen's main action.
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.small, bottom = Spacing.medium)
                            .height(Dimens.buttonHeight)
                            .pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Icon(
                            imageVector = VerborumIcons.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconMedium),
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = "Delete dictionary",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeModes(
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
        PracticeModeButton(
            text = "Test",
            icon = VerborumIcons.Check,
            containerColor = MaterialTheme.colorScheme.primary,
            isEnabled = canTest,
            onClick = onTestClick,
            onUnavailableClick = { onUnavailableMode(true) },
            modifier = Modifier.weight(1f),
        )
        PracticeModeButton(
            text = "Self practice",
            icon = VerborumIcons.Book,
            containerColor = MaterialTheme.colorScheme.secondary,
            isEnabled = canSelfPractice,
            onClick = onSelfPracticeClick,
            onUnavailableClick = { onUnavailableMode(false) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TagsRow(tags: List<String>) {
    Text(
        text = tags.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.small),
    )
}

/** Kept next to the screen it explains, so the threshold and the message cannot drift apart. */
internal fun unavailableModeMessage(isTest: Boolean, wordCount: Int): String = when {
    wordCount == 0 -> "Add a word first — there is nothing to practise yet."
    isTest -> "A test needs at least $REQUIRED_WORDS_FOR_TEST different words to choose between."
    else -> "Add a word first — there is nothing to practise yet."
}
