package de.coldtea.verborum.core.database.bibliotheca

import androidx.room.Room
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileProtectionCompleteUntilFirstUserAuthentication
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

private const val DATABASE_FILE = "db_verborum_bibliotheca"

/** The app's own directory, not `Documents` — this is state the app manages, not user files. */
private const val DATABASE_DIRECTORY = "de.coldtea.verborum"

actual fun createBibliothecaDatabase(): BibliothecaDatabase? =
    RoomBibliothecaDatabase(
        Room.databaseBuilder<BibliothecaRoomDatabase>(name = databaseFilePath())
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build(),
    )

/**
 * A database held only in memory, for tests that want the real SQLite behaviour — `IN ()`, upsert
 * conflicts, transactions — without a file to clean up afterwards.
 */
fun createInMemoryBibliothecaDatabase(): BibliothecaDatabase =
    RoomBibliothecaDatabase(
        Room.inMemoryDatabaseBuilder<BibliothecaRoomDatabase>()
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build(),
    )

/**
 * Adapts Room to the interface `commonMain` sees, so nothing outside this module names a Room type.
 */
private class RoomBibliothecaDatabase(
    private val room: BibliothecaRoomDatabase,
) : BibliothecaDatabase {

    override val dictionaryDao: DictionaryDao = RoomBackedDictionaryDao(room.dictionaryDao())

    override val wordDao: WordDao = RoomBackedWordDao(room.wordDao())

    override val importedDictionaryDao: ImportedDictionaryDao =
        RoomBackedImportedDictionaryDao(room.importedDictionaryDao(), room.importedWordDao())

    override val importedWordDao: ImportedWordDao =
        RoomBackedImportedWordDao(room.importedWordDao())

    override suspend fun <R> withTransaction(block: suspend () -> R): R =
        room.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }

    /**
     * Every table, downloaded dictionaries included: sign-out hands the device to whoever signs in
     * next, and someone else's dictionary left behind is as much a leak as the user's own.
     */
    override suspend fun clear() = withTransaction {
        room.wordDao().clear()
        room.dictionaryDao().clear()
        room.importedWordDao().clear()
        room.importedDictionaryDao().clear()
    }
}

private class RoomBackedDictionaryDao(private val dao: RoomDictionaryDao) : DictionaryDao {

    override fun observeDictionaries(): Flow<List<DictionaryEntity>> =
        dao.observeDictionaries().map { rows -> rows.map(RoomDictionary::toEntity) }

    override fun observeDictionary(dictionaryId: String): Flow<DictionaryEntity?> =
        dao.observeDictionary(dictionaryId).map { row -> row?.toEntity() }

    override suspend fun getDictionary(dictionaryId: String): DictionaryEntity? =
        dao.getDictionary(dictionaryId)?.toEntity()

    override suspend fun timestamps(): List<RowTimestamps> =
        dao.timestamps().map(RoomRowTimestamps::toEntity)

    override suspend fun tombstonedIds(): List<String> = dao.tombstonedIds()

    override suspend fun pendingUploadIds(): List<String> = dao.pendingUploadIds()

    override suspend fun tombstoned(): List<DictionaryEntity> =
        dao.tombstoned().map(RoomDictionary::toEntity)

    override suspend fun pendingUploads(): List<DictionaryEntity> =
        dao.pendingUploads().map(RoomDictionary::toEntity)

    override suspend fun upsert(dictionary: DictionaryEntity) = dao.upsert(dictionary.toRoom())

    override suspend fun upsertAll(dictionaries: List<DictionaryEntity>) =
        dao.upsertAll(dictionaries.map(DictionaryEntity::toRoom))

    override suspend fun markDeleted(dictionaryId: String) = dao.markDeleted(dictionaryId)

    override suspend fun clearTombstone(dictionaryId: String) = dao.clearTombstone(dictionaryId)

    override suspend fun delete(dictionaryId: String) = dao.delete(dictionaryId)

    override suspend fun deleteNotIn(keepIds: List<String>) = dao.deleteNotIn(keepIds)

    override suspend fun clear() = dao.clear()
}

private class RoomBackedWordDao(private val dao: RoomWordDao) : WordDao {

    override fun observeWords(dictionaryId: String): Flow<List<WordEntity>> =
        dao.observeWords(dictionaryId).map { rows -> rows.map(RoomWord::toEntity) }

    override fun observeAllWords(): Flow<List<WordEntity>> =
        dao.observeAllWords().map { rows -> rows.map(RoomWord::toEntity) }

    override fun observeWordCounts(): Flow<List<DictionaryWordCount>> =
        dao.observeWordCounts().map { rows -> rows.map(RoomDictionaryWordCount::toEntity) }

    override suspend fun getWord(wordId: String): WordEntity? = dao.getWord(wordId)?.toEntity()

    override suspend fun timestamps(): List<RowTimestamps> =
        dao.timestamps().map(RoomRowTimestamps::toEntity)

    override suspend fun tombstonedIds(): List<String> = dao.tombstonedIds()

    override suspend fun tombstonedIdsIn(dictionaryId: String): List<String> =
        dao.tombstonedIdsIn(dictionaryId)

    override suspend fun pendingUploadIds(): List<String> = dao.pendingUploadIds()

    override suspend fun pendingUploadIdsIn(dictionaryId: String): List<String> =
        dao.pendingUploadIdsIn(dictionaryId)

    override suspend fun tombstoned(): List<WordEntity> = dao.tombstoned().map(RoomWord::toEntity)

    override suspend fun pendingUploads(): List<WordEntity> =
        dao.pendingUploads().map(RoomWord::toEntity)

    override suspend fun upsert(word: WordEntity) = dao.upsert(word.toRoom())

    override suspend fun upsertAll(words: List<WordEntity>) =
        dao.upsertAll(words.map(WordEntity::toRoom))

    override suspend fun markDeleted(wordId: String) = dao.markDeleted(wordId)

    override suspend fun markDictionaryDeleted(dictionaryId: String) =
        dao.markDictionaryDeleted(dictionaryId)

    override suspend fun clearTombstone(wordId: String) = dao.clearTombstone(wordId)

    override suspend fun clearDictionaryTombstones(dictionaryId: String) =
        dao.clearDictionaryTombstones(dictionaryId)

    override suspend fun delete(wordId: String) = dao.delete(wordId)

    override suspend fun deleteByDictionary(dictionaryId: String) =
        dao.deleteByDictionary(dictionaryId)

    override suspend fun deleteNotIn(keepIds: List<String>) = dao.deleteNotIn(keepIds)

    override suspend fun deleteInDictionaryNotIn(dictionaryId: String, keepIds: List<String>) =
        dao.deleteInDictionaryNotIn(dictionaryId, keepIds)

    override suspend fun clear() = dao.clear()
}

/**
 * Deleting a downloaded dictionary takes its words with it.
 *
 * By hand rather than with a foreign key: the Android schema declares none, and adding one here
 * would make the two clients' tables differ in a way that matters the next time either is migrated.
 * The word delete goes first so a failure between the two leaves orphaned words — invisible, because
 * every read joins through the dictionary — rather than a dictionary whose words are gone.
 */
private class RoomBackedImportedDictionaryDao(
    private val dao: RoomImportedDictionaryDao,
    private val words: RoomImportedWordDao,
) : ImportedDictionaryDao {

    override fun observeDictionaries(): Flow<List<ImportedDictionaryEntity>> =
        dao.observeDictionaries().map { rows -> rows.map(RoomImportedDictionary::toEntity) }

    override fun observeDictionary(dictionaryId: String): Flow<ImportedDictionaryEntity?> =
        dao.observeDictionary(dictionaryId).map { row -> row?.toEntity() }

    override fun observeDictionaryIds(): Flow<List<String>> = dao.observeDictionaryIds()

    override suspend fun getDictionary(dictionaryId: String): ImportedDictionaryEntity? =
        dao.getDictionary(dictionaryId)?.toEntity()

    override suspend fun dictionaries(): List<ImportedDictionaryEntity> =
        dao.dictionaries().map(RoomImportedDictionary::toEntity)

    override suspend fun upsert(dictionary: ImportedDictionaryEntity) =
        dao.upsert(dictionary.toRoom())

    override suspend fun delete(dictionaryId: String) {
        words.deleteByDictionary(dictionaryId)
        dao.delete(dictionaryId)
    }

    override suspend fun clear() = dao.clear()
}

private class RoomBackedImportedWordDao(
    private val dao: RoomImportedWordDao,
) : ImportedWordDao {

    override fun observeWords(dictionaryId: String): Flow<List<ImportedWordEntity>> =
        dao.observeWords(dictionaryId).map { rows -> rows.map(RoomImportedWord::toEntity) }

    override fun observeWordsInLanguagePair(
        fromLang: String,
        toLang: String,
    ): Flow<List<ImportedWordEntity>> =
        dao.observeWordsInLanguagePair(fromLang, toLang)
            .map { rows -> rows.map(RoomImportedWord::toEntity) }

    override fun observeWordCounts(): Flow<List<DictionaryWordCount>> =
        dao.observeWordCounts().map { rows -> rows.map(RoomDictionaryWordCount::toEntity) }

    override suspend fun getWord(wordId: String): ImportedWordEntity? =
        dao.getWord(wordId)?.toEntity()

    override suspend fun words(dictionaryId: String): List<ImportedWordEntity> =
        dao.words(dictionaryId).map(RoomImportedWord::toEntity)

    override suspend fun upsertAll(words: List<ImportedWordEntity>) =
        dao.upsertAll(words.map(ImportedWordEntity::toRoom))

    override suspend fun updateLevel(wordId: String, level: Int) = dao.updateLevel(wordId, level)

    override suspend fun deleteByDictionary(dictionaryId: String) =
        dao.deleteByDictionary(dictionaryId)

    override suspend fun clear() = dao.clear()
}

/**
 * The database file, in a directory created on first use.
 *
 * Two protections are applied there, matching how the Keychain holds the tokens: the contents stay
 * unreadable until the device has been unlocked once, and the directory is kept out of backups —
 * the rows are a local mirror of what the server holds, so a restore has nothing to lose.
 */
@OptIn(ExperimentalForeignApi::class)
private fun databaseFilePath(): String {
    val root = NSSearchPathForDirectoriesInDomains(
        directory = NSApplicationSupportDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    ).firstOrNull() as? String ?: NSTemporaryDirectory()

    val directory = "$root/$DATABASE_DIRECTORY"

    NSFileManager.defaultManager.createDirectoryAtPath(
        path = directory,
        withIntermediateDirectories = true,
        attributes = mapOf(NSFileProtectionKey to NSFileProtectionCompleteUntilFirstUserAuthentication),
        error = null,
    )
    NSURL.fileURLWithPath(directory).setResourceValue(
        value = true,
        forKey = NSURLIsExcludedFromBackupKey,
        error = null,
    )

    return "$directory/$DATABASE_FILE"
}
