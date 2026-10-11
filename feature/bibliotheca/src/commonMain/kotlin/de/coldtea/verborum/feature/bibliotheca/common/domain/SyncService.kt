package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase

/**
 * Brings local state and the server together. One entry point per thing a screen opens, so callers do
 * not have to know which requests that takes: the list asks for [syncDictionaries], the details screen
 * for [syncDictionary], and both are also the pull-to-refresh action.
 *
 * Errors are values here, not exceptions: a sync that could not reach the server reports a failure and
 * leaves local state untouched, so a screen can decide whether that is worth showing.
 */
internal class SyncService(
    private val activeUser: ActiveUserUseCase,
    private val syncDictionariesUseCase: SyncUserDictionariesUseCase,
    private val dictionaryRepository: DictionaryRepository,
    private val wordRepository: WordRepository,
    private val uploadPendingChanges: UploadPendingChangesUseCase,
    private val importedDictionaryService: ImportedDictionaryService,
) {

    /**
     * Downloads the user's dictionaries and, in one further request, every word they own — which is
     * what puts a count on each list row. Signed out there is nothing to reconcile against, which is a
     * success with nothing to do rather than an error.
     *
     * The dictionaries decide the outcome: without words the list still works, just without counts,
     * so a failed word pull is not worth turning the whole screen into an error. The vault refresh
     * at the end is judged the same way.
     *
     * Order is fixed: upload pending → download remote → refresh the vault.
     */
    suspend fun syncDictionaries(): Outcome<Unit> {
        val userId = activeUser() ?: return Outcome.Success(Unit)

        // Upload first, always: a download that ran before it could drop a row the server has not
        // been told about yet. This is also the only thing that drains changes made offline.
        uploadPendingChanges()

        val outcome = syncDictionariesUseCase(userId)
        if (outcome is Outcome.Success) wordRepository.pullAllWords(userId)

        // Last, and its failure is swallowed: the vault lives in a different service, and a Forum
        // that cannot be reached must not make the user's own library look broken. The downloaded
        // tables are never part of the upload above — they are someone else's content.
        importedDictionaryService.refreshFromVault()

        return outcome
    }

    /**
     * Downloads everything one dictionary is made of: its words, and its tags — which the service
     * keeps as a resource of their own, so nothing else brings them.
     *
     * Only for a dictionary the user owns. A download takes the imported path, which this decides
     * rather than its callers: three screens open a single dictionary, and each one getting the
     * question right separately is three chances to get it wrong.
     *
     * The words decide the outcome. A dictionary whose tags could not be read still shows its words,
     * which is the screen's reason for existing.
     */
    suspend fun syncDictionary(dictionaryId: String): Outcome<Unit> {
        activeUser() ?: return Outcome.Success(Unit)

        uploadPendingChanges()

        // A download is someone else's, and `words/dictionary/{id}` would write the owner's words
        // straight into the user's **own** word table — where the upload can see them, and where
        // they then appear a second time beside the downloaded copy. It is refreshed through the
        // path that owns it instead.
        if (importedDictionaryService.find(dictionaryId) != null) {
            return importedDictionaryService.refresh(dictionaryId)
        }

        return wordRepository.pullWords(dictionaryId).also { dictionaryRepository.pullTags(dictionaryId) }
    }
}
