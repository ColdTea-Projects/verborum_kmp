package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.core.common.ApiTimestamp
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.common.getOrNull
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryApi
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryTagDto
import de.coldtea.verborum.feature.bibliotheca.common.data.WordApi
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedWord

/** A downloaded dictionary and its words, as the owner currently has them. */
internal data class ImportedContent(
    val dictionary: ImportedDictionary,
    val words: List<ImportedWord>,
)

/**
 * Reads someone else's public dictionary and its words.
 *
 * `Success(null)` means **unreadable**: a 404 because the owner made it private, deleted it, or left
 * the Forum. That is a legitimate state with a defined response — drop the local copy — so it is a
 * success carrying nothing rather than a failure. A failure means the request did not land, and the
 * caller keeps what it has.
 *
 * A `fun interface` so a test can state what the owner's copy looks like without standing up an HTTP
 * client — which matters more than it sounds: driving a mock engine from a view model's scope does
 * not settle under `advanceUntilIdle` on Kotlin/Native, so the seam is what keeps those tests
 * behaving the same on both targets.
 */
internal fun interface FetchImportedDictionaryContent {
    suspend operator fun invoke(
        dictionaryId: String,
        publisherName: String?,
        importedAt: Long,
    ): Outcome<ImportedContent?>
}

/**
 * The real one, reading from **ms_dictionary** rather than the marketplace: the Forum answers what
 * is on offer, the dictionary service answers what it actually contains, and an importer reads it
 * exactly as they read their own — only read-only.
 */
internal class NetworkImportedDictionaryContent(
    private val dictionaryApi: DictionaryApi,
    private val wordApi: WordApi,
) : FetchImportedDictionaryContent {
    override suspend fun invoke(
        dictionaryId: String,
        publisherName: String?,
        importedAt: Long,
    ): Outcome<ImportedContent?> {
        val dictionary = when (val outcome = dictionaryApi.dictionary(dictionaryId)) {
            is Outcome.Success -> outcome.data
            is Outcome.Failure -> return if (outcome.error.isNotFound()) {
                Outcome.Success(null)
            } else {
                outcome
            }

            Outcome.Loading -> return Outcome.Loading
        }

        // Tags and words are best effort: a dictionary that reads but whose tags do not is still
        // worth having, and an unreadable word list answers `[]` rather than 404 anyway.
        val tags = dictionaryApi.tagsOf(dictionaryId).getOrNull()
            .orEmpty()
            .map(DictionaryTagDto::tag)
            .map { tag -> tag.trim().lowercase() }
            .filter(String::isNotEmpty)
            .distinct()

        val words = wordApi.wordsOfDictionary(dictionaryId).getOrNull().orEmpty()
            .mapNotNull { dto ->
                val wordId = dto.wordId?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
                val createdAt = ApiTimestamp.parse(dto.createdAt) ?: importedAt

                ImportedWord(
                    wordId = wordId,
                    dictionaryId = dictionaryId,
                    word = dto.word.orEmpty(),
                    wordMeta = dto.wordMeta.orEmpty(),
                    translation = dto.translation.orEmpty(),
                    translationMeta = dto.translationMeta.orEmpty(),
                    // Always 0, never the server's value: `level` on someone else's word is the
                    // *owner's* progress. The store then carries over whatever this device knew, so
                    // a refresh does not reset the user's own practice.
                    level = 0,
                    createdAt = createdAt,
                    updatedAt = ApiTimestamp.parse(dto.updatedAt) ?: createdAt,
                )
            }

        return Outcome.Success(
            ImportedContent(
                dictionary = ImportedDictionary(
                    dictionaryId = dictionaryId,
                    publisherId = dictionary.userId,
                    publisherName = publisherName?.takeIf(String::isNotBlank),
                    name = dictionary.name,
                    fromLang = dictionary.fromLang,
                    toLang = dictionary.toLang,
                    tags = tags,
                    importedAt = importedAt,
                ),
                words = words,
            ),
        )
    }
}

private fun VerborumError.isNotFound(): Boolean = this is VerborumError.Http && status == 404
