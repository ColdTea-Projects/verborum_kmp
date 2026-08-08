package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.isWorthKeeping

/**
 * Deletes a dictionary, local view first: the row is tombstoned so it disappears immediately, then
 * the server is asked. On success the row goes for good.
 *
 * A request that never reached the server leaves the tombstone standing and reports success — the
 * dictionary stays gone from the user's view and `UploadPendingChangesUseCase` finishes the delete
 * on the next sync, which is what the Android app's worker does. Only a delete the server actively
 * refused lifts the tombstone, so the dictionary reappears rather than quietly vanishing from a
 * list that never actually changed.
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

            is Outcome.Failure -> when {
                outcome.error.isWorthKeeping() -> Outcome.Success(Unit)

                else -> {
                    repository.restore(dictionaryId)
                    outcome
                }
            }

            Outcome.Loading -> Outcome.Loading
        }
    }
}
