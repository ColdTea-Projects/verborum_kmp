package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.ApiTimestamp
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The dictionary service's own shape. Kept in the data layer and mapped before it reaches the
 * domain, so a wire change never propagates into the UI.
 */
@Serializable
internal data class DictionaryDto(
    @SerialName("dictionaryId") val dictionaryId: String,
    @SerialName("userId") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("isPublic") val isPublic: Boolean = false,
    @SerialName("fromLang") val fromLang: String = "",
    @SerialName("toLang") val toLang: String = "",
    /**
     * Server-owned, written by Hibernate's `@CreationTimestamp` / `@UpdateTimestamp` and serialised
     * as ISO-8601 in UTC. Nullable so a backend that does not expose them still deserialises.
     */
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
) {
    /**
     * The server's timestamps win when present — that is what makes a creation date survive a
     * reinstall instead of every row reading "just now". The fallbacks cover a backend that omits
     * them: the value already held locally, else the current time.
     */
    fun toDictionary(fallbackCreatedAt: Long, fallbackUpdatedAt: Long) = Dictionary(
        dictionaryId = dictionaryId,
        userId = userId,
        name = name,
        isPublic = isPublic,
        fromLang = fromLang,
        toLang = toLang,
        createdAt = ApiTimestamp.parse(createdAt) ?: fallbackCreatedAt,
        updatedAt = ApiTimestamp.parse(updatedAt) ?: fallbackUpdatedAt,
        isSynced = true,
    )
}

/**
 * One tag of one dictionary, as its own resource.
 *
 * The service keeps tags beside the dictionary rather than inside it, which is why they are absent
 * from [DictionaryDto]: only [tag] matters here, the rest being the row's own bookkeeping.
 */
@Serializable
internal data class DictionaryTagDto(
    @SerialName("tagId") val tagId: String? = null,
    @SerialName("dictionaryId") val dictionaryId: String? = null,
    @SerialName("tag") val tag: String,
)

/** Adding one tag; the service takes them one at a time and removes them by name. */
@Serializable
internal data class DictionaryTagRequest(@SerialName("tag") val tag: String)

/** The create/update payload. Tags are their own sub-resource, so they are absent here. */
@Serializable
internal data class DictionaryRequest(
    @SerialName("dictionaryId") val dictionaryId: String,
    @SerialName("userId") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("isPublic") val isPublic: Boolean,
    @SerialName("fromLang") val fromLang: String,
    @SerialName("toLang") val toLang: String,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

internal fun Dictionary.toRequest() = DictionaryRequest(
    dictionaryId = dictionaryId,
    userId = userId,
    name = name,
    isPublic = isPublic,
    fromLang = fromLang,
    toLang = toLang,
    createdAt = ApiTimestamp.format(createdAt),
    updatedAt = ApiTimestamp.format(updatedAt),
)
