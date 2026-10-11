package de.coldtea.verborum.core.network

import de.coldtea.verborum.core.common.VerborumError
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The error body every backend service answers a non-2xx with (marketplace guide §7). Unlike the
 * app's `Envelope`, it is the whole body rather than a wrapper, which is why the plain and
 * status-only calls read it separately.
 *
 * [error] is the server's exception class name — `SharingRequiredException`,
 * `InvalidProfileException`, `RecordNotFoundException` — and is what a caller branches on *after*
 * the status code has already decided the shape of the failure. [errorDetail] is a sentence written
 * for people, so it is the one piece of server text a screen may show verbatim.
 */
@Serializable
internal data class ServiceErrorDto(
    @SerialName("error") val error: String? = null,
    @SerialName("errorDetail") val errorDetail: String? = null,
)

/**
 * Reads the service error body off a response whose status has already failed, so the backend's own
 * code and sentence survive into [VerborumError.Http] instead of being thrown away.
 *
 * Why the decode failure is discarded: this is only ever called where the status already settles the
 * outcome. A body that will not parse — a proxy's HTML page, an empty gateway answer — must still
 * arrive as `Http`, carrying nothing extra rather than reading as a malformed payload.
 *
 * `@PublishedApi internal` because the calls that use it are inline.
 */
@PublishedApi
internal suspend fun HttpResponse.serviceErrorOrNull(): ServiceErrorDto? = try {
    body<ServiceErrorDto>()
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    null
}

/** The shared mapping from a failed response onto the app's error model. */
@PublishedApi
internal suspend fun HttpResponse.toHttpFailure(): VerborumError.Http {
    val body = serviceErrorOrNull()

    return VerborumError.Http(
        status = status.value,
        code = body?.error,
        message = body?.errorDetail,
    )
}
