package de.coldtea.verborum.feature.bibliotheca.createword.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
import de.coldtea.verborum.feature.bibliotheca.createword.ui.composables.LanguageInputCard

@Composable
internal actual fun CreateWordContent(
    state: CreateWordUiState,
    onWordTypeChanged: (WordType) -> Unit,
    onTextChanged: (WordSide, Int, String) -> Unit,
    onGenderChanged: (WordSide, Int, Gender?) -> Unit,
    onFieldChanged: (WordSide, Int, FieldKey, String) -> Unit,
    onAddMeaning: () -> Unit,
    onRemoveMeaning: (Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    RegisterTopBar(
        title = if (state.isEditing) "Edit word" else "New word",
        subtitle = state.dictionary?.name,
        showBackButton = true,
    )

    val dictionary = state.dictionary

    when {
        state.hasFailed -> ErrorState(
            message = "This word could not be loaded.",
            modifier = modifier,
            onRetry = onRetry,
        )

        dictionary == null -> LoadingState(modifier)

        else -> ContentColumn(modifier = modifier) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(Spacing.medium))

                WordTypeChips(selected = state.wordType, onSelect = onWordTypeChanged)

                repeat(state.meaningCount) { index ->
                    Spacer(modifier = Modifier.height(Spacing.medium))

                    MeaningHeader(
                        index = index,
                        canRemove = state.meaningCount > 1,
                        onRemove = { onRemoveMeaning(index) },
                    )

                    state.sourceInputs.getOrNull(index)?.let { input ->
                        LanguageInputCard(
                            languageCode = dictionary.fromLang,
                            wordType = state.wordType,
                            input = input,
                            onTextChanged = { text -> onTextChanged(WordSide.SOURCE, index, text) },
                            onGenderChanged = { gender ->
                                onGenderChanged(WordSide.SOURCE, index, gender)
                            },
                            onFieldChanged = { key, value ->
                                onFieldChanged(WordSide.SOURCE, index, key, value)
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.small))

                    state.targetInputs.getOrNull(index)?.let { input ->
                        LanguageInputCard(
                            languageCode = dictionary.toLang,
                            wordType = state.wordType,
                            input = input,
                            onTextChanged = { text -> onTextChanged(WordSide.TARGET, index, text) },
                            onGenderChanged = { gender ->
                                onGenderChanged(WordSide.TARGET, index, gender)
                            },
                            onFieldChanged = { key, value ->
                                onFieldChanged(WordSide.TARGET, index, key, value)
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.small))

                TextButton(
                    onClick = onAddMeaning,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Text(text = "Add another meaning")
                }

                Spacer(modifier = Modifier.height(Spacing.small))
            }

            Button(
                onClick = onSave,
                enabled = state.canSave,
                shape = Shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.medium)
                    .height(Dimens.buttonHeight)
                    .pointerHoverIcon(PointerIcon.Hand),
            ) {
                Text(
                    text = when {
                        state.isSaving -> "Saving…"
                        state.isEditing -> "Save changes"
                        else -> "Add word"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

/** The word type decides which grammatical fields both language cards ask for. */
@Composable
private fun WordTypeChips(selected: WordType, onSelect: (WordType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        WordType.entries.forEach { type ->
            val isSelected = type == selected
            val accent = MaterialTheme.colorScheme.primary

            Surface(
                onClick = { onSelect(type) },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                shape = Shapes.pill,
                color = if (isSelected) {
                    accent.copy(alpha = SelectedAlpha)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                border = BorderStroke(
                    width = Dimens.border,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        horizontal = Spacing.medium,
                        vertical = Spacing.small,
                    ),
                )
            }
        }
    }
}

/** Meanings are numbered so the two language cards below are visibly a pair. */
@Composable
private fun MeaningHeader(index: Int, canRemove: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "MEANING ${index + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (canRemove) {
            TextButton(
                onClick = onRemove,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            ) {
                Text(text = "Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private const val SelectedAlpha = 0.12f
