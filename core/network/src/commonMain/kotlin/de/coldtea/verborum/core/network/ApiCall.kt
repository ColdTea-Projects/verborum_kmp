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
    }
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (serialization: SerializationException) {
    Outcome.Failure(VerborumError.Serialization(serialization.message))
} catch (throwable: Throwable) {
    Outcome.Failure(throwable.toVerborumError())
}

/** Maps a transport-level throwable onto the shared error model. */
fun Throwable.toVerborumError(): VerborumError = when (this) {
    is SerializationException -> VerborumError.Serialization(message)
    else -> VerborumError.Network(message ?: this::class.simpleName)
}
