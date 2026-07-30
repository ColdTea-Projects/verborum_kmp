package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Creating a dictionary, or editing one when opened with an id. One design for both platforms, in
 * the app's usual capped column.
 */
@Composable
internal fun CreateDictionaryScreen(
    dictionaryId: String?,
    onSaved: (DictionarySaved) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateDictionaryViewModel = koinViewModel { parametersOf(dictionaryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ShowSnackbarMessages(viewModel.messages)

    // Leaves only on a successful save, never on a failed one.
    LaunchedEffect(viewModel) {
        viewModel.saved.collect(onSaved)
    }

    CreateDictionaryContent(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onFromLanguageChanged = viewModel::onFromLanguageChanged,
        onToLanguageChanged = viewModel::onToLanguageChanged,
        onTagToggled = viewModel::onTagToggled,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

/**
 * The per-platform half of the screen.
 *
 * iOS: a phone-width column under the shared top bar — the Android design. Web: a narrow desktop
 * form that titles itself, with the language pair locked once the dictionary exists.
 */
@Composable
internal expect fun CreateDictionaryContent(
    state: CreateDictionaryUiState,
    onNameChanged: (String) -> Unit,
    onFromLanguageChanged: (SupportedLanguage) -> Unit,
    onToLanguageChanged: (SupportedLanguage) -> Unit,
    onTagToggled: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier,
)
