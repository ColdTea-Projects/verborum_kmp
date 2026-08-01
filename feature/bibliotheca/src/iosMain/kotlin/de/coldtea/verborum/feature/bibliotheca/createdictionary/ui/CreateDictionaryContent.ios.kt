package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.composables.LanguageDropdown
import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.composables.TagSelector
import de.coldtea.verborum.core.localization.strings

@Composable
internal actual fun CreateDictionaryContent(
    state: CreateDictionaryUiState,
    onNameChanged: (String) -> Unit,
    onFromLanguageChanged: (SupportedLanguage) -> Unit,
    onToLanguageChanged: (SupportedLanguage) -> Unit,
    onTagToggled: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier,
) {
    RegisterTopBar(
        title = if (state.isEditing) strings.editDictionaryTitle else strings.newDictionary,
        showBackButton = true,
    )

    ContentColumn(modifier = modifier) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(Spacing.medium))

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = { Text(strings.dictionaryName) },
                singleLine = true,
                shape = Shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            LanguageDropdown(
                label = strings.fromLanguage,
                selected = state.fromLanguage,
                onSelect = onFromLanguageChanged,
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            LanguageDropdown(
                label = strings.toLanguage,
                selected = state.toLanguage,
                onSelect = onToLanguageChanged,
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            TagSelector(selectedCodes = state.tags, onToggle = onTagToggled)

            Spacer(modifier = Modifier.height(Spacing.medium))
        }

        Button(
            onClick = onSave,
            // A dictionary without a name or a direction is not something the app can store.
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
                    else -> "Create dictionary"
                },
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
