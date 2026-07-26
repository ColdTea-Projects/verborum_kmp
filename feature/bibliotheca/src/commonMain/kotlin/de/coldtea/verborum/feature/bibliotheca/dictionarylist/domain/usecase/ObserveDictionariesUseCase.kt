package de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.usecase

import de.coldtea.verborum.feature.bibliotheca.dictionarylist.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.Dictionary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class ObserveDictionariesUseCase(
    private val repository: DictionaryRepository,
) {
    /**
     * Deduplicated so a sync that changes only bookkeeping the list cannot show — `isSynced`, tags —
     * produces no emission, and the list does not recompose for it.
     */
    operator fun invoke(): Flow<List<Dictionary>> =
        repository.observeDictionaries().distinctUntilChanged()
}
