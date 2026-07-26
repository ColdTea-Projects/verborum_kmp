package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
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
