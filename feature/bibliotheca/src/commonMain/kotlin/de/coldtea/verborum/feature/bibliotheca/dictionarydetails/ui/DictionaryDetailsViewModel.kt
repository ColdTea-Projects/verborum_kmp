package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.core.localization.stringsFor
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.pluralize
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.ObserveSharingStateUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.SharingState
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteOutcome
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.SetDictionarySharedUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.SharingChange
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.REQUIRED_WORDS_FOR_TEST
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.toUi
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.languagePairLabel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class DictionaryDetailsUiState(
    val details: DictionaryDetailsState = DictionaryDetailsState.Loading,
    val isRefreshing: Boolean = false,
)

internal class DictionaryDetailsViewModel(
    private val languageSettings: LanguageSettings,
    private val dictionaryId: String,
    private val dictionaryService: DictionaryService,
    private val wordService: WordService,
    private val syncService: SyncService,
    private val importedDictionaryService: ImportedDictionaryService,
    private val setDictionaryShared: SetDictionarySharedUseCase,
    observeSharingState: ObserveSharingStateUseCase,
) : BaseViewModel<DictionaryDetailsUiState, Nothing>(DictionaryDetailsUiState()) {

    private val sharingState = observeSharingState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SharingState())

    /** Read fresh each time: the language can change while this screen is open. */
    private val strings: Strings get() = stringsFor(languageSettings.language.value)


    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Delete failures and refresh notices; the screen puts them on the shared snackbar. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var observeJob: Job? = null

    /** Until something has been downloaded, "not in the store" says nothing about what exists. */
    private var hasPulled = false

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
                sharingState,
            ) { row, words, sharing ->
                val dictionary = row?.dictionary
                // A missing dictionary means it was deleted — here or elsewhere. Reported as its own
                // state so the screen leaves once, instead of re-rendering a header for a row that
                // no longer exists.
                if (dictionary == null) {
                    // Before the first pull this is a store nothing has filled yet — a link someone
                    // shared opens this screen with one — and not a row that went away.
                    if (hasPulled) DictionaryDetailsState.Deleted else DictionaryDetailsState.Loading
                } else {
                    DictionaryDetailsState.Success(
                        name = dictionary.name,
                        languagePair = languagePairLabel(dictionary.fromLang, dictionary.toLang, strings),
                        words = words.map(Word::toUi),
                        tags = dictionary.tags,
                        canSelfPractice = words.isNotEmpty(),
                        canTest = words.distinctWordCount() >= REQUIRED_WORDS_FOR_TEST,
                        isImported = row.isImported,
                        publisherName = row.publisherName,
                        // The switch is for members only, and never for a download: someone else's
                        // dictionary is not the user's to share.
                        isSharingVisible = sharing.isMember && !row.isImported,
                        isShared = dictionary.isPublic,
                        isLastShared = sharing.isLastShared(dictionaryId),
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
        hasPulled = false
        observeDetails()
        refresh()
    }

    fun refresh() {
        setState { copy(isRefreshing = true) }

        viewModelScope.launch {
            // Never seen here: the row itself was never downloaded, which is what a link opened
            // from somewhere else arrives to. Pull the library before concluding anything. Which
            // kind of dictionary it is, once it *is* known, is the sync's question rather than this
            // screen's — a download is refreshed through the path that owns it.
            val outcome = if (dictionaryService.libraryDictionary(dictionaryId) == null) {
                syncService.syncDictionaries()
            } else {
                syncService.syncDictionary(dictionaryId)
            }

            hasPulled = true
            setState { copy(isRefreshing = false) }

            when {
                outcome is Outcome.Failure && currentState.details is DictionaryDetailsState.Loading ->
                    setState { copy(details = DictionaryDetailsState.Failed) }

                // Pulled, and still not there: now it really is gone. The observed flow will not say
                // so on its own, having never had a row to lose.
                //
                // Asked across the **whole** library, own and downloaded. Asking the own-only read
                // here is what made every downloaded dictionary report itself deleted and bounce
                // straight back to the list the moment it was opened.
                outcome is Outcome.Success && dictionaryService.libraryDictionary(dictionaryId) == null ->
                    setState { copy(details = DictionaryDetailsState.Deleted) }
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
                _messages.emit(strings.wordDeleteFailed)
            }
        }
    }

    /**
     * Deletes the dictionary. Success needs no navigation call — the observed dictionary becomes
     * null, which moves the screen to `Deleted`.
     *
     * Only the dictionary is deleted remotely; the server removes its words in the same transaction,
     * after checking the sharing rule. Sending a word delete first and then being refused would
     * destroy the words of a dictionary that still exists.
     */
    fun deleteDictionary() {
        viewModelScope.launch {
            when (dictionaryService.deleteDictionary(dictionaryId)) {
                DeleteOutcome.DELETED, DeleteOutcome.PENDING -> Unit

                DeleteOutcome.SHARING_REQUIRED -> _messages.emit(strings.errorDeleteLastShared)

                DeleteOutcome.REFUSED -> _messages.emit(strings.dictionaryDeleteFailed)
            }
        }
    }

    /**
     * Moves the sharing switch.
     *
     * Turning it **off** asks first: it takes the dictionary out of the Forum and everyone who
     * downloaded it loses access — which is not visible from the switch itself. Turning it on needs
     * no confirmation; nothing is lost by sharing.
     */
    fun setShared(isShared: Boolean) {
        val details = success() ?: return
        if (!details.canToggleSharing) return

        if (isShared) {
            share(isShared = true)
        } else {
            updateSuccess { copy(isUnshareConfirmationShown = true) }
        }
    }

    fun confirmUnshare() {
        updateSuccess { copy(isUnshareConfirmationShown = false) }
        share(isShared = false)
    }

    fun dismissUnshareConfirmation() = updateSuccess { copy(isUnshareConfirmationShown = false) }

    fun askToUnsubscribe() = updateSuccess { copy(isUnsubscribeConfirmationShown = true) }

    fun dismissUnsubscribeConfirmation() =
        updateSuccess { copy(isUnsubscribeConfirmationShown = false) }

    /**
     * Removes a download. The server drops the vault entry first; only then does the local copy go,
     * and the screen leaves because the observed dictionary becomes null.
     */
    fun confirmUnsubscribe() {
        updateSuccess { copy(isUnsubscribeConfirmationShown = false) }

        viewModelScope.launch {
            if (importedDictionaryService.unsubscribe(dictionaryId) is Outcome.Failure) {
                _messages.emit(strings.errorUnsubscribeFailed)
            }
        }
    }

    private fun share(isShared: Boolean) {
        updateSuccess { copy(isUpdatingSharing = true) }

        viewModelScope.launch {
            when (setDictionaryShared(dictionaryId, isShared)) {
                // The row's own `isPublic` is what the switch draws from, and the observed flow
                // delivers it — so there is nothing to set here.
                SharingChange.UPDATED -> Unit

                SharingChange.LAST_SHARED -> _messages.emit(strings.errorDeleteLastShared)

                SharingChange.FAILED -> _messages.emit(strings.errorSharingFailed)
            }

            updateSuccess { copy(isUpdatingSharing = false) }
        }
    }

    private fun success(): DictionaryDetailsState.Success? =
        currentState.details as? DictionaryDetailsState.Success

    private fun updateSuccess(transform: DictionaryDetailsState.Success.() -> DictionaryDetailsState.Success) =
        setState {
            val details = this.details
            if (details is DictionaryDetailsState.Success) copy(details = details.transform()) else this
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
