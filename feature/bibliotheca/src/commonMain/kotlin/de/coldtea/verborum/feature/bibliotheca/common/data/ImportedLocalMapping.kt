package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.database.bibliotheca.ImportedDictionaryEntity
import de.coldtea.verborum.core.database.bibliotheca.ImportedWordEntity
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedWord

/**
 * Between the stored row and the domain model, the same way [LocalMapping] does for the user's own
 * dictionaries — including the tag encoding, which the database keeps as opaque JSON so that
 * `core:database` needs no serialization dependency.
 */
internal fun ImportedDictionaryEntity.toDomain() = ImportedDictionary(
    dictionaryId = dictionaryId,
    publisherId = publisherId,
    publisherName = publisherName,
    name = name,
    fromLang = fromLang,
    toLang = toLang,
    tags = decodeTags(tags),
    importedAt = importedAt,
)

internal fun ImportedDictionary.toEntity() = ImportedDictionaryEntity(
    dictionaryId = dictionaryId,
    publisherId = publisherId,
    publisherName = publisherName,
    name = name,
    fromLang = fromLang,
    toLang = toLang,
    tags = encodeTags(tags),
    importedAt = importedAt,
)

internal fun ImportedWordEntity.toDomain() = ImportedWord(
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

internal fun ImportedWord.toEntity() = ImportedWordEntity(
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
