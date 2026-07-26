package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeycloakAuthClientTest {

    private val tokenResponse = """
        {
          "access_token": "access-1",
          "refresh_token": "refresh-1",
          "id_token": "id-1",
          "expires_in": 300
        }
    """.trimIndent()

    @Test
    fun `a code exchange turns expires_in into an absolute expiry`() = runTest {
        val recording = RecordingAuthClient(body = tokenResponse, nowEpochSeconds = { 1_000L })

        val outcome = recording.client.exchangeCode(code = "code-1", codeVerifier = "verifier-1")

        assertEquals(
            Outcome.Success(
                AuthTokens(
                    accessToken = "access-1",
                    refreshToken = "refresh-1",
                    expiresAtEpochSeconds = 1_300L,
                    idToken = "id-1",
                ),
            ),
            outcome,
        )
    }

    @Test
    fun `the exchange proves possession of the verifier and never sends a client secret`() = runTest {
        val recording = RecordingAuthClient(body = tokenResponse)

        recording.client.exchangeCode(code = "code-1", codeVerifier = "verifier-1")

        val body = recording.lastRequestBody.orEmpty()
        assertTrue(body.contains("grant_type=authorization_code"))
        assertTrue(body.contains("code_verifier=verifier-1"))
        assertTrue(!body.contains("client_secret"))
    }

    @Test
    fun `a rejected grant is Unauthorized rather than an http error`() = runTest {
        val recording = RecordingAuthClient(
            status = HttpStatusCode.BadRequest,
            body = """{"error":"invalid_grant"}""",
        )

        val outcome = recording.client.exchangeCode(code = "stale", codeVerifier = "verifier-1")

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), outcome)
    }

    @Test
    fun `a server failure keeps its status so it can be told apart from a bad credential`() = runTest {
        val recording = RecordingAuthClient(status = HttpStatusCode.ServiceUnavailable)

        val outcome = recording.client.refresh(refreshToken = "refresh-1")

        assertEquals(Outcome.Failure(VerborumError.Http(503)), outcome)
    }

    @Test
    fun `a response without a refresh token cannot establish a session`() = runTest {
        // Keycloak answers this way when the client is not configured for `offline_access`; a
        // session built on it would die at the first access-token expiry.
        val recording = RecordingAuthClient(
            body = """{"access_token":"access-1","expires_in":300}""",
        )

        val outcome = recording.client.exchangeCode(code = "code-1", codeVerifier = "verifier-1")

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), outcome)
    }

    @Test
    fun `ending the session posts the refresh token to the logout endpoint`() = runTest {
        val recording = RecordingAuthClient(body = "")

        val outcome = recording.client.endSession(refreshToken = "refresh-1")

        assertEquals(Outcome.Success(Unit), outcome)
        assertTrue(recording.lastRequestBody.orEmpty().contains("refresh_token=refresh-1"))
    }
}
