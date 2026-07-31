package de.coldtea.verborum.feature.bibliotheca.createword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.WebChip
import de.coldtea.verborum.core.designsystem.component.WebEyebrow
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.component.WebTextAction
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardController
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.LocalKeyboardController
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordFormInput
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
import de.coldtea.verborum.feature.bibliotheca.createword.ui.composables.WebLanguageCard

/** Room for more alternatives, and more fields per card, than any language actually asks for. */
private const val CARD_STRIDE = 100
private const val SIDE_STRIDE = 100_000

/**
 * The word form as a desktop page: the word type across the top, then one column per language, each
 * holding that language's alternatives and its own add button.
 *
 * Which grammatical fields each card asks for is still `WordGrammar`'s decision — a German verb asks
 * for the same things here as it does on the phone.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val dictionary = state.dictionary
    // One controller for the whole form, not one per card: Enter has to be able to walk out of the
    // last source field and into the first target one.
    val keyboardController = remember { KeyboardController() }

    // Registers the chrome as well as the way back: an unregistered screen reads to the shell as one
    // that wants no chrome at all, and would lose the sidebar with it.
    RegisterTopBar(
        title = if (state.isEditing) "Edit word" else "New word",
        subtitle = dictionary?.name,
        showBackButton = true,
        backLabel = dictionary?.let { "Back to ${it.name}" } ?: "Back",
    )

    when {
        state.hasFailed -> ErrorState(
            message = "This word could not be loaded.",
            modifier = modifier,
            onRetry = onRetry,
        )

        dictionary == null -> LoadingState(modifier)

        else -> CompositionLocalProvider(LocalKeyboardController provides keyboardController) {
            Box(
                modifier = modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                ContentPane(maxWidth = ContentWidth.Web.wordForm) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        WebPageSpacer(Spacing.extraLarge)

                        WebPageTitle(title = if (state.isEditing) "Edit Word" else "Add Word")

                        WebPageSpacer()

                        WebEyebrow("Word type")

                        WebPageSpacer(Spacing.small)

                        // Every type gets its own chip here, the closed classes included: a desktop row
                        // wraps to a second line where a phone would need the "Other" dropdown instead.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            WordType.entries.forEach { type ->
                                WebChip(
                                    label = type.chipLabel,
                                    isSelected = type == state.wordType,
                                    onClick = { onWordTypeChanged(type) },
                                )
                            }
                        }

                        WebPageSpacer()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        ) {
                            MeaningColumn(
                                side = WordSide.SOURCE,
                                languageCode = dictionary.fromLang,
                                accentColor = MaterialTheme.colorScheme.primary,
                                inputs = state.sourceInputs,
                                wordType = state.wordType,
                                onTextChanged = onTextChanged,
                                onGenderChanged = onGenderChanged,
                                onFieldChanged = onFieldChanged,
                                onAddMeaning = onAddMeaning,
                                onRemoveMeaning = onRemoveMeaning,
                                modifier = Modifier.weight(1f),
                            )
                            MeaningColumn(
                                side = WordSide.TARGET,
                                languageCode = dictionary.toLang,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                inputs = state.targetInputs,
                                wordType = state.wordType,
                                onTextChanged = onTextChanged,
                                onGenderChanged = onGenderChanged,
                                onFieldChanged = onFieldChanged,
                                onAddMeaning = onAddMeaning,
                                onRemoveMeaning = onRemoveMeaning,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        WebPageSpacer()

                        WebPrimaryButton(
                            label = when {
                                state.isSaving -> "Saving…"
                                state.isEditing -> "Save Changes"
                                else -> "Save Word"
                            },
                            onClick = onSave,
                            isEnabled = state.canSave,
                        )

                        WebPageSpacer(Spacing.extraLarge)
                    }
            }
            }
        }
    }
}

/**
 * One language's alternatives, with its own add button.
 *
 * The two sides are independent: *kaufen* and *erwerben* can both mean *buy*, so adding an
 * alternative here adds a card to this column only, and the other language keeps whatever it has.
 */
@Composable
private fun MeaningColumn(
    side: WordSide,
    languageCode: String,
    accentColor: Color,
    inputs: List<WordFormInput>,
    wordType: WordType,
    onTextChanged: (WordSide, Int, String) -> Unit,
    onGenderChanged: (WordSide, Int, Gender?) -> Unit,
    onFieldChanged: (WordSide, Int, FieldKey, String) -> Unit,
    onAddMeaning: (WordSide) -> Unit,
    onRemoveMeaning: (WordSide, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        inputs.forEachIndexed { index, input ->
            if (index != 0) WebPageSpacer(Spacing.medium)

            // A single alternative needs no numbering — the card already names its language.
            if (inputs.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    WebEyebrow("Meaning ${index + 1}")
                    WebTextAction(
                        label = "Remove",
                        onClick = { onRemoveMeaning(side, index) },
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                WebPageSpacer(Spacing.small)
            }

            WebLanguageCard(
                languageCode = languageCode,
                wordType = wordType,
                input = input,
                accentColor = accentColor,
                cardId = "${side.name}-$index",
                // Enter walks the source column top to bottom, then the target column: a card's
                // fields are contiguous, and one card's block never overlaps the next.
                fieldOrder = side.ordinal * SIDE_STRIDE + index * CARD_STRIDE,
                onTextChanged = { onTextChanged(side, index, it) },
                onGenderChanged = { onGenderChanged(side, index, it) },
                onFieldChanged = { key, value -> onFieldChanged(side, index, key, value) },
            )
        }

        WebPageSpacer(Spacing.small)

        WebTextAction(label = "+ Add another meaning", onClick = { onAddMeaning(side) })
    }
}
