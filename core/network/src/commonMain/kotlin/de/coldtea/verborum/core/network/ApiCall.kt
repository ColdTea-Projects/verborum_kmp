package de.coldtea.verborum.core.network

import de.coldtea.verborum.core.common.Envelope
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.common.toOutcome
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Runs a request and folds every outcome — success, HTTP error, transport
 * failure, decode failure — into a single [Outcome]. Repositories call this so
 * no Ktor exception ever escapes `core:network`.
 */
suspend inline fun <reified T> apiCall(
    crossinline request: suspend () -> HttpResponse,
): Outcome<T> = try {
    val response = request()
    val status = response.status.value

    when {
        status == 401 -> Outcome.Failure(VerborumError.Unauthorized)
        status in 200..299 -> response.body<Envelope<T>>().toOutcome(status)

        // A failing status is read for the backend's own error code, but never for its verdict: the
        // status already decided that, and a body that will not decode at all — a proxy's HTML page,
        // an empty gateway response — must still arrive as `Http`. That is what a caller branches on
        // to tell a retryable 5xx from a refusal it should stop repeating; as a `Serialization` it
        // would look like a permanently malformed payload instead.
        else -> {
            val error = response.envelopeOrNull<T>()?.error
            Outcome.Failure(VerborumError.Http(status, error?.code, error?.message))
        }
    }.logFailure(response.call.request.url.encodedPath)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (serialization: SerializationException) {
    Outcome.Failure(VerborumError.Serialization(serialization.message)).logFailure()
} catch (throwable: Throwable) {
    Outcome.Failure(throwable.toVerborumError()).logFailure()
}

/**
 * The same folding as [apiCall] for endpoints that answer with the payload directly instead of an
 * [Envelope]. The dictionary service does exactly that — `GET dictionaries/{userId}` returns a bare
 * JSON array — and the Android client reads it the same way.
 */
suspend inline fun <reified T> plainApiCall(
    crossinline request: suspend () -> HttpResponse,
): Outcome<T> = try {
    val response = request()
    val status = response.status.value

    when {
        status == 401 -> Outcome.Failure(VerborumError.Unauthorized)
        status in 200..299 -> Outcome.Success(response.body<T>())

        // The services that answer with the payload directly answer a failure with their own error
        // body rather than an `Envelope`, and callers branch on the class name in it: the sharing
        // rule and the profile rules are both "400 plus which rule". Dropping it would leave them
        // unable to tell one 400 from another.
        else -> Outcome.Failure(response.toHttpFailure())
    }.logFailure(response.call.request.url.encodedPath)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (serialization: SerializationException) {
    Outcome.Failure(VerborumError.Serialization(serialization.message)).logFailure()
} catch (throwable: Throwable) {
    Outcome.Failure(throwable.toVerborumError()).logFailure()
}

/**
 * For endpoints whose answer is the status code — a create, update or delete that returns no body.
 * Nothing is decoded, so an empty 204 is a success rather than a deserialisation failure.
 */
suspend inline fun statusApiCall(
    crossinline request: suspend () -> HttpResponse,
): Outcome<Unit> = try {
    val response = request()
    val status = response.status.value

    when {
        status == 401 -> Outcome.Failure(VerborumError.Unauthorized)
        status in 200..299 -> Outcome.Success(Unit)
        else -> Outcome.Failure(response.toHttpFailure())
    }.logFailure(response.call.request.url.encodedPath)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Outcome.Failure(throwable.toVerborumError()).logFailure()
}

/**
 * The body as an [Envelope], or null when it is not one — a proxy's HTML error page, a plain-text
 * gateway message, an empty body.
 *
 * `@PublishedApi internal` because [apiCall] is inline. Why the body would not decode is deliberately
 * discarded: this is only ever called where a status already settles the outcome, so the reason adds
 * nothing that would reach a caller.
 */
@PublishedApi
internal suspend inline fun <reified T> HttpResponse.envelopeOrNull(): Envelope<T>? = try {
    body<Envelope<T>>()
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    null
}

/** Maps a transport-level throwable onto the shared error model. */
fun Throwable.toVerborumError(): VerborumError = when (this) {
    is SerializationException -> VerborumError.Serialization(message)
    // Ktor wraps a decode failure in its own conversion exception rather than letting the
    // kotlinx one through, so without this a malformed payload would read as a transport fault.
    is ContentConvertException -> VerborumError.Serialization(message)
    else -> VerborumError.Network(message ?: this::class.simpleName)
}
