package de.coldtea.verborum.feature.bibliotheca.createword.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordCategory
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordFormInput
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.defaultType
import de.coldtea.verborum.feature.bibliotheca.createword.ui.composables.LanguageInputCard
import de.coldtea.verborum.core.localization.strings

@Composable
internal actual fun CreateWordContent(
    state: CreateWordUiState,
    onWordTypeChanged: (WordType) -> Unit,
    onTextChanged: (WordSide, Int, String) -> Unit,
    onGenderChanged: (WordSide, Int, Gender?) -> Unit,
    onFieldChanged: (WordSide, Int, FieldKey, String) -> Unit,
    onAddMeaning: (WordSide) -> Unit,
    onRemoveMeaning: (WordSide, Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    RegisterTopBar(
        title = if (state.isEditing) strings.editWordTitle else strings.newWord,
        subtitle = state.dictionary?.name,
        showBackButton = true,
    )

    val dictionary = state.dictionary

    when {
        state.hasFailed -> ErrorState(
            message = strings.wordLoadFailed,
            modifier = modifier,
            onRetry = onRetry,
        )

        dictionary == null -> LoadingState(modifier)

        else -> ContentColumn(modifier = modifier) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(Spacing.medium))

                WordTypeChips(selected = state.wordType, onSelect = onWordTypeChanged)

                // The closed classes share one chip and pick their part of speech here.
                if (state.wordType.category == WordCategory.OTHER) {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    OtherTypeDropdown(selected = state.wordType, onSelect = onWordTypeChanged)
                }

                MeaningSection(
                    side = WordSide.SOURCE,
                    languageCode = dictionary.fromLang,
                    inputs = state.sourceInputs,
                    wordType = state.wordType,
                    onTextChanged = onTextChanged,
                    onGenderChanged = onGenderChanged,
                    onFieldChanged = onFieldChanged,
                    onAddMeaning = onAddMeaning,
                    onRemoveMeaning = onRemoveMeaning,
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                MeaningSection(
                    side = WordSide.TARGET,
                    languageCode = dictionary.toLang,
                    inputs = state.targetInputs,
                    wordType = state.wordType,
                    onTextChanged = onTextChanged,
                    onGenderChanged = onGenderChanged,
                    onFieldChanged = onFieldChanged,
                    onAddMeaning = onAddMeaning,
                    onRemoveMeaning = onRemoveMeaning,
                )

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
                        state.isSaving -> strings.saving
                        state.isEditing -> strings.saveChanges
                        else -> strings.addWord
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

/**
 * The word type decides which grammatical fields both language cards ask for.
 *
 * Only the four open classes get a chip of their own; every closed class collapses into "Other",
 * which then names itself in the dropdown below — the Android arrangement, kept because eleven chips
 * do not fit a phone.
 */
@Composable
private fun WordTypeChips(selected: WordType, onSelect: (WordType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        WordCategory.entries.forEach { category ->
            val isSelected = category == selected.category
            val accent = MaterialTheme.colorScheme.primary

            Surface(
                // Re-picking the category the form is already on would throw away the sub-type.
                onClick = { if (!isSelected) onSelect(category.defaultType) },
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
                    text = category.label(strings),
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

/** Which kind of "Other": free text, or one of the closed parts of speech. */
@Composable
private fun OtherTypeDropdown(selected: WordType, onSelect: (WordType) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected.chipLabel(strings),
            onValueChange = {},
            readOnly = true,
            label = { Text(strings.typeOfWord) },
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
            WordType.otherTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.chipLabel(strings)) },
                    onClick = {
                        onSelect(type)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

/**
 * One language's alternatives, with its own add button.
 *
 * The two sides are independent: *kaufen* and *erwerben* can both mean *buy*, so adding an
 * alternative here adds a card to this language only, and the other keeps whatever it has.
 */
@Composable
private fun MeaningSection(
    side: WordSide,
    languageCode: String,
    inputs: List<WordFormInput>,
    wordType: WordType,
    onTextChanged: (WordSide, Int, String) -> Unit,
    onGenderChanged: (WordSide, Int, Gender?) -> Unit,
    onFieldChanged: (WordSide, Int, FieldKey, String) -> Unit,
    onAddMeaning: (WordSide) -> Unit,
    onRemoveMeaning: (WordSide, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        inputs.forEachIndexed { index, input ->
            if (index != 0) Spacer(modifier = Modifier.height(Spacing.small))

            // A single alternative needs no numbering — the card already names its language.
            if (inputs.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings.meaningNumber(index + 1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { onRemoveMeaning(side, index) },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Text(text = strings.remove, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            LanguageInputCard(
                languageCode = languageCode,
                wordType = wordType,
                input = input,
                onTextChanged = { text -> onTextChanged(side, index, text) },
                onGenderChanged = { gender -> onGenderChanged(side, index, gender) },
                onFieldChanged = { key, value -> onFieldChanged(side, index, key, value) },
            )
        }

        TextButton(
            onClick = { onAddMeaning(side) },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        ) {
            Text(text = strings.addAnotherMeaning)
        }
    }
}

private const val SelectedAlpha = 0.12f
