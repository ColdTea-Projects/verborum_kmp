package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.RefreshWhileVisible
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.REQUIRED_WORDS_FOR_TEST
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * One dictionary: how to practise it, the words it holds, and how to get rid of it.
 *
 * Serves both kinds. A download is read-only throughout — no add-word button, no word editing, no
 * sharing switch — and offers Unsubscribe where an own dictionary offers Delete.
 */
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

    // Web has no push: an edit made elsewhere shows up when the tab is next in front, or within a
    // minute while it stays visible. Quiet — see refreshInBackground.
    RefreshWhileVisible(onRefresh = viewModel::refreshInBackground)

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
        onSharedChange = viewModel::setShared,
        onConfirmUnshare = viewModel::confirmUnshare,
        onDismissUnshare = viewModel::dismissUnshareConfirmation,
        onUnsubscribeClick = viewModel::askToUnsubscribe,
        onConfirmUnsubscribe = viewModel::confirmUnsubscribe,
        onDismissUnsubscribe = viewModel::dismissUnsubscribeConfirmation,
        onRate = viewModel::rate,
        onRemoveRating = viewModel::removeRating,
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
    /** Moves the sharing switch. Only reachable while `isSharingVisible`. */
    onSharedChange: (Boolean) -> Unit,
    onConfirmUnshare: () -> Unit,
    onDismissUnshare: () -> Unit,
    /** Opens the unsubscribe confirmation; only a download offers it. */
    onUnsubscribeClick: () -> Unit,
    onConfirmUnsubscribe: () -> Unit,
    onDismissUnsubscribe: () -> Unit,
    /** Rating a download — the same card the Forum's details screen shows. */
    onRate: (Int) -> Unit,
    onRemoveRating: () -> Unit,
    modifier: Modifier,
)

/** Kept next to the screen it explains, so the threshold and the message cannot drift apart. */
internal fun unavailableModeMessage(isTest: Boolean, wordCount: Int, strings: Strings): String = when {
    // An empty dictionary is the more basic problem, and it wins over the pair being too small:
    // there is nothing to be asked about, however many distractors are available.
    wordCount == 0 -> "Add a word first — there is nothing to practise yet."
    isTest -> strings.notEnoughWords(REQUIRED_WORDS_FOR_TEST)
    else -> "Add a word first — there is nothing to practise yet."
}
