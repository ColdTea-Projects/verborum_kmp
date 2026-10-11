package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.network.VerborumJson
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryApi
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.SaveDictionaryRemotelyUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The server owns `isPublic` once a dictionary exists — joining the Forum makes every one of the
 * user's dictionaries public and leaving makes them all private, both by event. So the client's copy
 * of that flag is routinely stale, and an upload that sent it would undo a join the device had not
 * heard about yet.
 *
 * These pin the escalation: omit it, and add it only on the specific refusal that asks for it.
 */
class SaveDictionaryRemotelyUseCaseTest {

    @Test
    fun `an ordinary save leaves the sharing flag out of the body entirely`() = runTest {
        val backend = RecordingDictionaryBackend()

        val outcome = SaveDictionaryRemotelyUseCase(backend.api)(dictionary("a"))

        assertEquals(Outcome.Success(Unit), outcome)
        // Absent, not `false`: on this endpoint absent means "unchanged", which is the whole point.
        assertTrue(backend.bodies.single().let { "isPublic" !in it })
    }

    @Test
    fun `a dictionary the server does not have is resent with the local value`() = runTest {
        val backend = RecordingDictionaryBackend(
            answers = listOf(invalidRequest, HttpStatusCode.Created to "{}"),
        )

        val outcome = SaveDictionaryRemotelyUseCase(backend.api)(
            dictionary("a").copy(isPublic = true),
        )

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals(2, backend.bodies.size)
        assertTrue("isPublic" !in backend.bodies[0])
        assertTrue(""""isPublic":true""" in backend.bodies[1])
    }

    @Test
    fun `a member's first dictionary is resent as shared when the server insists`() = runTest {
        val backend = RecordingDictionaryBackend(
            answers = listOf(invalidRequest, sharingRequired, HttpStatusCode.Created to "{}"),
        )

        // Private, and the member has nothing shared — which the server will not allow.
        val outcome = SaveDictionaryRemotelyUseCase(backend.api)(dictionary("a"))

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals(3, backend.bodies.size)
        assertTrue(""""isPublic":false""" in backend.bodies[1])
        // Public is the state the server was going to insist on anyway.
        assertTrue(""""isPublic":true""" in backend.bodies[2])
    }

    @Test
    fun `a dictionary already public is not resent after a sharing refusal`() = runTest {
        val backend = RecordingDictionaryBackend(answers = listOf(invalidRequest, sharingRequired))

        val outcome = SaveDictionaryRemotelyUseCase(backend.api)(
            dictionary("a").copy(isPublic = true),
        )

        // There is nothing left to escalate to, so the refusal stands rather than looping.
        assertTrue(outcome is Outcome.Failure)
        assertEquals(2, backend.bodies.size)
    }

    @Test
    fun `an ordinary refusal is never retried with different data`() = runTest {
        val backend = RecordingDictionaryBackend(
            answers = listOf(HttpStatusCode.BadRequest to """{"error":"MethodArgumentNotValidException"}"""),
        )

        val outcome = SaveDictionaryRemotelyUseCase(backend.api)(dictionary("a"))

        assertTrue(outcome is Outcome.Failure)
        // Each step runs only on the specific code the one before answered with.
        assertEquals(1, backend.bodies.size)
    }

    private val invalidRequest =
        HttpStatusCode.BadRequest to """{"status":400,"error":"InvalidRequestException"}"""

    private val sharingRequired =
        HttpStatusCode.BadRequest to """{"status":400,"error":"SharingRequiredException"}"""
}

/** Answers each call in turn, recording the bodies sent. The last answer repeats. */
private class RecordingDictionaryBackend(
    private val answers: List<Pair<HttpStatusCode, String>> = listOf(HttpStatusCode.Created to "{}"),
) {
    private val _bodies = mutableListOf<String>()

    val bodies: List<String> get() = _bodies

    private var index = 0

    val api = DictionaryApi(
        HttpClient(
            MockEngine { request ->
                _bodies += (request.body as? OutgoingContent.ByteArrayContent)
                    ?.bytes()
                    ?.decodeToString()
                    .orEmpty()

                val (status, body) = answers.getOrElse(index) { answers.last() }
                index++

                respond(body, status, headersOf("Content-Type", "application/json"))
            },
        ) {
            install(ContentNegotiation) { json(VerborumJson) }
        },
    )
}
