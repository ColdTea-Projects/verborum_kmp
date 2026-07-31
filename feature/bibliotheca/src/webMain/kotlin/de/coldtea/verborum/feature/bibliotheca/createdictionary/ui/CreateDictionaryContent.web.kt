package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.WebChip
import de.coldtea.verborum.core.designsystem.component.WebFieldLabel
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.component.WebSelect
import de.coldtea.verborum.core.designsystem.component.WebTextField
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.model.ALL_TAGS

/**
 * The dictionary form as a desktop page: a name, the language pair side by side, and the tag
 * catalogue as chips.
 *
 * Editing locks the two language selects. A dictionary's words are written *in* its language pair,
 * so changing the pair afterwards would leave every one of them mislabelled — the same rule the
 * Android app applies.
 */
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
    // Registers the chrome as well as the way back: an unregistered screen reads to the shell as one
    // that wants no chrome at all, and would lose the sidebar with it.
    RegisterTopBar(
        title = if (state.isEditing) "Edit dictionary" else "New dictionary",
        showBackButton = true,
        backLabel = "Back to dictionaries",
    )

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ContentPane(maxWidth = ContentWidth.Web.dictionaryForm) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                WebPageSpacer(Spacing.extraLarge)

                WebPageTitle(
                    title = if (state.isEditing) "Edit Dictionary" else "Create Dictionary",
                    subtitle = "Name it, choose a language pair, and tag it for easy filtering.",
                )

                WebPageSpacer()

                WebFieldLabel("Dictionary name")
                WebTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    placeholder = "e.g. German Basics",
                )

                WebPageSpacer()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    LanguageField(
                        label = "From language",
                        selected = state.fromLanguage,
                        isEnabled = !state.isEditing,
                        onSelect = onFromLanguageChanged,
                        modifier = Modifier.weight(1f),
                    )
                    LanguageField(
                        label = "To language",
                        selected = state.toLanguage,
                        isEnabled = !state.isEditing,
                        onSelect = onToLanguageChanged,
                        modifier = Modifier.weight(1f),
                    )
                }

                WebPageSpacer()

                WebFieldLabel("Tags")
                TagChips(selectedCodes = state.tags, onToggle = onTagToggled)

                WebPageSpacer()

                WebPrimaryButton(
                    label = when {
                        state.isSaving -> "Saving…"
                        state.isEditing -> "Save Changes"
                        else -> "Create Dictionary"
                    },
                    onClick = onSave,
                    // A dictionary without a name or a direction is not something the app can store.
                    isEnabled = state.canSave,
                )

                WebPageSpacer(Spacing.extraLarge)
            }
        }
    }
}

@Composable
private fun LanguageField(
    label: String,
    selected: SupportedLanguage?,
    isEnabled: Boolean,
    onSelect: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        WebFieldLabel(label)

        WebSelect(label = selected?.displayName ?: "Select…", isEnabled = isEnabled) { dismiss ->
            SupportedLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onSelect(language)
                        dismiss()
                    },
                )
            }
        }
    }
}

/** The whole tag catalogue, wrapping across the form's width. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(selectedCodes: List<String>, onToggle: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        ALL_TAGS.forEach { tag ->
            WebChip(
                label = tag.label,
                isSelected = tag.code in selectedCodes,
                onClick = { onToggle(tag.code) },
            )
        }
    }
}
