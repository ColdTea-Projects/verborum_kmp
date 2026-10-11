package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.marketplace.DictionaryLanguages
import de.coldtea.verborum.core.marketplace.OwnedDictionariesSource
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import kotlinx.coroutines.flow.first

/**
 * This feature's answer to "which language pairs does the user already work in" — what the Forum
 * opens its default filter on.
 *
 * Tombstoned rows are excluded: a dictionary on its way out should not shape what the user is
 * offered. Downloaded ones are excluded too, deliberately — the Forum adds those from the vault
 * itself, with their own activity times, and counting them here would count each one twice.
 */
internal class OwnedLanguagesSource(
    private val repository: DictionaryRepository,
) : OwnedDictionariesSource {

    override suspend fun ownedLanguages(): List<DictionaryLanguages> =
        repository.observeDictionaries().first().map { dictionary ->
            DictionaryLanguages(
                fromLang = dictionary.fromLang,
                toLang = dictionary.toLang,
                // `updatedAt` is 0 until the server first stamps the row, so a dictionary created
                // offline would otherwise rank as the oldest thing the user owns.
                lastActivityAt = maxOf(dictionary.updatedAt, dictionary.createdAt),
            )
        }
}
