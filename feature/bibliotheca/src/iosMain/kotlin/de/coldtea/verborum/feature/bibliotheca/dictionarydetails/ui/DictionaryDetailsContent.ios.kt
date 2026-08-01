package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables.PracticeModeButton
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables.WordListItem
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DeleteDictionaryDialog
import de.coldtea.verborum.core.localization.strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun DictionaryDetailsContent(
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
                ContentColumn {
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
                                text = strings.words,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (details.words.isEmpty()) {
                            item {
                                Text(
                                    text = strings.noWordsYet,
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
                        Text(text = strings.addWordTitle, style = MaterialTheme.typography.titleSmall)
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
                            text = strings.deleteDictionary,
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
            text = strings.test,
            icon = VerborumIcons.Check,
            containerColor = MaterialTheme.colorScheme.primary,
            isEnabled = canTest,
            onClick = onTestClick,
            onUnavailableClick = { onUnavailableMode(true) },
            modifier = Modifier.weight(1f),
        )
        PracticeModeButton(
            text = strings.selfPractice,
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
