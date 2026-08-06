package de.coldtea.verborum.feature.bibliotheca.createword.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.composables.DropdownField
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordCategory
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordFormInput
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordGrammar
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.defaultType
import de.coldtea.verborum.feature.bibliotheca.createword.ui.composables.AnimatedWordCount
import de.coldtea.verborum.feature.bibliotheca.createword.ui.composables.LanguageInputCard
import de.coldtea.verborum.feature.bibliotheca.createword.ui.composables.WordFieldFocus
import de.coldtea.verborum.core.localization.strings
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun CreateWordContent(
    state: CreateWordUiState,
    wordCountChanged: Int,
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
    val dictionary = state.dictionary

    val subtitleContent = remember(wordCountChanged, dictionary?.name) {
        @Composable {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dictionary?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnimatedWordCount(
                    wordCount = state.wordCount,
                    changeTrigger = wordCountChanged,
                )
            }
        }
    }

    RegisterTopBar(
        title = if (state.isEditing) strings.editWordTitle else strings.newWord,
        subtitleContent = subtitleContent,
        showBackButton = true,
    )

    when {
        state.hasFailed -> ErrorState(
            message = strings.wordLoadFailed,
            modifier = modifier,
            onRetry = onRetry,
        )

        dictionary == null -> LoadingState(modifier)

        else -> ContentColumn(modifier = modifier) {
            val focusFor = rememberWordFieldFocus(state, dictionary.fromLang, dictionary.toLang)

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(Spacing.medium))

                WordTypeChips(selected = state.wordType, onSelect = onWordTypeChanged)

                // The closed classes share one chip and pick their part of speech here.
                if (state.wordType.category == WordCategory.OTHER) {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    OtherTypeDropdown(selected = state.wordType, onSelect = onWordTypeChanged)
                    Spacer(modifier = Modifier.height(Spacing.large))
                }

                MeaningSection(
                    side = WordSide.SOURCE,
                    languageCode = dictionary.fromLang,
                    focusFor = focusFor,
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
                    focusFor = focusFor,
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
 * The form's fields in the order the user meets them, as a lookup from field to its focus wiring.
 *
 * Built here rather than inside the cards because the return key has to cross from the last field of
 * the source card into the first of the target one, and neither card can see the other. The order
 * mirrors what the cards lay out — every meaning of the source side, then every meaning of the
 * target — and is derived from the same [WordGrammar] calls they use, so the two cannot drift.
 */
@Composable
private fun rememberWordFieldFocus(
    state: CreateWordUiState,
    fromLang: String,
    toLang: String,
): (WordSide, Int, FieldKey?) -> WordFieldFocus {
    // Requesters must outlive recomposition: a field that got a fresh one each pass could never be
    // focused, because the instance the modifier holds would already be stale.
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val focusManager = LocalFocusManager.current

    val slots = buildList {
        listOf(
            Triple(WordSide.SOURCE, fromLang, state.sourceInputs),
            Triple(WordSide.TARGET, toLang, state.targetInputs),
        ).forEach { (side, languageCode, inputs) ->
            val auxiliaryIsChips = WordGrammar.auxiliaryOptions(languageCode).isNotEmpty()

            inputs.forEachIndexed { index, input ->
                add(slotId(side, index, null) to input.text.isBlank())

                WordGrammar.fieldsFor(languageCode, state.wordType)
                    // The auxiliary is drawn as chips there, so it has no field to focus.
                    .filterNot { key -> key == FieldKey.AUXILIARY && auxiliaryIsChips }
                    .forEach { key -> add(slotId(side, index, key) to input.field(key).isBlank()) }
            }
        }
    }

    return { side, index, key ->
        val id = slotId(side, index, key)
        val next = slots
            .drop(slots.indexOfFirst { it.first == id } + 1)
            .firstOrNull { (_, isEmpty) -> isEmpty }

        WordFieldFocus(
            requester = requesters.getOrPut(id) { FocusRequester() },
            hasNext = next != null,
            onNext = {
                next?.let { requesters[it.first]?.requestFocus() } ?: focusManager.clearFocus()
            },
        )
    }
}

private fun slotId(side: WordSide, index: Int, key: FieldKey?): String =
    "$side/$index/${key?.name.orEmpty()}"

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
    DropdownField(
        label = strings.typeOfWord,
        value = selected.chipLabel(strings),
        options = WordType.otherTypes,
        optionLabel = { it.chipLabel(strings) },
        onSelect = onSelect,
    )
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
    focusFor: (WordSide, Int, FieldKey?) -> WordFieldFocus,
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
                focusFor = { key -> focusFor(side, index, key) },
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
