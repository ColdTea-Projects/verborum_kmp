package de.coldtea.verborum.feature.forum.data

import de.coldtea.verborum.core.common.ApiTimestamp
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.core.network.asPathSegment
import de.coldtea.verborum.core.network.plainApiCall
import de.coldtea.verborum.feature.forum.domain.SharedDictionary
import de.coldtea.verborum.feature.forum.domain.SharedWord
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The read-only slice of `ms_dictionary` the Forum needs.
 *
 * The marketplace answers what is *on offer*; what a dictionary actually contains comes from the
 * dictionary service, which since 2026-09-27 lets anyone signed in read a public one. Every write
 * is a 403, which is why this class has no write.
 *
 * A separate class from the library's `DictionaryApi` rather than a shared one: a feature may not
 * depend on a feature, and promoting the library's version to `core` would carry its create, update
 * and delete along with it — into a module whose whole point is that it cannot write.
 */
internal class SharedDictionaryApi(private val client: HttpClient) {

    /** 404 once the owner makes it private, deletes it, or leaves the Forum. */
    suspend fun dictionary(dictionaryId: String): Outcome<SharedDictionary> =
        plainApiCall<SharedDictionaryResponse> {
            client.get("dictionaries/dictionary/${dictionaryId.asPathSegment()}")
        }.map(SharedDictionaryResponse::toDictionary)

    /** Unreadable ids are silently dropped from the answer rather than failing it. */
    suspend fun dictionaries(dictionaryIds: List<String>): Outcome<List<SharedDictionary>> =
        plainApiCall<List<SharedDictionaryResponse>> {
            client.get("dictionaries/batch") { parameter("ids", dictionaryIds.joinToString(",")) }
        }.map { responses -> responses.map(SharedDictionaryResponse::toDictionary) }

    suspend fun tags(dictionaryId: String): Outcome<List<String>> =
        plainApiCall<List<SharedDictionaryTagResponse>> {
            client.get("dictionaries/${dictionaryId.asPathSegment()}/tags")
        }.map { tags ->
            tags.mapNotNull { tag -> tag.tag?.trim()?.lowercase()?.takeIf(String::isNotEmpty) }
                .distinct()
        }

    /** Answers `[]` rather than 404 for a dictionary that has become unreadable. */
    suspend fun words(dictionaryId: String): Outcome<List<SharedWord>> =
        plainApiCall<List<SharedWordResponse>> {
            client.get("words/dictionary/${dictionaryId.asPathSegment()}")
        }.map { words -> words.mapNotNull(SharedWordResponse::toWord) }
}

@Serializable
internal data class SharedDictionaryResponse(
    @SerialName("dictionaryId") val dictionaryId: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("fromLang") val fromLang: String? = null,
    @SerialName("toLang") val toLang: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
) {
    fun toDictionary() = SharedDictionary(
        dictionaryId = dictionaryId.orEmpty(),
        publisherId = userId.orEmpty(),
        name = name.orEmpty(),
        fromLang = fromLang.orEmpty().uppercase(),
        toLang = toLang.orEmpty().uppercase(),
        createdAt = ApiTimestamp.parse(createdAt) ?: 0L,
    )
}

@Serializable
internal data class SharedDictionaryTagResponse(@SerialName("tag") val tag: String? = null)

/**
 * Someone else's word. `level` is deliberately absent from this shape: on another user's word the
 * server always sends null, because the value is the *owner's* progress — so there is nothing here
 * worth decoding, and a field would only invite someone to read it.
 */
@Serializable
internal data class SharedWordResponse(
    @SerialName("wordId") val wordId: String? = null,
    @SerialName("word") val word: String? = null,
    @SerialName("wordMeta") val wordMeta: String? = null,
    @SerialName("translation") val translation: String? = null,
    @SerialName("translationMeta") val translationMeta: String? = null,
) {
    fun toWord(): SharedWord? {
        val id = wordId?.takeIf(String::isNotBlank) ?: return null

        return SharedWord(
            wordId = id,
            word = word.orEmpty(),
            wordMeta = wordMeta.orEmpty(),
            translation = translation.orEmpty(),
            translationMeta = translationMeta.orEmpty(),
        )
    }
}
