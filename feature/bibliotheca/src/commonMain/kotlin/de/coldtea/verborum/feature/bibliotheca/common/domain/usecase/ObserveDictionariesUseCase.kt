package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.LibraryDictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.asLibraryRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class ObserveDictionariesUseCase(
    private val repository: DictionaryRepository,
    private val imported: ImportedDictionaryService,
) {
    /**
     * The user's own dictionaries and the ones they downloaded, as one list.
     *
     * Deduplicated so a sync that changes only bookkeeping the list cannot show — `isSynced`, tags —
     * produces no emission, and the list does not recompose for it. Tags are dropped from this
     * projection for the same reason: they are shown on the details screen only, so a tag pull would
     * otherwise redraw every row in the list.
     */
    operator fun invoke(): Flow<List<LibraryDictionary>> = combine(
        repository.observeDictionaries(),
        imported.dictionaries,
    ) { own, downloaded ->
        own.map { dictionary -> dictionary.asLibraryRow() } +
            downloaded.map(ImportedDictionary::asLibraryRow)
    }
        .map { rows ->
            rows.map { row -> row.copy(dictionary = row.dictionary.copy(tags = emptyList())) }
        }
        .distinctUntilChanged()
}
