package de.coldtea.verborum.feature.bibliotheca.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError

/**
 * Stand-in for the dictionary endpoint so the shell runs end to end before the
 * backend is wired up. Swap this binding in the Koin module for the HTTP one.
 */
class InMemoryWordRepository : WordRepository {

    private val words = listOf(
        Word("verbum", "verbum", "word", "That which is spoken; a single unit of language."),
        Word("liber", "liber", "book", "A written or printed work of pages bound together."),
        Word("lumen", "lumen", "light", "Light, especially as a source of understanding."),
        Word("forum", "forum", "marketplace", "A public square for trade and public affairs."),
    )

    override suspend fun search(query: String): Outcome<List<Word>> = Outcome.Success(
        if (query.isBlank()) {
            words
        } else {
            words.filter { it.lemma.contains(query, ignoreCase = true) || it.translation.contains(query, ignoreCase = true) }
        }
    )

    override suspend fun word(id: String): Outcome<Word> =
        words.firstOrNull { it.id == id }
            ?.let { Outcome.Success(it) }
            ?: Outcome.Failure(VerborumError.Http(status = 404, code = "word_not_found"))
}
