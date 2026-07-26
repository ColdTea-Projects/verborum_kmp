package de.coldtea.verborum.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthorizationFlowTest {

    private val pkce = PkceChallenge(codeVerifier = "verifier", codeChallenge = "challenge")

    @Test
    fun `sign in targets the authorization endpoint with PKCE and state`() {
        val request = testConfig.buildAuthorizationRequest(
            entry = AuthEntry.SignIn,
            pkce = pkce,
            state = "state-123",
        )

        assertTrue(request.url.startsWith("${testConfig.issuer}/protocol/openid-connect/auth"))
        assertTrue(request.url.contains("response_type=code"))
        assertTrue(request.url.contains("client_id=verborum-app"))
        assertTrue(request.url.contains("code_challenge=challenge"))
        assertTrue(request.url.contains("code_challenge_method=S256"))
        assertTrue(request.url.contains("state=state-123"))
    }

    @Test
    fun `the redirect uri is percent encoded rather than pasted in raw`() {
        val request = testConfig.buildAuthorizationRequest(AuthEntry.SignIn, pkce, "state-123")

        // A raw "://" in a query value would truncate the parameter at the first delimiter.
        assertTrue(request.url.contains("redirect_uri=de.coldtea.verborum%3A%2F%2Foauth2redirect%2Fcb"))
    }

    @Test
    fun `creating an account is the same flow against the registrations endpoint`() {
        val request = testConfig.buildAuthorizationRequest(
            entry = AuthEntry.CreateAccount,
            pkce = pkce,
            state = "state-123",
        )

        assertTrue(
            request.url.startsWith("${testConfig.issuer}/protocol/openid-connect/registrations"),
        )
    }

    @Test
    fun `the pending half keeps the verifier and state for the redirect to be matched against`() {
        val request = testConfig.buildAuthorizationRequest(AuthEntry.SignIn, pkce, "state-123")

        assertEquals(PendingAuthorization("verifier", "state-123"), request.pending)
        // The verifier itself must never appear in the URL — only its challenge.
        assertTrue(!request.url.contains("verifier"))
    }

    @Test
    fun `every attempt gets a fresh verifier and a fresh state`() {
        val first = testConfig.buildAuthorizationRequest(AuthEntry.SignIn)
        val second = testConfig.buildAuthorizationRequest(AuthEntry.SignIn)

        assertNotEquals(first.pending.codeVerifier, second.pending.codeVerifier)
        assertNotEquals(first.pending.state, second.pending.state)
    }
}
