package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteOutcome
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionaryUseCase
import kotlinx.coroutines.flow.Flow

/**
 * What screens talk to for dictionaries. The use cases stay one concern each; this composes them.
 *
 * It deals in [LibraryDictionary], not a screen's row model: the list row and the details header
 * need different shapes, and each maps what it needs in its own `ui/model`. Reads cover both kinds —
 * the user's own dictionaries and the ones they downloaded — while every write is about an own one,
 * because a download is read-only.
 */
internal class DictionaryService(
    private val observeDictionariesUseCase: ObserveDictionariesUseCase,
    private val observeDictionaryUseCase: ObserveDictionaryUseCase,
    private val deleteDictionaryUseCase: DeleteDictionaryUseCase,
    private val repository: DictionaryRepository,
    private val imported: ImportedDictionaryService,
) {

    fun observeDictionaries(): Flow<List<LibraryDictionary>> = observeDictionariesUseCase()

    /** Emits null once the dictionary is gone, which is how the details screen learns to leave. */
    fun observeDictionary(dictionaryId: String): Flow<LibraryDictionary?> =
        observeDictionaryUseCase(dictionaryId)

    suspend fun deleteDictionary(dictionaryId: String): DeleteOutcome =
        deleteDictionaryUseCase(dictionaryId)

    /**
     * One-shot read for prefilling the **edit** form, and deliberately own-only: a download cannot
     * be edited, so a caller that is about to write wants exactly this.
     */
    suspend fun dictionary(dictionaryId: String): Dictionary? = repository.findDictionary(dictionaryId)

    /**
     * One-shot read across the whole library — the user's own dictionaries *and* their downloads.
     *
     * The question this answers is "does this exist at all", which is not the same question
     * [dictionary] answers. Asking the own-only one made the details screen report every downloaded
     * dictionary as deleted and bounce straight back to the list.
     */
    suspend fun libraryDictionary(dictionaryId: String): LibraryDictionary? =
        repository.findDictionary(dictionaryId)?.asLibraryRow()
            ?: imported.find(dictionaryId)?.asLibraryRow()

    suspend fun saveDictionary(dictionary: Dictionary, isNew: Boolean): Outcome<Unit> =
        repository.save(dictionary, isNew)
}
