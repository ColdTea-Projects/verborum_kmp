package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.core.network.ApiConfig
import de.coldtea.verborum.core.network.createHttpClient
import de.coldtea.verborum.core.network.toVerborumError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A dedicated client for the Keycloak endpoints: no bearer plugin (these calls authenticate with the
 * code or the refresh token itself) and logging off, because every request and response here carries
 * credentials.
 */
fun createAuthHttpClient(config: AuthConfig): HttpClient = createHttpClient(
    config = ApiConfig(baseUrl = config.issuer, enableLogging = false),
    tokenProvider = null,
)

/**
 * The OpenID Connect token endpoint. Keycloak answers with its own JSON shape rather than the app's
 * `Envelope`, so these calls map their own responses instead of going through `apiCall`.
 *
 * Nothing here is ever logged: a failure reports the status only, never the body.
 */
class KeycloakAuthClient(
    private val config: AuthConfig,
    private val httpClient: HttpClient,
    private val nowEpochSeconds: () -> Long = ::currentEpochSeconds,
) {

    /** Exchanges an authorization code for tokens, proving possession of the PKCE verifier. */
    suspend fun exchangeCode(code: String, codeVerifier: String): Outcome<AuthTokens> = tokenCall(
        Parameters.build {
            append("grant_type", "authorization_code")
            append("client_id", config.clientId)
            append("code", code)
            append("redirect_uri", config.redirectUri)
            append("code_verifier", codeVerifier)
        },
    )

    suspend fun refresh(refreshToken: String): Outcome<AuthTokens> = tokenCall(
        Parameters.build {
            append("grant_type", "refresh_token")
            append("client_id", config.clientId)
            append("refresh_token", refreshToken)
        },
    )

    /**
     * Back-channel logout. Dropping local tokens without this leaves the Keycloak SSO session
     * behind, and the next login silently re-authenticates the same user with no prompt.
     */
    suspend fun endSession(refreshToken: String): Outcome<Unit> = call {
        httpClient.submitForm(
            url = config.endSessionEndpoint,
            formParameters = Parameters.build {
                append("client_id", config.clientId)
                append("refresh_token", refreshToken)
            },
        )
    }.map { }

    private suspend fun tokenCall(form: Parameters): Outcome<AuthTokens> =
        when (val outcome = call { httpClient.submitForm(url = config.tokenEndpoint, formParameters = form) }) {
            is Outcome.Success -> decodeTokens(outcome.data)
            is Outcome.Failure -> outcome
            Outcome.Loading -> Outcome.Loading
        }

    private suspend fun decodeTokens(response: HttpResponse): Outcome<AuthTokens> = try {
        val body = response.body<KeycloakTokenResponse>()
        val refreshToken = body.refreshToken

        if (refreshToken == null) {
            // Without a refresh token the session would die at the first access-token expiry;
            // that means the client is misconfigured (no `offline_access`), not that login failed.
            Outcome.Failure(VerborumError.Unauthorized)
        } else {
            Outcome.Success(
                AuthTokens(
                    accessToken = body.accessToken,
                    refreshToken = refreshToken,
                    expiresAtEpochSeconds = nowEpochSeconds() + body.expiresInSeconds,
                    idToken = body.idToken,
                ),
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Outcome.Failure(throwable.toVerborumError())
    }

    private suspend fun call(request: suspend () -> HttpResponse): Outcome<HttpResponse> = try {
        val response = request()

        when {
            response.status.isSuccess() -> Outcome.Success(response)
            // 400/401 from these endpoints means the grant was rejected: expired code, replayed
            // code, revoked refresh token. All of them mean "not signed in".
            response.status.value == 400 || response.status.value == 401 ->
                Outcome.Failure(VerborumError.Unauthorized)

            else -> Outcome.Failure(VerborumError.Http(response.status.value))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Outcome.Failure(throwable.toVerborumError())
    }
}

@Serializable
private data class KeycloakTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long = 0,
)
