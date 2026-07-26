package de.coldtea.verborum.core.auth

import io.ktor.http.URLBuilder
import kotlinx.serialization.Serializable

/** Login and account creation are the same flow pointed at different Keycloak endpoints. */
enum class AuthEntry { SignIn, CreateAccount }

/**
 * The half of an authorization attempt that must never leave the client: the PKCE verifier and the
 * CSRF `state`. On web it has to survive a full page reload, so it is persisted — see
 * [PendingAuthorizationStore].
 */
@Serializable
data class PendingAuthorization(
    val codeVerifier: String,
    val state: String,
)

/** A browser URL to open, plus the secret the resulting redirect must be matched against. */
data class AuthorizationRequest(
    val url: String,
    val pending: PendingAuthorization,
)

/** The outcome of the browser leg of the Authorization Code flow. */
sealed interface AuthorizationResult {

    data class Code(val code: String, val state: String) : AuthorizationResult

    /** The user dismissed the browser; not an error, and not worth surfacing as one. */
    data object Cancelled : AuthorizationResult

    data class Failed(val reason: String?) : AuthorizationResult
}

/**
 * Opens the platform's *system* browser for the authorization leg — `ASWebAuthenticationSession` on
 * iOS, a top-level navigation on web. Never an embedded web view: it can read the user's
 * credentials, breaks federated sign-in and is an App Store review risk.
 */
interface AuthorizationLauncher {

    /**
     * Opens [url] and waits for the redirect to [redirectUri]. On web this navigates the page away
     * and therefore never returns — the redirect arrives at the next app start via [consumeRedirect].
     */
    suspend fun authorize(url: String, redirectUri: String): AuthorizationResult

    /**
     * The authorization code the app was *started* with, if any (web only). Consuming it also
     * removes it from the page URL, so a code never lingers in browser history.
     */
    fun consumeRedirect(): AuthorizationResult?
}

expect fun createAuthorizationLauncher(): AuthorizationLauncher

/** Holds [PendingAuthorization] across the browser leg. Persisted on web, in memory on iOS. */
interface PendingAuthorizationStore {
    fun save(pending: PendingAuthorization)

    /** Reads and removes it: one pending authorization can be completed exactly once. */
    fun consume(): PendingAuthorization?
}

expect fun createPendingAuthorizationStore(): PendingAuthorizationStore

/**
 * Builds one Authorization Code + PKCE attempt. A fresh verifier and a fresh `state` per attempt:
 * PKCE binds the code to this client, `state` is the CSRF defence, and PKCE does not replace it.
 */
fun AuthConfig.buildAuthorizationRequest(
    entry: AuthEntry,
    pkce: PkceChallenge = Pkce.generate(),
    state: String = randomState(),
): AuthorizationRequest {
    val endpoint = when (entry) {
        AuthEntry.SignIn -> authorizationEndpoint
        AuthEntry.CreateAccount -> registrationEndpoint
    }

    val url = URLBuilder(endpoint).apply {
        parameters.append("client_id", clientId)
        parameters.append("response_type", "code")
        parameters.append("redirect_uri", redirectUri)
        parameters.append("scope", scope)
        parameters.append("state", state)
        parameters.append("code_challenge", pkce.codeChallenge)
        parameters.append("code_challenge_method", pkce.codeChallengeMethod)
    }.buildString()

    return AuthorizationRequest(
        url = url,
        pending = PendingAuthorization(codeVerifier = pkce.codeVerifier, state = state),
    )
}

/** 16 bytes of platform CSPRNG — the same source PKCE uses. */
internal fun randomState(): String = Pkce.base64UrlEncode(secureRandomBytes(STATE_BYTES))

private const val STATE_BYTES = 16
