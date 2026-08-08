package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.database.bibliotheca.BibliothecaDatabase
import de.coldtea.verborum.core.database.bibliotheca.DictionaryEntity
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The durable [DictionaryStore]: rows live in the `dictionary` table and outlive the process, which
 * is the whole point of the local database.
 *
 * No mutex here, unlike [InMemoryDictionaryStore] — [merge] is the only read-then-write, and it runs
 * inside a database transaction, which is what serialises it.
 */
internal class DatabaseDictionaryStore(
    private val database: BibliothecaDatabase,
) : DictionaryStore {

    private val dao = database.dictionaryDao

    override val dictionaries: Flow<List<Dictionary>> =
        dao.observeDictionaries().map { rows -> rows.map(DictionaryEntity::toDictionary) }

    override suspend fun merge(remote: List<Dictionary>) = database.withTransaction {
        val remoteIds = remote.map(Dictionary::dictionaryId).toSet()
        val pendingIds = dao.pendingUploadIds().toSet()
        val tombstonedIds = dao.tombstonedIds().toSet()

        // Rows the server still has stay; rows it has forgotten go, which is how a tombstone's
        // delete is finally confirmed. Anything not yet uploaded is held back from both.
        dao.deleteNotIn((remoteIds + pendingIds).toList())

        // A tombstoned row must not be overwritten by the copy the server still holds, or the
        // delete would undo itself on the next sync.
        val untouchable = tombstonedIds + pendingIds

        dao.upsertAll(
            remote.filterNot { it.dictionaryId in untouchable }.map(Dictionary::toEntity),
        )
    }

    override suspend fun markDeleted(dictionaryId: String) = dao.markDeleted(dictionaryId)

    override suspend fun remove(dictionaryId: String) = dao.delete(dictionaryId)

    override suspend fun clearTombstone(dictionaryId: String) = dao.clearTombstone(dictionaryId)

    override suspend fun find(dictionaryId: String): Dictionary? =
        dao.getDictionary(dictionaryId)?.toDictionary()

    override suspend fun pendingUploads(): List<Dictionary> =
        dao.pendingUploads().map(DictionaryEntity::toDictionary)

    override suspend fun tombstoned(): List<Dictionary> =
        dao.tombstoned().map(DictionaryEntity::toDictionary)

    override suspend fun upsert(dictionary: Dictionary) = dao.upsert(dictionary.toEntity())

    override suspend fun knownTimestamps(): Map<String, Pair<Long, Long>> =
        dao.timestamps().associate { it.id to (it.createdAt to it.updatedAt) }

    override suspend fun clear() = dao.clear()
}
