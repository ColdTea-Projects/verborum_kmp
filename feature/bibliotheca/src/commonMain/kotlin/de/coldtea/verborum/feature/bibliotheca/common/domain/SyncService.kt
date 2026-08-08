package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase

/**
 * Brings local state and the server together. One entry point per thing a screen opens, so callers do
 * not have to know which requests that takes: the list asks for [syncDictionaries], the details screen
 * for [syncDictionaryWords], and both are also the pull-to-refresh action.
 *
 * Errors are values here, not exceptions: a sync that could not reach the server reports a failure and
 * leaves local state untouched, so a screen can decide whether that is worth showing.
 */
internal class SyncService(
    private val activeUser: ActiveUserUseCase,
    private val syncDictionariesUseCase: SyncUserDictionariesUseCase,
    private val wordRepository: WordRepository,
    private val uploadPendingChanges: UploadPendingChangesUseCase,
) {

    /**
     * Downloads the user's dictionaries and, in one further request, every word they own — which is
     * what puts a count on each list row. Signed out there is nothing to reconcile against, which is a
     * success with nothing to do rather than an error.
     *
     * The dictionaries decide the outcome: without words the list still works, just without counts,
     * so a failed word pull is not worth turning the whole screen into an error.
     */
    suspend fun syncDictionaries(): Outcome<Unit> {
        val userId = activeUser() ?: return Outcome.Success(Unit)

        // Upload first, always: a download that ran before it could drop a row the server has not
        // been told about yet. This is also the only thing that drains changes made offline.
        uploadPendingChanges()

        return syncDictionariesUseCase(userId).also { outcome ->
            if (outcome is Outcome.Success) wordRepository.pullAllWords(userId)
        }
    }

    /** Downloads one dictionary's words, for the details screen. */
    suspend fun syncDictionaryWords(dictionaryId: String): Outcome<Unit> {
        activeUser() ?: return Outcome.Success(Unit)

        uploadPendingChanges()

        return wordRepository.pullWords(dictionaryId)
    }
}
