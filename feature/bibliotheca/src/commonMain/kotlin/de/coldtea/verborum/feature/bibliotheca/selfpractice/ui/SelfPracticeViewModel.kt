package de.coldtea.verborum.feature.bibliotheca.selfpractice.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.core.localization.stringsFor
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model.PracticeWordUi
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model.toPracticeUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** What the practice session is showing. */
internal sealed interface SelfPracticeState {
    data object Loading : SelfPracticeState
    data object Failed : SelfPracticeState
    data class Success(val dictionaryName: String, val words: List<PracticeWordUi>) :
        SelfPracticeState
}

internal data class SelfPracticeUiState(
    val practice: SelfPracticeState = SelfPracticeState.Loading,
    /** Asks the translation instead of the word — the "switch sides" action. */
    val isReversed: Boolean = false,
    /** Cards whose answer is showing: revealed on mobile, flipped on web. */
    val openWordIds: Set<String> = emptySet(),
)

internal class SelfPracticeViewModel(
    private val languageSettings: LanguageSettings,
    private val dictionaryId: String,
    private val dictionaryService: DictionaryService,
    private val wordService: WordService,
    private val syncService: SyncService,
) : BaseViewModel<SelfPracticeUiState, Nothing>(SelfPracticeUiState()) {

    /** Read fresh each time: the language can change while this screen is open. */
    private val strings: Strings get() = stringsFor(languageSettings.language.value)


    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Save failures; the screen puts them on the shared snackbar. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Named apart from the state's own flag: inside `setState` the state property shadows it. */
    private val reverseDirection = MutableStateFlow(false)

    /**
     * The order words are practised in, fixed once per session. Recomputing it on every emission
     * would reshuffle the deck under the user each time a level is saved.
     */
    private var practiceOrder: List<String> = emptyList()

    private var observeJob: Job? = null

    init {
        observeWords()
        viewModelScope.launch { syncService.syncDictionaryWords(dictionaryId) }
    }

    private fun observeWords() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                dictionaryService.observeDictionary(dictionaryId),
                wordService.observeWords(dictionaryId),
                reverseDirection,
            ) { dictionary, words, reversed ->
                if (dictionary == null) {
                    SelfPracticeState.Failed
                } else {
                    SelfPracticeState.Success(
                        dictionaryName = dictionary.name,
                        words = words.inPracticeOrder().map { word -> word.toPracticeUi(reversed, strings) },
                    )
                }
            }
                .catch { emit(SelfPracticeState.Failed) }
                .collect { practice -> setState { copy(practice = practice) } }
        }
    }

    /** Re-subscribes after a failure — the observed flow terminates when it errors. */
    fun retry() {
        setState { copy(practice = SelfPracticeState.Loading) }
        observeWords()
    }

    /** Swaps which side is asked. Open cards close, or the answer would already be showing. */
    fun switchSides() {
        reverseDirection.value = !reverseDirection.value
        setState { copy(isReversed = reverseDirection.value, openWordIds = emptySet()) }
    }

    fun toggleOpen(wordId: String) = setState {
        copy(
            openWordIds = if (wordId in openWordIds) openWordIds - wordId else openWordIds + wordId,
        )
    }

    /** "I knew it" — one rung up the ladder. */
    fun onCorrect(wordId: String) = changeLevel(wordId, by = 1)

    /** "I did not" — one rung down. */
    fun onWrong(wordId: String) = changeLevel(wordId, by = -1)

    fun onLevelChanged(wordId: String, level: Int) {
        viewModelScope.launch { save(wordId, level) }
    }

    private fun changeLevel(wordId: String, by: Int) {
        val current = currentWords().firstOrNull { it.wordId == wordId } ?: return

        viewModelScope.launch { save(wordId, current.level + by) }
    }

    private suspend fun save(wordId: String, level: Int) {
        // A failed save keeps the session going: losing a rung is not worth interrupting practice
        // for, and the snackbar says so.
        if (wordService.updateLevel(wordId, level) is Outcome.Failure) {
            _messages.emit(strings.answerSaveFailed)
        }
    }

    private fun currentWords(): List<PracticeWordUi> =
        (currentState.practice as? SelfPracticeState.Success)?.words.orEmpty()

    private fun List<Word>.inPracticeOrder(): List<Word> {
        if (practiceOrder.isEmpty()) practiceOrder = map(Word::wordId).shuffled()

        val byId = associateBy(Word::wordId)

        // Words added or removed mid-session (a sync landing) keep the established order and are
        // appended rather than reshuffling everything the user is working through.
        return practiceOrder.mapNotNull(byId::get) + filterNot { it.wordId in practiceOrder }
    }
}
