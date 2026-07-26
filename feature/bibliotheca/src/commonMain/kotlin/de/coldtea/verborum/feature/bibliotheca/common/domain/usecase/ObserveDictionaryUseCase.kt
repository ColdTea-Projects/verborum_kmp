package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class ObserveDictionaryUseCase(
    private val repository: DictionaryRepository,
) {
    /** Null once the dictionary is deleted or has never been seen. */
    operator fun invoke(dictionaryId: String): Flow<Dictionary?> =
        repository.observeDictionary(dictionaryId).distinctUntilChanged()
}
