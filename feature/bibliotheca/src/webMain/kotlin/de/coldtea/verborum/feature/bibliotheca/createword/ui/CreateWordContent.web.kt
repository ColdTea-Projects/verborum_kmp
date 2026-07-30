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
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.LocalNavigateBack
import de.coldtea.verborum.core.designsystem.component.WebBackLink
import de.coldtea.verborum.core.designsystem.component.WebChip
import de.coldtea.verborum.core.designsystem.component.WebEyebrow
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.component.WebTextAction
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
import de.coldtea.verborum.feature.bibliotheca.createword.ui.composables.WebLanguageCard

/**
 * The word form as a desktop page: the word type across the top, then the two languages side by
 * side, one card each.
 *
 * Which grammatical fields each card asks for is still `WordGrammar`'s decision, so the pairing is
 * the only thing the wider layout changes — a German verb asks for the same things here as it does
 * on the phone.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val navigateBack = LocalNavigateBack.current
    val dictionary = state.dictionary

    when {
        state.hasFailed -> ErrorState(
            message = "This word could not be loaded.",
            modifier = modifier,
            onRetry = onRetry,
        )

        dictionary == null -> LoadingState(modifier)

        else -> Box(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        ) {
            ContentPane(maxWidth = ContentWidth.Web.wordForm) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    WebPageSpacer(Spacing.extraLarge)

                    WebBackLink(label = "Back to ${dictionary.name}", onClick = navigateBack)

                    WebPageSpacer(Spacing.small)

                    WebPageTitle(title = if (state.isEditing) "Edit Word" else "Add Word")

                    WebPageSpacer()

                    WebEyebrow("Word type")

                    WebPageSpacer(Spacing.small)

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        WordType.entries.forEach { type ->
                            WebChip(
                                label = type.label,
                                isSelected = type == state.wordType,
                                onClick = { onWordTypeChanged(type) },
                            )
                        }
                    }

                    repeat(state.meaningCount) { index ->
                        WebPageSpacer()

                        MeaningHeader(
                            index = index,
                            canRemove = state.meaningCount > 1,
                            onRemove = { onRemoveMeaning(index) },
                        )

                        WebPageSpacer(Spacing.small)

                        Row(
                            // Both cards take the height of the taller one, so the accent bars run
                            // the full side of the pair rather than stopping short.
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        ) {
                            state.sourceInputs.getOrNull(index)?.let { input ->
                                WebLanguageCard(
                                    languageCode = dictionary.fromLang,
                                    wordType = state.wordType,
                                    input = input,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onTextChanged = { onTextChanged(WordSide.SOURCE, index, it) },
                                    onGenderChanged = { onGenderChanged(WordSide.SOURCE, index, it) },
                                    onFieldChanged = { key, value ->
                                        onFieldChanged(WordSide.SOURCE, index, key, value)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            state.targetInputs.getOrNull(index)?.let { input ->
                                WebLanguageCard(
                                    languageCode = dictionary.toLang,
                                    wordType = state.wordType,
                                    input = input,
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    onTextChanged = { onTextChanged(WordSide.TARGET, index, it) },
                                    onGenderChanged = { onGenderChanged(WordSide.TARGET, index, it) },
                                    onFieldChanged = { key, value ->
                                        onFieldChanged(WordSide.TARGET, index, key, value)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    WebPageSpacer(Spacing.medium)

                    WebTextAction(label = "+ Add another meaning", onClick = onAddMeaning)

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

/** Meanings are numbered so the two language cards below them read as a pair. */
@Composable
private fun MeaningHeader(index: Int, canRemove: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WebEyebrow("Meaning ${index + 1}")

        if (canRemove) {
            WebTextAction(
                label = "Remove",
                onClick = onRemove,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
