package de.coldtea.verborum.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

// The web build is served next to the API, so requests stay same-origin and
// avoid a CORS preflight on every call.
actual fun defaultApiConfig(): ApiConfig = ApiConfig(
    baseUrl = "/api",
    enableLogging = false,
)

internal actual fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Js, config)
