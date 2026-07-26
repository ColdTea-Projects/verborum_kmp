package de.coldtea.verborum.core.network

import de.coldtea.verborum.core.common.browserOrigin
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

/**
 * The API answers on the app's own origin under `/api`, both deployed and in development — the dev
 * server proxies it to the local `ms_dictionary` (see
 * `composeApp/webpack.config.d/devServerProxy.js`).
 *
 * Same-origin by design: no CORS preflight on every call, and no service-side origin allowlist to
 * keep in step with the dev port.
 */
actual fun defaultApiConfig(): ApiConfig = ApiConfig(
    baseUrl = "${browserOrigin()}/api",
    enableLogging = false,
)

internal actual fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Js, config)
