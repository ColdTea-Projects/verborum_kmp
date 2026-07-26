package de.coldtea.verborum.feature.bibliotheca.data

import de.coldtea.verborum.core.common.Outcome

/** One dictionary entry. */
data class Word(
    val id: String,
    val lemma: String,
    val translation: String,
    val definition: String,
)

interface WordRepository {
    suspend fun search(query: String): Outcome<List<Word>>
    suspend fun word(id: String): Outcome<Word>
}
