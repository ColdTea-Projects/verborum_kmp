package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.LibraryDictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.asLibraryRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal class ObserveDictionaryUseCase(
    private val repository: DictionaryRepository,
    private val imported: ImportedDictionaryService,
) {
    /**
     * Null once the dictionary is deleted or has never been seen — which is how a details screen
     * learns to leave, and now also how it learns the user has left the Forum: the downloaded flow
     * answers null for a non-member.
     *
     * An id is one kind or the other, so the own copy simply wins; it can never be both.
     */
    operator fun invoke(dictionaryId: String): Flow<LibraryDictionary?> = combine(
        repository.observeDictionary(dictionaryId),
        imported.dictionary(dictionaryId),
    ) { own, downloaded ->
        own?.asLibraryRow() ?: downloaded?.asLibraryRow()
    }.distinctUntilChanged()
}
