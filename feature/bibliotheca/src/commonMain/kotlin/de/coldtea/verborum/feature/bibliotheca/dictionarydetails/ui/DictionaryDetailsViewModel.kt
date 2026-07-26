package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.pluralize
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.REQUIRED_WORDS_FOR_TEST
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.toUi
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.languagePairLabel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal data class DictionaryDetailsUiState(
    val details: DictionaryDetailsState = DictionaryDetailsState.Loading,
    val isRefreshing: Boolean = false,
)

internal class DictionaryDetailsViewModel(
    private val dictionaryId: String,
    private val dictionaryService: DictionaryService,
    private val wordService: WordService,
    private val syncService: SyncService,
) : BaseViewModel<DictionaryDetailsUiState, Nothing>(DictionaryDetailsUiState()) {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Delete failures and refresh notices; the screen puts them on the shared snackbar. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var observeJob: Job? = null

    init {
        observeDetails()
        refresh()
    }

    private fun observeDetails() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                dictionaryService.observeDictionary(dictionaryId),
                wordService.observeWords(dictionaryId),
            ) { dictionary, words ->
                // A missing dictionary means it was deleted — here or elsewhere. Reported as its own
                // state so the screen leaves once, instead of re-rendering a header for a row that
                // no longer exists.
                if (dictionary == null) {
                    DictionaryDetailsState.Deleted
                } else {
                    DictionaryDetailsState.Success(
                        name = dictionary.name,
                        languagePair = languagePairLabel(dictionary.fromLang, dictionary.toLang),
                        words = words.map(Word::toUi),
                        tags = dictionary.tags,
                        canSelfPractice = words.isNotEmpty(),
                        canTest = words.distinctWordCount() >= REQUIRED_WORDS_FOR_TEST,
                    )
                }
            }
                .catch { emit(DictionaryDetailsState.Failed) }
                .collect { details -> setState { copy(details = details) } }
        }
    }

    /** Re-subscribes after a failure — the observed flow terminates when it errors. */
    fun retry() {
        setState { copy(details = DictionaryDetailsState.Loading) }
        observeDetails()
        refresh()
    }

    fun refresh() {
        setState { copy(isRefreshing = true) }

        viewModelScope.launch {
            val outcome = syncService.syncDictionaryWords(dictionaryId)

            setState { copy(isRefreshing = false) }

            if (outcome is Outcome.Failure && currentState.details is DictionaryDetailsState.Loading) {
                setState { copy(details = DictionaryDetailsState.Failed) }
            }
        }
    }

    /**
     * Says why a practice mode cannot start. A disabled tile that explains itself beats one that
     * silently does nothing, and the reason depends on state only this side knows.
     */
    fun explainUnavailableMode(isTest: Boolean) {
        val wordCount = (currentState.details as? DictionaryDetailsState.Success)?.words?.size ?: 0

        viewModelScope.launch { _messages.emit(unavailableModeMessage(isTest, wordCount)) }
    }

    fun deleteWord(wordId: String) {
        viewModelScope.launch {
            if (wordService.deleteWord(wordId) is Outcome.Failure) {
                _messages.emit("That word could not be deleted. It is back in the list.")
            }
        }
    }

    /**
     * Deletes the dictionary and its words. The words go first: a dictionary whose delete succeeds
     * while its words linger would leave rows belonging to nothing. Success needs no navigation call —
     * the observed dictionary becomes null, which moves the screen to `Deleted`.
     */
    fun deleteDictionary() {
        viewModelScope.launch {
            wordService.cleanWordsInDictionary(dictionaryId)

            if (dictionaryService.deleteDictionary(dictionaryId) is Outcome.Failure) {
                _messages.emit("That dictionary could not be deleted. It is back in your list.")
            }
        }
    }

    /**
     * Distinct word/translation pairs. Duplicates cannot serve as each other's wrong answers, so the
     * test threshold counts what is actually distinguishable rather than how many rows exist.
     */
    private fun List<Word>.distinctWordCount(): Int =
        distinctBy { word -> word.word + word.translation }.size
}

/** "12 words" for the header subtitle. */
internal fun wordCountLabel(count: Int): String = pluralize(count, "word")
