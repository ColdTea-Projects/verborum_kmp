package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository

/**
 * The download half of the sync: fetches the user's dictionaries and merges them into the store,
 * where the merge itself decides what local state survives (see `DictionaryStore.merge`).
 *
 * The Android app also uploads pending local changes first, so a download can never drop something
 * the server has not seen. There is nothing to upload here yet — creating and editing dictionaries
 * is not implemented, and deletes are pushed as they happen — so this is download-only. When the
 * create/edit screens land, the upload phase belongs in front of this call.
 */
internal class SyncUserDictionariesUseCase(
    private val repository: DictionaryRepository,
) {
    suspend operator fun invoke(userId: String): Outcome<Unit> =
        repository.pullDictionaries(userId).map { }
}
