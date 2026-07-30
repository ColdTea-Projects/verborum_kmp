package de.coldtea.verborum.feature.bibliotheca.createword.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Creating a word, or editing one when opened with a word id. One design for both platforms, in the
 * app's usual capped column.
 *
 * The form follows the dictionary's languages: which grammatical fields appear is decided per
 * language and word type, so the same screen asks a German verb and a Japanese noun different things.
 */
@Composable
internal fun CreateWordScreen(
    dictionaryId: String,
    wordId: String?,
    onSaved: (wasEditing: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateWordViewModel = koinViewModel { parametersOf(dictionaryId, wordId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ShowSnackbarMessages(viewModel.messages)

    // Leaves only on a successful save.
    LaunchedEffect(viewModel) {
        viewModel.saved.collect(onSaved)
    }

    CreateWordContent(
        state = state,
        onWordTypeChanged = viewModel::onWordTypeChanged,
        onTextChanged = viewModel::onTextChanged,
        onGenderChanged = viewModel::onGenderChanged,
        onFieldChanged = viewModel::onFieldChanged,
        onAddMeaning = viewModel::addMeaning,
        onRemoveMeaning = viewModel::removeMeaning,
        onSave = viewModel::save,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * The per-platform half of the screen.
 *
 * iOS: the two language cards stacked in a phone-width column — the Android design. Web: a wider
 * page with the pair side by side, one column per language.
 */
@Composable
internal expect fun CreateWordContent(
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
)
