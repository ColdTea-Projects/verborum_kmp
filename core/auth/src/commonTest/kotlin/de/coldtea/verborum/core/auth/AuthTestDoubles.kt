package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.network.VerborumJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json

internal val testConfig = AuthConfig(
    issuer = "https://auth.example.test/realms/verborum",
    clientId = "verborum-app",
    redirectUri = "de.coldtea.verborum://oauth2redirect/cb",
)

/** A Keycloak client answering [body] with [status] for every call, recording the last request. */
internal class RecordingAuthClient(
    private val status: HttpStatusCode = HttpStatusCode.OK,
    private val body: String = "{}",
    nowEpochSeconds: () -> Long = { 0L },
) {
    var lastRequestBody: String? = null
        private set

    val client = KeycloakAuthClient(
        config = testConfig,
        httpClient = HttpClient(
            MockEngine { request ->
                lastRequestBody = (request.body as? OutgoingContent.ByteArrayContent)
                    ?.bytes()
                    ?.decodeToString()
                respond(
                    content = body,
                    status = status,
                    headers = headersOf("Content-Type", "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(VerborumJson) }
        },
        nowEpochSeconds = nowEpochSeconds,
    )
}

/**
 * Stands in for the system browser. [resultFor] receives the `state` the authorization URL was built
 * with, so a test can echo it back the way Keycloak does — or deliberately not, to model a code
 * arriving from someone else's request.
 */
internal class FakeAuthorizationLauncher(
    private val resultFor: (state: String) -> AuthorizationResult,
) : AuthorizationLauncher {

    var authorizedUrl: String? = null
        private set

    override suspend fun authorize(url: String, redirectUri: String): AuthorizationResult {
        authorizedUrl = url
        return resultFor(Url(url).parameters["state"].orEmpty())
    }

    override fun consumeRedirect(): AuthorizationResult? = null
}

/**
 * [retainSaves] `false` models the pending authorization being lost — a different tab, or cleared
 * storage — which must never let a code through.
 */
internal class FakePendingAuthorizationStore(
    private val retainSaves: Boolean = true,
) : PendingAuthorizationStore {

    private var pending: PendingAuthorization? = null

    override fun save(pending: PendingAuthorization) {
        if (retainSaves) this.pending = pending
    }

    override fun consume(): PendingAuthorization? = pending.also { pending = null }
}

/** A JWT-shaped token carrying [claims]; the signature is never checked client-side. */
internal fun jwtWithClaims(claims: String): String {
    val payload = Pkce.base64UrlEncode(claims.encodeToByteArray())
    return "header.$payload.signature"
}
