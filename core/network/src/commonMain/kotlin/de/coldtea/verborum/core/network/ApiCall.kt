package de.coldtea.verborum.core.network

import de.coldtea.verborum.core.common.Envelope
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.common.toOutcome
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
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

    when (status) {
        401 -> Outcome.Failure(VerborumError.Unauthorized)
        else -> response.body<Envelope<T>>().toOutcome(status)
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
        else -> Outcome.Failure(VerborumError.Http(status))
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
        else -> Outcome.Failure(VerborumError.Http(status))
    }.logFailure(response.call.request.url.encodedPath)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Outcome.Failure(throwable.toVerborumError()).logFailure()
}

/** Maps a transport-level throwable onto the shared error model. */
fun Throwable.toVerborumError(): VerborumError = when (this) {
    is SerializationException -> VerborumError.Serialization(message)
    else -> VerborumError.Network(message ?: this::class.simpleName)
}
