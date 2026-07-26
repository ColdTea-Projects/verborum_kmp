package de.coldtea.verborum.core.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The envelope every backend endpoint wraps its payload in (integration §3).
 * Exactly one of [data] / [error] is populated.
 */
@Serializable
data class Envelope<T>(
    @SerialName("data") val data: T? = null,
    @SerialName("error") val error: ErrorDto? = null,
    @SerialName("meta") val meta: MetaDto? = null,
)

@Serializable
data class ErrorDto(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String? = null,
    @SerialName("details") val details: Map<String, String> = emptyMap(),
)

@Serializable
data class MetaDto(
    @SerialName("page") val page: Int? = null,
    @SerialName("pageSize") val pageSize: Int? = null,
    @SerialName("total") val total: Int? = null,
)

/** Unwraps an envelope into an [Outcome], turning a populated `error` into a failure. */
fun <T> Envelope<T>.toOutcome(status: Int): Outcome<T> = when {
    error != null -> Outcome.Failure(VerborumError.Http(status, error.code, error.message))
    data != null -> Outcome.Success(data)
    else -> Outcome.Failure(VerborumError.Serialization("Envelope carried neither data nor error"))
}
