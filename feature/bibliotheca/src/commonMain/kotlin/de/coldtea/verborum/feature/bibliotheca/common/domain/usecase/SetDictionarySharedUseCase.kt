package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository

/** What came of the sharing switch. */
internal enum class SharingChange {

    UPDATED,

    /** The member's last shared dictionary; the server will not leave them with none. */
    LAST_SHARED,

    /** Unreachable, or refused for any other reason. The switch stays where it was. */
    FAILED,
}

/**
 * Shares or unshares one of the user's own dictionaries.
 *
 * **Online-only, and the one place an explicit `isPublic` is sent for a dictionary the server
 * already has** — see [SaveDictionaryRemotelyUseCase] for why every other write omits it. The local
 * row is written only after the server agrees, so a failed call leaves nothing to reconcile and the
 * switch simply does not move.
 */
internal class SetDictionarySharedUseCase(
    private val repository: DictionaryRepository,
) {
    suspend operator fun invoke(dictionaryId: String, isShared: Boolean): SharingChange {
        val dictionary = repository.findDictionary(dictionaryId) ?: return SharingChange.FAILED

        return when (val outcome = repository.setShared(dictionary, isShared)) {
            is Outcome.Success -> SharingChange.UPDATED

            is Outcome.Failure -> if (outcome.error.isSharingRequired()) {
                SharingChange.LAST_SHARED
            } else {
                SharingChange.FAILED
            }

            Outcome.Loading -> SharingChange.FAILED
        }
    }
}
