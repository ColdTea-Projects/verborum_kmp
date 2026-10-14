package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.core.localization.stringsFor
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.ObserveSharingStateUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.SharingState
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteOutcome
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.core.localization.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryFilterState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryListState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryUi
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.toUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class DictionaryListUiState(
    val listState: DictionaryListState = DictionaryListState.Loading,
    val filters: DictionaryFilterState = DictionaryFilterState(),
    val isRefreshing: Boolean = false,
)

/** One-shot events: opening a dictionary is navigation, not state. */
internal sealed interface DictionaryListEffect {
    data class OpenDictionary(val dictionaryId: String) : DictionaryListEffect
}

internal class DictionaryListViewModel(
    private val languageSettings: LanguageSettings,
    private val dictionaryService: DictionaryService,
    private val wordService: WordService,
    private val syncService: SyncService,
    private val importedDictionaryService: ImportedDictionaryService,
    observeSharingState: ObserveSharingStateUseCase,
) : BaseViewModel<DictionaryListUiState, DictionaryListEffect>(DictionaryListUiState()) {

    /**
     * Who may unshare or delete what. Held as state rather than asked for on each tap, because the
     * answer has to be known *before* a delete is attempted — see [deleteDictionary].
     */
    private val sharingState = observeSharingState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SharingState())

    /** Read fresh each time: the language can change while this screen is open. */
    private val strings: Strings get() = stringsFor(languageSettings.language.value)


    private val filters = MutableStateFlow(DictionaryFilterState())

    /**
     * Whether the last sync failed. Part of the state the list is derived from rather than something
     * written straight into it: the store keeps emitting, and an emission arriving after a failure
     * would otherwise overwrite the error surface with an empty list.
     */
    private val syncFailed = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Delete failures and other one-off notices; the screen puts them on the shared snackbar. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        observeDictionaries()
        refresh()
    }

    /**
     * The live subscription, kept so a re-subscribe replaces it instead of adding to it.
     *
     * Only [retry] re-subscribes, and only from the failed state, where the previous flow has
     * already completed — so nothing collects twice today. Cancelling first is what keeps that true
     * of a caller who does not know it.
     */
    private var dictionariesSubscription: Job? = null

    private fun observeDictionaries() {
        dictionariesSubscription?.cancel()

        dictionariesSubscription = viewModelScope.launch {
            combine(
                dictionaryService.observeDictionaries(),
                wordService.observeWordCounts(),
                filters,
                syncFailed,
            ) { dictionaries, wordCounts, filters, hasFailed ->
                val rows = dictionaries
                    .map { dictionary ->
                        // Null counts mean "not synced yet"; once known, a dictionary missing from
                        // the map genuinely has no words, and 0 is the right thing to show.
                        dictionary.toUi(wordCounts?.let { it[dictionary.dictionary.dictionaryId] ?: 0 })
                    }
                    .filterAndSort(filters)

                when {
                    // Nothing to show and the server could not be reached: the error surface is the
                    // honest answer. With rows on screen, stale data beats an error page.
                    rows.isEmpty() && hasFailed && !filters.hasActiveFilters ->
                        DictionaryListState.Failed

                    else -> DictionaryListState.Success(rows)
                }
            }
                .catch { emit(DictionaryListState.Failed) }
                .collect { listState -> setState { copy(listState = listState) } }
        }
    }

    /** Re-subscribes after a failure — the observed flow terminates when it errors. */
    fun retry() {
        setState { copy(listState = DictionaryListState.Loading) }
        observeDictionaries()
        refresh()
    }

    /**
     * Pulls from the server. Also the pull-to-refresh action, so the spinner has to clear on every
     * path — `Outcome` makes that explicit instead of depending on nothing having thrown.
     */
    fun refresh() {
        setState { copy(isRefreshing = true) }

        viewModelScope.launch {
            val outcome = syncService.syncDictionaries()
            val hasFailed = outcome is Outcome.Failure

            syncFailed.value = hasFailed
            setState { copy(isRefreshing = false) }

            // With rows still on screen the failure is not obvious, so it is said out loud.
            if (hasFailed && currentState.listState.hasRows()) {
                _messages.emit(strings.refreshFailed)
            }
        }
    }

    /** The in-flight [refreshInBackground], so a timer tick cannot stack a second sync on a slow one. */
    private var backgroundRefresh: Job? = null

    /**
     * [refresh] without the ceremony, for a screen that keeps itself current (roadmap `P4N-10`): the
     * user did not ask for it, so there is no spinner and a failure says nothing — the next tick, or
     * the next time the tab is in front, tries again. A success clears an earlier failure.
     *
     * Skipped while a refresh is already running, whoever started it.
     */
    fun refreshInBackground() {
        if (currentState.isRefreshing || backgroundRefresh?.isActive == true) return

        backgroundRefresh = viewModelScope.launch {
            if (syncService.syncDictionaries() is Outcome.Success) syncFailed.value = false
        }
    }

    fun onDictionaryClicked(dictionaryId: String) =
        emitEffect(DictionaryListEffect.OpenDictionary(dictionaryId))

    /** The magnifier toggles the search area; collapsing clears the query so its filter lifts. */
    fun toggleSearch() = updateFilters { state ->
        val expanded = !state.isSearchExpanded
        state.copy(isSearchExpanded = expanded, query = if (expanded) state.query else "")
    }

    fun onQueryChanged(query: String) = updateFilters { it.copy(query = query) }

    fun onFromLanguageChanged(language: SupportedLanguage?) =
        updateFilters { it.copy(fromLanguage = language) }

    fun onToLanguageChanged(language: SupportedLanguage?) =
        updateFilters { it.copy(toLanguage = language) }

    fun onSortChanged(sort: DictionarySort) = updateFilters { it.copy(sort = sort) }

    /** Resets search, both language filters and the sort — the Clear chip. */
    fun clearFilters() = updateFilters { DictionaryFilterState() }

    /**
     * Deletes an own dictionary.
     *
     * The guard in front of the request is not belt-and-braces: without it, the row is tombstoned —
     * gone from the list — before the server refuses, and the user watches their last shared
     * dictionary disappear and come back. Checking first means nothing moves at all.
     */
    fun deleteDictionary(dictionaryId: String) {
        if (sharingState.value.isLastShared(dictionaryId)) {
            viewModelScope.launch { _messages.emit(strings.errorDeleteLastShared) }
            return
        }

        viewModelScope.launch {
            when (dictionaryService.deleteDictionary(dictionaryId)) {
                // Gone, or gone from view with the sync to finish it. Either way, silence: the row
                // has already left the list, which is the only answer the user was waiting for.
                DeleteOutcome.DELETED, DeleteOutcome.PENDING -> Unit

                DeleteOutcome.SHARING_REQUIRED -> _messages.emit(strings.errorDeleteLastShared)

                DeleteOutcome.REFUSED -> _messages.emit(strings.dictionaryDeleteFailed)
            }
        }
    }

    /** Removes a downloaded dictionary: the vault entry goes, then the local copy. Online-only. */
    fun unsubscribe(dictionaryId: String) {
        viewModelScope.launch {
            if (importedDictionaryService.unsubscribe(dictionaryId) is Outcome.Failure) {
                _messages.emit(strings.errorUnsubscribeFailed)
            }
        }
    }

    private fun updateFilters(reducer: (DictionaryFilterState) -> DictionaryFilterState) {
        val updated = reducer(filters.value)
        filters.value = updated
        setState { copy(filters = updated) }
    }

    private fun List<DictionaryUi>.filterAndSort(
        filters: DictionaryFilterState,
    ): List<DictionaryUi> {
        val query = filters.query.trim()

        return filter { dictionary ->
            (query.isEmpty() || dictionary.name.contains(query, ignoreCase = true)) &&
                (
                    filters.fromLanguage == null ||
                        dictionary.fromLang.equals(filters.fromLanguage.code, ignoreCase = true)
                    ) &&
                (
                    filters.toLanguage == null ||
                        dictionary.toLang.equals(filters.toLanguage.code, ignoreCase = true)
                    )
        }.sortedWith(filters.sort.comparator())
    }
}

private fun DictionaryListState.hasRows(): Boolean =
    this is DictionaryListState.Success && dictionaries.isNotEmpty()
