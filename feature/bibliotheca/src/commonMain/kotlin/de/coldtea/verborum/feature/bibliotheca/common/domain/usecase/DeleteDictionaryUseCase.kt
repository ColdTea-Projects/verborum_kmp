package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.isWorthKeeping

/** What came of asking the server to delete a dictionary. */
internal enum class DeleteOutcome {

    /** Gone, locally and remotely. */
    DELETED,

    /** The server could not be reached; the row stays tombstoned for the next sync to finish. */
    PENDING,

    /** The member's last shared dictionary — the server will not leave them with none shared. */
    SHARING_REQUIRED,

    /** The server refused for some other reason; the row is back. */
    REFUSED,
}

/**
 * Deletes a dictionary, local view first: the row and its words are tombstoned so they disappear
 * immediately, then the server is asked.
 *
 * **Only `DELETE /dictionaries/{id}` is sent, and the words are never deleted remotely.** The server
 * removes them in the same transaction, *after* checking the sharing rule — so deleting the words up
 * front and then being refused destroys the words of a dictionary that still exists. That was a real
 * data-loss bug on Android and the same ordering was here; this is the fix, not a preference.
 *
 * The three failure outcomes differ in what they do locally, and each matters:
 *
 * - **Never landed** ([DeleteOutcome.PENDING]): the tombstone stands, the dictionary stays gone from
 *   the user's view, and the next sync finishes the job with this very use case.
 * - **Refused** ([DeleteOutcome.SHARING_REQUIRED], [DeleteOutcome.REFUSED]): the tombstone is lifted,
 *   so the dictionary reappears rather than quietly vanishing from a list that never changed — and
 *   so the sync stops replaying a delete the server has already judged, forever.
 */
internal class DeleteDictionaryUseCase(
    private val dictionaryRepository: DictionaryRepository,
    private val wordRepository: WordRepository,
) {
    suspend operator fun invoke(dictionaryId: String): DeleteOutcome {
        dictionaryRepository.markDeleted(dictionaryId)
        // Locally only, and the reason is the upload set: an unsynced word of a dictionary being
        // deleted would otherwise be pushed to a dictionary that is on its way out.
        wordRepository.markDictionaryDeleted(dictionaryId)

        return when (val outcome = dictionaryRepository.deleteRemotely(dictionaryId)) {
            is Outcome.Success -> {
                wordRepository.removeDictionaryLocally(dictionaryId)
                dictionaryRepository.removeLocally(dictionaryId)
                DeleteOutcome.DELETED
            }

            is Outcome.Failure -> when {
                outcome.error.isWorthKeeping() -> DeleteOutcome.PENDING

                else -> {
                    dictionaryRepository.restore(dictionaryId)
                    wordRepository.restoreDictionaryWords(dictionaryId)

                    if (outcome.error.isSharingRequired()) {
                        DeleteOutcome.SHARING_REQUIRED
                    } else {
                        DeleteOutcome.REFUSED
                    }
                }
            }

            Outcome.Loading -> DeleteOutcome.PENDING
        }
    }
}

/**
 * The server's own exception name, which is the only thing separating this 400 from any other.
 * Branching on the code *within* a status — never on the sentence, which is written for people and
 * may change.
 */
internal fun VerborumError.isSharingRequired(): Boolean =
    this is VerborumError.Http && status == 400 && code == SHARING_REQUIRED_EXCEPTION

internal const val SHARING_REQUIRED_EXCEPTION = "SharingRequiredException"
