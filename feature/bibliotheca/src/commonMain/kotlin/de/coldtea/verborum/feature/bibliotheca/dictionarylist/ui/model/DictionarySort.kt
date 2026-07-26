package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model

/**
 * How the dictionary list is ordered; [NEWEST] is the default. [label] is shown both on the sort
 * chip and in the sort sheet.
 *
 * The language orders sort by the stored language *code* — a deterministic proxy for the display
 * name — and every comparator ends on the id so equal rows keep a fixed order instead of
 * reshuffling on each emission.
 */
internal enum class DictionarySort(val label: String) {
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    FROM_LANGUAGE("From language"),
    TO_LANGUAGE("To language"),
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    ;

    fun comparator(): Comparator<DictionaryUi> = when (this) {
        NAME_ASC -> compareBy({ it.name.lowercase() }, { it.dictionaryId })
        NAME_DESC -> compareByDescending<DictionaryUi> { it.name.lowercase() }
            .thenBy { it.dictionaryId }

        FROM_LANGUAGE -> compareBy({ it.fromLang }, { it.name.lowercase() }, { it.dictionaryId })
        TO_LANGUAGE -> compareBy({ it.toLang }, { it.name.lowercase() }, { it.dictionaryId })
        NEWEST -> compareByDescending<DictionaryUi> { it.createdAt }.thenBy { it.dictionaryId }
        OLDEST -> compareBy({ it.createdAt }, { it.dictionaryId })
    }
}
