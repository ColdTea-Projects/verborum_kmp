package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.REQUIRED_WORDS_FOR_TEST
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** One dictionary: how to practise it, the words it holds, and how to get rid of it. */
@Composable
internal fun DictionaryDetailsScreen(
    dictionaryId: String,
    onTestClick: () -> Unit,
    onSelfPracticeClick: () -> Unit,
    onCreateWordClick: () -> Unit,
    onEditWordClick: (String) -> Unit,
    onDictionaryDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryDetailsViewModel = koinViewModel { parametersOf(dictionaryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ShowSnackbarMessages(viewModel.messages)

    // Leaving is driven by the observed state, not by the delete button: the screen goes exactly
    // once, so an async delete cannot race the back navigation, and a torn-down screen can never
    // re-register the shared header with a dictionary that no longer exists.
    LaunchedEffect(state.details) {
        if (state.details is DictionaryDetailsState.Deleted) onDictionaryDeleted()
    }

    DictionaryDetailsContent(
        state = state,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onTestClick = onTestClick,
        onSelfPracticeClick = onSelfPracticeClick,
        onCreateWordClick = onCreateWordClick,
        onEditWordClick = onEditWordClick,
        onDeleteWord = viewModel::deleteWord,
        onDeleteDictionary = viewModel::deleteDictionary,
        onUnavailableMode = viewModel::explainUnavailableMode,
        modifier = modifier,
    )
}

/**
 * The per-platform half of the screen.
 *
 * iOS: practice tiles over a scrolling word list, with the dictionary named in the shared top bar —
 * the Android design. Web: a desktop page that names itself, with the word list in a bordered panel.
 */
@Composable
internal expect fun DictionaryDetailsContent(
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
)

/** Kept next to the screen it explains, so the threshold and the message cannot drift apart. */
internal fun unavailableModeMessage(isTest: Boolean, wordCount: Int): String = when {
    wordCount == 0 -> "Add a word first — there is nothing to practise yet."
    isTest -> "A test needs at least $REQUIRED_WORDS_FOR_TEST different words to choose between."
    else -> "Add a word first — there is nothing to practise yet."
}
