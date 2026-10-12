package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.core.localization.stringsFor
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.common.TimeProvider
import de.coldtea.verborum.feature.bibliotheca.common.domain.ActiveUserUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.ObserveSharingStateUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.SharingState
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.core.localization.SupportedLanguage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal data class CreateDictionaryUiState(
    val name: String = "",
    val fromLanguage: SupportedLanguage? = null,
    val toLanguage: SupportedLanguage? = null,
    val tags: List<String> = emptyList(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    /** Only ever true on the way to an edit; a new dictionary has nothing to wait for. */
    val isLoading: Boolean = false,
    val hasFailed: Boolean = false,
) {
    /** A dictionary needs a name and both directions before it can be saved. */
    val canSave: Boolean
        get() = name.isNotBlank() && fromLanguage != null && toLanguage != null && !isSaving
}

/** Saving succeeded; the id is the created — or edited — dictionary. */
internal data class DictionarySaved(val dictionaryId: String, val wasEditing: Boolean)

internal class CreateDictionaryViewModel(
    private val languageSettings: LanguageSettings,
    private val dictionaryId: String?,
    private val dictionaryService: DictionaryService,
    private val syncService: SyncService,
    private val activeUser: ActiveUserUseCase,
    observeSharingState: ObserveSharingStateUseCase,
    private val time: TimeProvider = SystemTimeProvider,
) : BaseViewModel<CreateDictionaryUiState, Nothing>(CreateDictionaryUiState()) {

    /** Only read for one thing: whether this new dictionary has to be the member's shared one. */
    private val sharingState = observeSharingState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SharingState())

    /** Read fresh each time: the language can change while this screen is open. */
    private val strings: Strings get() = stringsFor(languageSettings.language.value)


    // Replays one, unlike the other screens: the prefill failure is emitted from `init`, before the
    // screen has had a chance to subscribe, and a dropped message would leave a silently blank form.
    private val _messages = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _saved = MutableSharedFlow<DictionarySaved>(extraBufferCapacity = 1)

    /** Emitted only on a successful save, so the screen never leaves on a failed one. */
    val saved: SharedFlow<DictionarySaved> = _saved.asSharedFlow()

    private var editing: Dictionary? = null

    /** One pull per attempt; [retry] starts a fresh one. */
    private var hasPulled = false

    init {
        if (dictionaryId != null) prefill(dictionaryId)
    }

    private fun prefill(dictionaryId: String) {
        setState { copy(isLoading = true) }

        viewModelScope.launch {
            // A link can open this form before anything has been downloaded — one someone shared, or
            // a reload, after which the web app remembers nothing between visits.
            val row = dictionaryService.dictionary(dictionaryId) ?: pulled(dictionaryId)

            if (row == null) {
                // Emphatically *not* a blank create form: this screen was opened to change one
                // dictionary, and a form that forgot which would save a second one beside it.
                _messages.emit(strings.dictionaryLoadFailed)
                setState { copy(isLoading = false, hasFailed = true) }
                return@launch
            }

            // Tags live beside the dictionary rather than inside it, so the row alone never carries
            // them: however it was found, the chips would come up empty without this.
            syncService.syncDictionary(dictionaryId)

            val dictionary = dictionaryService.dictionary(dictionaryId) ?: row

            editing = dictionary
            setState {
                copy(
                    name = dictionary.name,
                    fromLanguage = SupportedLanguage.fromCode(dictionary.fromLang),
                    toLanguage = SupportedLanguage.fromCode(dictionary.toLang),
                    tags = dictionary.tags,
                    isEditing = true,
                    isLoading = false,
                    hasFailed = false,
                )
            }
        }
    }

    fun onNameChanged(name: String) = setState { copy(name = name) }

    fun onFromLanguageChanged(language: SupportedLanguage) = setState { copy(fromLanguage = language) }

    fun onToLanguageChanged(language: SupportedLanguage) = setState { copy(toLanguage = language) }

    fun onTagToggled(code: String) = setState {
        copy(tags = if (code in tags) tags - code else tags + code)
    }

    @OptIn(ExperimentalUuidApi::class)
    /** The account, downloaded once per attempt, for a store that has never held this row. */
    private suspend fun pulled(dictionaryId: String): Dictionary? {
        if (hasPulled) return null

        hasPulled = true
        syncService.syncDictionaries()

        return dictionaryService.dictionary(dictionaryId)
    }

    fun retry() {
        hasPulled = false
        setState { copy(hasFailed = false) }
        dictionaryId?.let(::prefill)
    }

    fun save() {
        val state = currentState
        val fromLanguage = state.fromLanguage ?: return
        val toLanguage = state.toLanguage ?: return
        if (!state.canSave) return

        setState { copy(isSaving = true) }

        viewModelScope.launch {
            val existing = editing
            val now = time.nowEpochMillis()

            val dictionary = Dictionary(
                // A new dictionary is identified client-side so the row can appear before the
                // server has answered.
                dictionaryId = existing?.dictionaryId ?: Uuid.random().toString(),
                userId = existing?.userId ?: activeUser().orEmpty(),
                name = state.name.trim(),
                // New dictionaries are private — except a member's *first* one, which the server
                // insists must be shared: a member who has dictionaries has to keep one in the
                // Forum, so creating a private first one is refused. An existing dictionary keeps
                // whatever the server last said, and the save omits the field anyway.
                isPublic = existing?.isPublic ?: sharingState.value.needsFirstShare,
                fromLang = fromLanguage.code,
                toLang = toLanguage.code,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                tags = state.tags,
            )

            val outcome = dictionaryService.saveDictionary(dictionary, isNew = existing == null)

            setState { copy(isSaving = false) }

            if (outcome is Outcome.Success) {
                _saved.emit(DictionarySaved(dictionary.dictionaryId, wasEditing = existing != null))
            } else {
                _messages.emit(strings.dictionarySaveFailed)
            }
        }
    }
}
