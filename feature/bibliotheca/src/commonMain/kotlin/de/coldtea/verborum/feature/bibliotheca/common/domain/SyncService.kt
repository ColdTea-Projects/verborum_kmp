package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome

/**
 * Brings local state and the server together. One entry point per direction so callers do not have
 * to know which half they need: the list screen asks for a full [syncDictionaries] on open and on
 * pull-to-refresh, and a future push handler or reconnect trigger can call the same thing.
 *
 * Errors are values here, not exceptions: a sync that could not reach the server reports a failure
 * and leaves local state untouched, so a screen can decide whether that is worth showing.
 */
internal class SyncService(
    private val activeUser: ActiveUserUseCase,
    private val syncDictionariesUseCase: SyncUserDictionariesUseCase,
) {

    /**
     * Downloads the signed-in user's dictionaries. Signed out there is nothing to reconcile
     * against, which is a success with nothing to do rather than an error to report.
     */
    suspend fun syncDictionaries(): Outcome<Unit> {
        val userId = activeUser() ?: return Outcome.Success(Unit)

        return syncDictionariesUseCase(userId)
    }
}
