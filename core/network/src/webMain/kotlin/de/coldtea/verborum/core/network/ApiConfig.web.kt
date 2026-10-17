package de.coldtea.verborum.core.network

import de.coldtea.verborum.core.common.browserOrigin
import de.coldtea.verborum.core.common.hostOf
import de.coldtea.verborum.core.common.isLocalDevelopmentOrigin
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

/**
 * In development the app calls the local `ms_gateway` directly on `:8080` — a different origin from the
 * dev server (`:8280`), so the gateway's CORS answer is what lets the browser through
 * (`CORS_ALLOWED_ORIGINS`, whose default is this dev origin). That is the point: the browser enforces
 * CORS and the Node tests cannot, so developing against the real thing is how a CORS regression shows.
 * The host follows the page, so `http://127.0.0.1:8280` talks to `http://127.0.0.1:8080`.
 *
 * Anywhere else the page is deployed, and the app keeps calling `<origin>/api` — what a reverse proxy
 * in front of the app serves. How a deployed web build reaches the gateway (that path, or an `api.`
 * origin with the CORS allowlist) is decided with the release (roadmap Phase 5R); this branch is
 * deliberately unchanged until then.
 *
 * Keycloak is not part of this: the login still answers on the app's own origin under `/auth`
 * (`AuthConfig.web.kt`, `composeApp/webpack.config.d/devServerProxy.js`).
 */
actual fun defaultApiConfig(): ApiConfig = apiConfigFor(browserOrigin())

/** Split out from [defaultApiConfig] so the routing of the two cases is testable without a browser. */
internal fun apiConfigFor(origin: String): ApiConfig = ApiConfig(
    baseUrl = if (isLocalDevelopmentOrigin(origin)) localGatewayFor(origin) else "$origin/api",
    enableLogging = false,
)

private fun localGatewayFor(origin: String): String {
    val scheme = origin.substringBefore("://")
    val host = hostOf(origin)
    // hostOf drops the brackets of an IPv6 literal; a URL needs them back
    val urlHost = if (':' in host) "[$host]" else host
    return "$scheme://$urlHost:$LOCAL_GATEWAY_PORT"
}

private const val LOCAL_GATEWAY_PORT = 8080

internal actual fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Js, config)
