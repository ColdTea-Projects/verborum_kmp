package de.coldtea.verborum.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Supplies the access token for outgoing requests; implemented by `core:auth`. */
fun interface BearerTokenProvider {
    suspend fun accessToken(): String?
}

/** The JSON contract shared by every request and response. */
val VerborumJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}

internal expect fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient

/**
 * Builds one [HttpClient] for one backend service. The engine is chosen per target (Darwin on iOS,
 * the browser fetch engine on web); [baseUrl] decides which service it talks to, because the base
 * URL is stamped on by `defaultRequest` rather than written at every call site.
 *
 * [baseUrl] rather than an [ApiConfig], even though that is where every caller but one gets it: the
 * Keycloak client has an issuer and no API service at all, and making it invent the other two URLs
 * to satisfy the type would be inventing exactly the values that must never be guessed.
 */
fun createHttpClient(
    baseUrl: String,
    enableLogging: Boolean = false,
    tokenProvider: BearerTokenProvider? = null,
): HttpClient = platformHttpClient {
    expectSuccess = false

    install(ContentNegotiation) {
        json(VerborumJson)
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
    }

    if (enableLogging) {
        install(Logging) {
            logger = KtorKermitLogger
            // INFO is method, URL and status — no headers and no bodies. The sanitiser is the
            // belt-and-braces for the day someone raises this to HEADERS while debugging: the
            // access token must never reach a console.
            level = LogLevel.INFO
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }

    if (tokenProvider != null) {
        install(bearerAuth(tokenProvider))
    }

    defaultRequest {
        url(baseUrl.ensureTrailingSlash())
        header(HttpHeaders.Accept, ContentType.Application.Json)
    }
}

/** Stamps `Authorization: Bearer …` onto every request that has a token available. */
private fun bearerAuth(tokenProvider: BearerTokenProvider) =
    createClientPlugin("VerborumBearerAuth") {
        onRequest { request, _ ->
            tokenProvider.accessToken()?.let { token ->
                request.header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
