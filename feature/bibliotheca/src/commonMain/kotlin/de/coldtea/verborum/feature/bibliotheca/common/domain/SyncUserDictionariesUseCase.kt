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
        repository.pullDictionaries(userId)
            .also { outcome ->
                // `isPublic` always comes from the server, **even on a row with unsynced edits**.
                // The merge deliberately leaves such a row alone so offline work is not lost, but
                // that protection must not extend to this one field: the server changes it by event
                // when the user joins or leaves the Forum, so a row that kept its local copy would
                // push the stale value on the next upload and silently undo the join — and because
                // the server then refuses it, retry forever.
                if (outcome is Outcome.Success) {
                    outcome.data.forEach { remote ->
                        repository.applyRemoteSharing(remote.dictionaryId, remote.isPublic)
                    }
                }
            }
            .map { }
}
