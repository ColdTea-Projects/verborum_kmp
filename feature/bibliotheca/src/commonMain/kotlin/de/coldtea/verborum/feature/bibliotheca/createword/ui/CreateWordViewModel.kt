package de.coldtea.verborum.feature.bibliotheca.createword.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.core.localization.stringsFor
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.common.TimeProvider
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordFormInput
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.composeWordMeta
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.composeWordText
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.parseWordFormInputs
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.parseWordMeta
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Which language side an edit applies to. */
internal enum class WordSide { SOURCE, TARGET }

internal data class CreateWordUiState(
    val dictionary: Dictionary? = null,
    val wordType: WordType = WordType.NOUN,
    val wordCount: Int = 0,
    /**
     * One entry per alternative on each side, and the two sides are independent: *kaufen/erwerben*
     * can both mean *buy*, so a word may carry two alternatives in one language and one in the
     * other. Each side is stored on its own — surfaces and meta are index-aligned *within* a side,
     * never across the pair — so nothing requires the lists to be the same length.
     */
    val sourceInputs: List<WordFormInput> = listOf(WordFormInput()),
    val targetInputs: List<WordFormInput> = listOf(WordFormInput()),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val hasFailed: Boolean = false,
) {
    /** Both sides need at least their first meaning filled in; everything else is optional. */
    val canSave: Boolean
        get() = dictionary != null &&
            sourceInputs.firstOrNull()?.text?.isNotBlank() == true &&
            targetInputs.firstOrNull()?.text?.isNotBlank() == true &&
            !isSaving

    fun inputsFor(side: WordSide): List<WordFormInput> = when (side) {
        WordSide.SOURCE -> sourceInputs
        WordSide.TARGET -> targetInputs
    }
}

internal class CreateWordViewModel(
    private val languageSettings: LanguageSettings,
    private val dictionaryId: String,
    private val wordId: String?,
    private val dictionaryService: DictionaryService,
    private val wordService: WordService,
    private val time: TimeProvider = SystemTimeProvider,
) : BaseViewModel<CreateWordUiState, Nothing>(CreateWordUiState()) {

    /** Read fresh each time: the language can change while this screen is open. */
    private val strings: Strings get() = stringsFor(languageSettings.language.value)


    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _saved = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    /** True when the save was an edit; emitted only on success, so a failure never navigates. */
    val saved: SharedFlow<Boolean> = _saved.asSharedFlow()

    private val _wordCountChanged = MutableStateFlow(0)
    val wordCountChanged: StateFlow<Int> = _wordCountChanged.asStateFlow()

    private var editing: Word? = null
    private var hasLoadedInitialCount = false

    init {
        load()
        observeWordCount()
    }

    private fun load() {
        viewModelScope.launch {
            val dictionary = dictionaryService.dictionary(dictionaryId)

            if (dictionary == null) {
                setState { copy(hasFailed = true) }
                return@launch
            }

            val word = wordId?.let { wordService.word(it) }
            editing = word

            setState {
                copy(
                    dictionary = dictionary,
                    isEditing = word != null,
                    hasFailed = false,
                    wordType = when (word) {
                        null -> WordType.NOUN
                        else -> parseWordMeta(word.wordMeta)?.wordType ?: WordType.FREE_TEXT
                    },
                    sourceInputs = word
                        ?.let { parseWordFormInputs(dictionary.fromLang, it.word, it.wordMeta) }
                        ?: listOf(WordFormInput()),
                    targetInputs = word
                        ?.let {
                            parseWordFormInputs(dictionary.toLang, it.translation, it.translationMeta)
                        }
                        ?: listOf(WordFormInput()),
                )
            }
        }
    }

    private fun observeWordCount() {
        viewModelScope.launch {
            wordService.observeWords(dictionaryId).collect { words ->
                val newCount = words.size
                val previousCount = currentState.wordCount
                setState { copy(wordCount = newCount) }
                if (hasLoadedInitialCount && newCount != previousCount) {
                    _wordCountChanged.value = newCount
                }
                hasLoadedInitialCount = true
            }
        }
    }

    fun retry() {
        setState { copy(hasFailed = false) }
        load()
    }

    fun onWordTypeChanged(wordType: WordType) = setState { copy(wordType = wordType) }

    fun onTextChanged(side: WordSide, index: Int, text: String) =
        updateInput(side, index) { it.copy(text = text) }

    fun onGenderChanged(side: WordSide, index: Int, gender: Gender?) =
        updateInput(side, index) { it.copy(gender = gender.takeIf { chosen -> chosen != it.gender }) }

    fun onFieldChanged(side: WordSide, index: Int, key: FieldKey, value: String) =
        updateInput(side, index) { it.withField(key, value) }

    /** Adds an alternative to one side only — the side whose button was pressed. */
    fun addMeaning(side: WordSide) = setState {
        when (side) {
            WordSide.SOURCE -> copy(sourceInputs = sourceInputs + WordFormInput())
            WordSide.TARGET -> copy(targetInputs = targetInputs + WordFormInput())
        }
    }

    /** Removes one alternative from one side; a side always keeps at least one. */
    fun removeMeaning(side: WordSide, index: Int) = setState {
        val inputs = inputsFor(side)
        if (inputs.size <= 1) return@setState this

        val remaining = inputs.filterIndexed { i, _ -> i != index }

        when (side) {
            WordSide.SOURCE -> copy(sourceInputs = remaining)
            WordSide.TARGET -> copy(targetInputs = remaining)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val state = currentState
        val dictionary = state.dictionary ?: return
        if (!state.canSave) return

        setState { copy(isSaving = true) }

        viewModelScope.launch {
            // Blank alternatives are dropped so surfaces and meta stay index-aligned; the first is
            // kept regardless, so a half-filled word still saves.
            val sources = state.sourceInputs.filter { it.text.isNotBlank() }
                .ifEmpty { state.sourceInputs.take(1) }
            val targets = state.targetInputs.filter { it.text.isNotBlank() }
                .ifEmpty { state.targetInputs.take(1) }

            val existing = editing
            val now = time.nowEpochMillis()

            val word = Word(
                wordId = existing?.wordId ?: Uuid.random().toString(),
                dictionaryId = dictionary.dictionaryId,
                word = composeWordText(dictionary.fromLang, sources),
                wordMeta = composeWordMeta(dictionary.fromLang, state.wordType, sources),
                translation = composeWordText(dictionary.toLang, targets),
                translationMeta = composeWordMeta(dictionary.toLang, state.wordType, targets),
                // Practice progress and creation time survive an edit.
                level = existing?.level ?: 0,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )

            val outcome = wordService.saveWord(word, isNew = existing == null)

            setState { copy(isSaving = false) }

            if (outcome is Outcome.Success) {
                if (existing == null) resetForm()
                _saved.emit(existing != null)
            } else {
                _messages.emit(strings.wordSaveFailed)
            }
        }
    }

    private fun updateInput(side: WordSide, index: Int, transform: (WordFormInput) -> WordFormInput) =
        setState {
            when (side) {
                WordSide.SOURCE -> copy(sourceInputs = sourceInputs.replace(index, transform))
                WordSide.TARGET -> copy(targetInputs = targetInputs.replace(index, transform))
            }
        }

    private fun resetForm() = setState {
        copy(
            sourceInputs = listOf(WordFormInput()),
            targetInputs = listOf(WordFormInput()),
        )
    }
}

private fun List<WordFormInput>.replace(
    index: Int,
    transform: (WordFormInput) -> WordFormInput,
): List<WordFormInput> = mapIndexed { i, input -> if (i == index) transform(input) else input }
