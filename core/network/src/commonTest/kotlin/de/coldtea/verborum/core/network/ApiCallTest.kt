package de.coldtea.verborum.core.network

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers how [apiCall] folds a response into an [Outcome] — the mapping features read, since
 * `VerborumError.Http` is what a caller branches on to tell a retryable 5xx from a refusal, and
 * `Serialization` is what tells it the payload itself is wrong.
 *
 * Driven through a real Ktor client on `MockEngine` rather than a hand-written fake: content
 * negotiation is the thing that decides whether a body is an envelope at all, so stubbing it out
 * would skip the part being tested.
 */
class ApiCallTest {

    @Serializable
    private data class Payload(val name: String)

    @Test
    fun `an enveloped payload is a success`() = runTest {
        val outcome = clientAnswering(body = """{"data":{"name":"verbum"}}""")
            .payloadCall()

        assertEquals(Outcome.Success(Payload("verbum")), outcome)
    }

    @Test
    fun `an envelope carrying an error is that error rather than its status alone`() = runTest {
        val outcome = clientAnswering(
            status = HttpStatusCode.BadRequest,
            body = """{"error":{"code":"name_taken","message":"That name is in use."}}""",
        ).payloadCall()

        assertEquals(
            Outcome.Failure(VerborumError.Http(400, "name_taken", "That name is in use.")),
            outcome,
        )
    }

    @Test
    fun `a gateway answering html is an http failure carrying the status`() = runTest {
        // The regression this guards: decoding the body regardless of status made a 502 read as a
        // `Serialization` fault, so a caller deciding whether to keep an unsent write saw a
        // permanently malformed payload instead of a server that is worth retrying.
        val outcome = clientAnswering(
            status = HttpStatusCode.BadGateway,
            body = "<html><body>502 Bad Gateway</body></html>",
            contentType = ContentType.Text.Html,
        ).payloadCall()

        assertEquals(Outcome.Failure(VerborumError.Http(502)), outcome)
    }

    @Test
    fun `an empty error body is still an http failure`() = runTest {
        val outcome = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }) {
            install(ContentNegotiation) { json(VerborumJson) }
        }.payloadCall()

        assertEquals(Outcome.Failure(VerborumError.Http(503)), outcome)
    }

    @Test
    fun `a failing status never reads as a success whatever the body carries`() = runTest {
        // An envelope with `data` and no `error` would otherwise unwrap to Success — the status has
        // to win, or a 500 could deliver a payload.
        val outcome = clientAnswering(
            status = HttpStatusCode.InternalServerError,
            body = """{"data":{"name":"verbum"}}""",
        ).payloadCall()

        assertEquals(Outcome.Failure(VerborumError.Http(500)), outcome)
    }

    @Test
    fun `401 is Unauthorized rather than an http error`() = runTest {
        val outcome = clientAnswering(
            status = HttpStatusCode.Unauthorized,
            body = """{"error":{"code":"expired"}}""",
        ).payloadCall()

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), outcome)
    }

    @Test
    fun `a malformed success body is a serialization failure`() = runTest {
        val outcome = clientAnswering(body = """["not","an","envelope"]""").payloadCall()

        val error = (outcome as Outcome.Failure).error
        assertEquals(VerborumError.Serialization::class, error::class)
    }

    @Test
    fun `an envelope with neither data nor error is a serialization failure`() = runTest {
        val outcome = clientAnswering(body = "{}").payloadCall()

        val error = (outcome as Outcome.Failure).error
        assertEquals(VerborumError.Serialization::class, error::class)
    }

    @Test
    fun `a request that never arrives is a network failure`() = runTest {
        val outcome = HttpClient(MockEngine { throw kotlinx.io.IOException("no route to host") }) {
            install(ContentNegotiation) { json(VerborumJson) }
        }.payloadCall()

        val error = (outcome as Outcome.Failure).error
        assertEquals(VerborumError.Network::class, error::class)
    }

    private suspend fun HttpClient.payloadCall(): Outcome<Payload> =
        apiCall { get("https://example.test/thing") }

    private fun clientAnswering(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        contentType: ContentType = ContentType.Application.Json,
    ): HttpClient = HttpClient(
        MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, contentType.toString()),
            )
        },
    ) {
        install(ContentNegotiation) { json(VerborumJson) }
    }
}
