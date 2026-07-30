package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionaryUseCase
import kotlinx.coroutines.flow.Flow

/**
 * What screens talk to for dictionaries. The use cases stay one concern each; this composes them.
 *
 * It deals in the domain [Dictionary], not a screen's row model: the list row and the details header
 * need different shapes, and each maps what it needs in its own `ui/model`.
 */
internal class DictionaryService(
    private val observeDictionariesUseCase: ObserveDictionariesUseCase,
    private val observeDictionaryUseCase: ObserveDictionaryUseCase,
    private val deleteDictionaryUseCase: DeleteDictionaryUseCase,
    private val repository: DictionaryRepository,
) {

    fun observeDictionaries(): Flow<List<Dictionary>> = observeDictionariesUseCase()

    /** Emits null once the dictionary is gone, which is how the details screen learns to leave. */
    fun observeDictionary(dictionaryId: String): Flow<Dictionary?> =
        observeDictionaryUseCase(dictionaryId)

    suspend fun deleteDictionary(dictionaryId: String): Outcome<Unit> =
        deleteDictionaryUseCase(dictionaryId)

    /** One-shot read for prefilling the edit form. */
    suspend fun dictionary(dictionaryId: String): Dictionary? = repository.findDictionary(dictionaryId)

    suspend fun saveDictionary(dictionary: Dictionary, isNew: Boolean): Outcome<Unit> =
        repository.save(dictionary, isNew)
}
