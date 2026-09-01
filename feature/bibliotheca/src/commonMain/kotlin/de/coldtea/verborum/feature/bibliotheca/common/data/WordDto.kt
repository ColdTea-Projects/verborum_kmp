package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.ApiTimestamp
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** The word service's own shape, mapped to [Word] before it reaches the domain. */
@Serializable
internal data class WordDto(
    @SerialName("wordId") val wordId: String? = null,
    @SerialName("dictionaryId") val dictionaryId: String? = null,
    @SerialName("word") val word: String? = null,
    @SerialName("wordMeta") val wordMeta: String? = null,
    @SerialName("translation") val translation: String? = null,
    @SerialName("translationMeta") val translationMeta: String? = null,
    /**
     * Captured as raw JSON rather than an `Int` so a malformed value — a string, a float, null — can
     * never fail the whole word's deserialisation. It is validated in [toWord] instead.
     */
    @SerialName("level") val level: JsonElement? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
) {
    /** The level the server sent, but only when it is a whole number the app could have written. */
    private val storedLevel: Int?
        get() = (level as? JsonPrimitive)?.intOrNull?.takeIf { it in 0..Word.MAX_LEVEL }

    /**
     * Whether the server said nothing at all about the level — the field absent, or an explicit
     * null. A different thing from having sent a value this app cannot use, and treated differently.
     */
    private val levelIsAbsent: Boolean
        get() = level == null || level is JsonNull

    /**
     * Server timestamps win when present; the fallbacks cover a backend that omits them.
     *
     * A level the server sent but this app could not have written is reset to 0 **and** the row is
     * marked unsynced, so the correction is pushed back and the bad value is healed rather than read
     * wrong forever.
     *
     * A level the server never sent is left synced. Silence is not a claim about the word, and
     * reading it as one would mark every pulled row unsynced and have the very next sync upload the
     * whole library with `level = 0` — destroying whatever progress the server actually holds.
     */
    fun toWord(
        dictionaryId: String,
        fallbackCreatedAt: Long,
        fallbackUpdatedAt: Long,
    ) = Word(
        wordId = wordId.orEmpty(),
        dictionaryId = this.dictionaryId ?: dictionaryId,
        word = word.orEmpty(),
        wordMeta = wordMeta.orEmpty(),
        translation = translation.orEmpty(),
        translationMeta = translationMeta.orEmpty(),
        createdAt = ApiTimestamp.parse(createdAt) ?: fallbackCreatedAt,
        updatedAt = ApiTimestamp.parse(updatedAt) ?: fallbackUpdatedAt,
        level = storedLevel ?: 0,
        isSynced = levelIsAbsent || storedLevel != null,
    )
}

/** The update payload: the service takes words grouped by dictionary. */
@Serializable
internal data class WordBundleRequest(
    @SerialName("dictionaryId") val dictionaryId: String,
    @SerialName("words") val words: List<WordRequest>,
)

@Serializable
internal data class WordRequest(
    @SerialName("wordId") val wordId: String,
    @SerialName("dictionaryId") val dictionaryId: String,
    @SerialName("word") val word: String,
    @SerialName("wordMeta") val wordMeta: String,
    @SerialName("translation") val translation: String,
    @SerialName("translationMeta") val translationMeta: String,
    /** Practice progress, 0..7 — synced so it survives a reinstall and follows the user. */
    @SerialName("level") val level: Int,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

internal fun Word.toRequest() = WordRequest(
    wordId = wordId,
    dictionaryId = dictionaryId,
    word = word,
    wordMeta = wordMeta,
    translation = translation,
    translationMeta = translationMeta,
    level = level.coerceIn(0, Word.MAX_LEVEL),
    createdAt = ApiTimestamp.format(createdAt),
    updatedAt = ApiTimestamp.format(updatedAt),
)
