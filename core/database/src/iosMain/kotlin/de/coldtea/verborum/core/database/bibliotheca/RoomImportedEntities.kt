package de.coldtea.verborum.core.database.bibliotheca

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * The Room row behind [ImportedDictionaryEntity]. Table and column names copied from the Android app
 * so both clients describe a downloaded dictionary identically.
 */
@Entity(tableName = "imported_dictionary", primaryKeys = ["dictionary_id"])
internal data class RoomImportedDictionary(
    @ColumnInfo(name = "dictionary_id")
    val dictionaryId: String,
    @ColumnInfo(name = "publisher_id")
    val publisherId: String,
    @ColumnInfo(name = "publisher_name")
    val publisherName: String?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "from_lang")
    val fromLang: String,
    @ColumnInfo(name = "to_lang")
    val toLang: String,
    @ColumnInfo(name = "tags")
    val tags: String,
    @ColumnInfo(name = "imported_at")
    val importedAt: Long,
)

/**
 * The Room row behind [ImportedWordEntity].
 *
 * Indexed on the dictionary key for the same reason [RoomWord] is: every read of these words is
 * scoped to one dictionary or one language pair, and without it each one is a full table scan.
 */
@Entity(
    tableName = "imported_word",
    primaryKeys = ["word_id"],
    indices = [Index(value = ["fk_dictionary_id"])],
)
internal data class RoomImportedWord(
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
    @ColumnInfo(name = "level")
    val level: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

internal fun RoomImportedDictionary.toEntity() = ImportedDictionaryEntity(
    dictionaryId = dictionaryId,
    publisherId = publisherId,
    publisherName = publisherName,
    name = name,
    fromLang = fromLang,
    toLang = toLang,
    tags = tags,
    importedAt = importedAt,
)

internal fun ImportedDictionaryEntity.toRoom() = RoomImportedDictionary(
    dictionaryId = dictionaryId,
    publisherId = publisherId,
    publisherName = publisherName,
    name = name,
    fromLang = fromLang,
    toLang = toLang,
    tags = tags,
    importedAt = importedAt,
)

internal fun RoomImportedWord.toEntity() = ImportedWordEntity(
    wordId = wordId,
    dictionaryId = dictionaryId,
    word = word,
    wordMeta = wordMeta,
    translation = translation,
    translationMeta = translationMeta,
    level = level,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun ImportedWordEntity.toRoom() = RoomImportedWord(
    wordId = wordId,
    dictionaryId = dictionaryId,
    word = word,
    wordMeta = wordMeta,
    translation = translation,
    translationMeta = translationMeta,
    level = level,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
