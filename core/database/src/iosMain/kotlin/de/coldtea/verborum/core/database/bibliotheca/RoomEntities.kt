package de.coldtea.verborum.core.database.bibliotheca

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * The Room row behind [DictionaryEntity]. Table and column names are copied from the Android app so
 * both clients describe a dictionary identically; the two classes are separate only because Room's
 * annotations cannot reach `commonMain`.
 */
@Entity(tableName = "dictionary", primaryKeys = ["dictionary_id"])
internal data class RoomDictionary(
    @ColumnInfo(name = "dictionary_id")
    val dictionaryId: String,
    @ColumnInfo(name = "fk_user_id")
    val userId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "is_public")
    val isPublic: Boolean,
    @ColumnInfo(name = "isSynced")
    val isSynced: Boolean,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean,
    @ColumnInfo(name = "from_lang")
    val fromLang: String,
    @ColumnInfo(name = "to_lang")
    val toLang: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "tags")
    val tags: String,
)

/**
 * The Room row behind [WordEntity].
 *
 * The index on the dictionary foreign key is the one deliberate addition to the Android schema: every
 * word read the app makes is scoped to a dictionary, and without it each one is a full table scan.
 */
@Entity(
    tableName = "word",
    primaryKeys = ["word_id"],
    indices = [Index(value = ["fk_dictionary_id"])],
)
internal data class RoomWord(
    @ColumnInfo(name = "word_id")
    val wordId: String,
    @ColumnInfo(name = "fk_dictionary_id")
    val dictionaryId: String,
    @ColumnInfo(name = "word")
    val word: String,
    @ColumnInfo(name = "word_meta")
    val wordMeta: String,
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "translation_meta")
    val translationMeta: String,
    @ColumnInfo(name = "isSynced")
    val isSynced: Boolean,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean,
    @ColumnInfo(name = "level")
    val level: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

/** The projection [WordDao.observeWordCounts] answers with. */
internal data class RoomDictionaryWordCount(
    @ColumnInfo(name = "fk_dictionary_id")
    val dictionaryId: String,
    @ColumnInfo(name = "word_count")
    val count: Int,
)

/** The projection behind [RowTimestamps]; the queries alias their columns to these names. */
internal data class RoomRowTimestamps(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
)

internal fun RoomDictionary.toEntity() = DictionaryEntity(
    dictionaryId = dictionaryId,
    userId = userId,
    name = name,
    isPublic = isPublic,
    isSynced = isSynced,
    isDeleted = isDeleted,
    fromLang = fromLang,
    toLang = toLang,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags,
)

internal fun DictionaryEntity.toRoom() = RoomDictionary(
    dictionaryId = dictionaryId,
    userId = userId,
    name = name,
    isPublic = isPublic,
    isSynced = isSynced,
    isDeleted = isDeleted,
    fromLang = fromLang,
    toLang = toLang,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags,
)

internal fun RoomWord.toEntity() = WordEntity(
    wordId = wordId,
    dictionaryId = dictionaryId,
    word = word,
    wordMeta = wordMeta,
    translation = translation,
    translationMeta = translationMeta,
    isSynced = isSynced,
    isDeleted = isDeleted,
    level = level,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun WordEntity.toRoom() = RoomWord(
    wordId = wordId,
    dictionaryId = dictionaryId,
    word = word,
    wordMeta = wordMeta,
    translation = translation,
    translationMeta = translationMeta,
    isSynced = isSynced,
    isDeleted = isDeleted,
    level = level,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun RoomDictionaryWordCount.toEntity() =
    DictionaryWordCount(dictionaryId = dictionaryId, count = count)

internal fun RoomRowTimestamps.toEntity() =
    RowTimestamps(id = id, createdAt = createdAt, updatedAt = updatedAt)
