package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository

/**
 * The download half of the sync: fetches the user's dictionaries and merges them into the store,
 * where the merge itself decides what local state survives (see `DictionaryStore.merge`).
 *
 * Download only, on purpose: the upload half is
 * [UploadPendingChangesUseCase][de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase],
 * which `SyncService` runs in front of this so a download can never drop something the server has
 * not seen.
 */
internal class SyncUserDictionariesUseCase(
    private val repository: DictionaryRepository,
) {
    suspend operator fun invoke(userId: String): Outcome<Unit> =
        repository.pullDictionaries(userId).map { }
}
