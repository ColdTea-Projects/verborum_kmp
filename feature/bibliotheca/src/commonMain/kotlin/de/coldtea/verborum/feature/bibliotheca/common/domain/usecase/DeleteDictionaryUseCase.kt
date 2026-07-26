package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository

/**
 * Deletes a dictionary, local view first: the row is tombstoned so it disappears immediately, then
 * the server is asked. On success the row goes for good; on failure the tombstone is lifted so the
 * dictionary reappears rather than quietly vanishing from a list that never actually changed.
 *
 * The Android app keeps the tombstone and retries via its worker, which it can do because the row
 * is on disk. Without a local database, a tombstone that outlived the screen would be invisible
 * state, so a failed delete is reported instead.
 */
internal class DeleteDictionaryUseCase(
    private val repository: DictionaryRepository,
) {
    suspend operator fun invoke(dictionaryId: String): Outcome<Unit> {
        repository.markDeleted(dictionaryId)

        return when (val outcome = repository.deleteRemotely(dictionaryId)) {
            is Outcome.Success -> {
                repository.removeLocally(dictionaryId)
                outcome
            }

            is Outcome.Failure -> {
                repository.restore(dictionaryId)
                outcome
            }

            Outcome.Loading -> Outcome.Loading
        }
    }
}
