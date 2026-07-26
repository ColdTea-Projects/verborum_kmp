package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model

import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage

/** What the list is showing: skeletons, an error surface, or rows. */
internal sealed interface DictionaryListState {
    data object Loading : DictionaryListState
    data object Failed : DictionaryListState
    data class Success(val dictionaries: List<DictionaryUi>) : DictionaryListState
}

/**
 * The search, filter and sort selection. Held in the view model rather than the composable so it
 * survives navigating in and out of the screen, and so the filtering is testable in one place.
 */
internal data class DictionaryFilterState(
    val isSearchExpanded: Boolean = false,
    val query: String = "",
    /** Null means "Any language". */
    val fromLanguage: SupportedLanguage? = null,
    val toLanguage: SupportedLanguage? = null,
    val sort: DictionarySort = DictionarySort.NEWEST,
) {
    /** The Clear chip only earns its place when something is actually filtered. */
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || fromLanguage != null || toLanguage != null
}
