package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.database.bibliotheca.BibliothecaDatabase
import de.coldtea.verborum.core.database.bibliotheca.WordEntity
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The durable [WordStore], backed by the `word` table. */
internal class DatabaseWordStore(
    private val database: BibliothecaDatabase,
) : WordStore {

    private val dao = database.wordDao

    override fun wordsOf(dictionaryId: String): Flow<List<Word>> =
        dao.observeWords(dictionaryId).map { rows -> rows.map(WordEntity::toWord) }

    /**
     * Never null, unlike the in-memory store: the table already holds whatever the last session left,
     * so an empty result really does mean "no words" rather than "nothing known yet".
     */
    override fun counts(): Flow<Map<String, Int>?> =
        dao.observeWordCounts().map { counts ->
            counts.associate { count -> count.dictionaryId to count.count }
        }

    override fun all(): Flow<List<Word>> =
        dao.observeAllWords().map { rows -> rows.map(WordEntity::toWord) }

    override suspend fun mergeDictionary(dictionaryId: String, remote: List<Word>) =
        database.withTransaction {
            val remoteIds = remote.map(Word::wordId).toSet()
            val pendingIds = dao.pendingUploadIdsIn(dictionaryId).toSet()
            val untouchable = dao.tombstonedIdsIn(dictionaryId).toSet() + pendingIds

            // Scoped to this dictionary: a details-screen pull must not touch the rest of the library.
            dao.deleteInDictionaryNotIn(dictionaryId, (remoteIds + pendingIds).toList())
            dao.upsertAll(remote.filterNot { it.wordId in untouchable }.map(Word::toEntity))
        }

    override suspend fun mergeAll(remote: List<Word>) = database.withTransaction {
        val remoteIds = remote.map(Word::wordId).toSet()
        val pendingIds = dao.pendingUploadIds().toSet()
        val untouchable = dao.tombstonedIds().toSet() + pendingIds

        dao.deleteNotIn((remoteIds + pendingIds).toList())
        dao.upsertAll(remote.filterNot { it.wordId in untouchable }.map(Word::toEntity))
    }

    override suspend fun markDeleted(wordId: String) = dao.markDeleted(wordId)

    override suspend fun markDictionaryDeleted(dictionaryId: String) =
        dao.markDictionaryDeleted(dictionaryId)

    override suspend fun remove(wordId: String) = dao.delete(wordId)

    override suspend fun removeDictionary(dictionaryId: String) =
        dao.deleteByDictionary(dictionaryId)

    override suspend fun clearTombstone(wordId: String) = dao.clearTombstone(wordId)

    override suspend fun find(wordId: String): Word? = dao.getWord(wordId)?.toWord()

    override suspend fun pendingUploads(): List<Word> = dao.pendingUploads().map(WordEntity::toWord)

    override suspend fun tombstoned(): List<Word> = dao.tombstoned().map(WordEntity::toWord)

    override suspend fun upsert(word: Word) = dao.upsert(word.toEntity())

    override suspend fun knownTimestamps(): Map<String, Pair<Long, Long>> =
        dao.timestamps().associate { it.id to (it.createdAt to it.updatedAt) }

    override suspend fun clear() = dao.clear()
}
