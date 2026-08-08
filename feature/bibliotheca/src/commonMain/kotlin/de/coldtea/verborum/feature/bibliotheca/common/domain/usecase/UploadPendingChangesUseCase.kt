package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository

/**
 * Pushes every local change the server has not seen and reconciles local state on success:
 * tombstoned rows are deleted remotely then dropped for good, unsent rows are uploaded then marked
 * synced. The counterpart to the app's writes, which no longer roll back when the backend is
 * unreachable — without this pass, a change made offline would sit on the device forever.
 *
 * **Runs before the download**, always. A merge protects unsent rows, but the ordering is what keeps
 * that from mattering: by the time the pull answers, everything local is already on the server.
 *
 * Best-effort by design, exactly as on Android: anything that fails keeps its pending state and is
 * tried again next time, so one bad row cannot block the rest. Nothing is reported, because this
 * runs underneath a screen that asked for a refresh, not for an upload.
 */
internal class UploadPendingChangesUseCase(
    private val dictionaryRepository: DictionaryRepository,
    private val wordRepository: WordRepository,
) {
    suspend operator fun invoke() {
        uploadDeletedDictionaries()
        uploadDeletedWords()

        dictionaryRepository.pendingUploads().forEach { dictionary ->
            dictionaryRepository.upload(dictionary)
        }

        wordRepository.pendingUploads().forEach { word -> wordRepository.upload(word) }
    }

    /** A dictionary takes its words with it, so both deletes have to land before the row goes. */
    private suspend fun uploadDeletedDictionaries() {
        dictionaryRepository.tombstoned().forEach { dictionary ->
            val id = dictionary.dictionaryId
            val wordsDeleted = wordRepository.deleteDictionaryWordsRemotely(id)
            val dictionaryDeleted = dictionaryRepository.deleteRemotely(id)

            if (wordsDeleted is Outcome.Success && dictionaryDeleted is Outcome.Success) {
                wordRepository.removeDictionaryLocally(id)
                dictionaryRepository.removeLocally(id)
            }
        }
    }

    private suspend fun uploadDeletedWords() {
        // Words of a dictionary that is itself tombstoned are handled above, as one delete.
        val deletedDictionaryIds = dictionaryRepository.tombstoned()
            .map { it.dictionaryId }
            .toSet()

        wordRepository.tombstoned()
            .filterNot { it.dictionaryId in deletedDictionaryIds }
            .forEach { word ->
                if (wordRepository.deleteRemotely(word.wordId) is Outcome.Success) {
                    wordRepository.removeLocally(word.wordId)
                }
            }
    }
}
