package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryFilterState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryListState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryUi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    private val dictionaryService: DictionaryService,
    private val syncService: SyncService,
) : BaseViewModel<DictionaryListUiState, DictionaryListEffect>(DictionaryListUiState()) {

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

    private fun observeDictionaries() {
        viewModelScope.launch {
            combine(
                dictionaryService.observeDictionaries(),
                filters,
                syncFailed,
            ) { dictionaries, filters, hasFailed ->
                val rows = dictionaries.filterAndSort(filters)

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
                _messages.emit("Could not refresh. Showing what was loaded earlier.")
            }
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

    fun deleteDictionary(dictionaryId: String) {
        viewModelScope.launch {
            if (dictionaryService.deleteDictionary(dictionaryId) is Outcome.Failure) {
                _messages.emit("That dictionary could not be deleted. It is back in your list.")
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
