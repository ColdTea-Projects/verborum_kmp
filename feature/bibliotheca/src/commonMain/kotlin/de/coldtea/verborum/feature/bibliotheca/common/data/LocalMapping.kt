package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.database.bibliotheca.DictionaryEntity
import de.coldtea.verborum.core.database.bibliotheca.WordEntity
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.serialization.json.Json

/**
 * The boundary between the stored row and the domain model, kept here rather than in `core:database`
 * so that module stays free of the feature's types — and of kotlinx.serialization, which only the
 * tags column needs.
 */
private val tagsJson = Json { ignoreUnknownKeys = true }

/** Tags travel as a JSON array in one column, exactly as they do on Android. */
internal fun encodeTags(tags: List<String>): String = tagsJson.encodeToString(tags)

internal fun decodeTags(raw: String): List<String> =
    runCatching { tagsJson.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())

internal fun Dictionary.toEntity() = DictionaryEntity(
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
    tags = encodeTags(tags),
)

internal fun DictionaryEntity.toDictionary() = Dictionary(
    dictionaryId = dictionaryId,
    userId = userId,
    name = name,
    isPublic = isPublic,
    fromLang = fromLang,
    toLang = toLang,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = isSynced,
    isDeleted = isDeleted,
    tags = decodeTags(tags),
)

internal fun Word.toEntity() = WordEntity(
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

internal fun WordEntity.toWord() = Word(
    wordId = wordId,
    dictionaryId = dictionaryId,
    word = word,
    wordMeta = wordMeta,
    translation = translation,
    translationMeta = translationMeta,
    createdAt = createdAt,
    updatedAt = updatedAt,
    level = level,
    isSynced = isSynced,
    isDeleted = isDeleted,
)
