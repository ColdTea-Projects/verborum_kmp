package de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryUi
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.toUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * What the dictionary list's view model talks to. The use cases stay one concern each; this is the
 * place that composes them and maps the domain model to the shape the screen renders.
 */
internal class DictionaryService(
    private val observeDictionariesUseCase: ObserveDictionariesUseCase,
    private val deleteDictionaryUseCase: DeleteDictionaryUseCase,
) {

    fun observeDictionaries(): Flow<List<DictionaryUi>> =
        observeDictionariesUseCase().map { dictionaries -> dictionaries.map(Dictionary::toUi) }

    suspend fun deleteDictionary(dictionaryId: String): Outcome<Unit> =
        deleteDictionaryUseCase(dictionaryId)
}
