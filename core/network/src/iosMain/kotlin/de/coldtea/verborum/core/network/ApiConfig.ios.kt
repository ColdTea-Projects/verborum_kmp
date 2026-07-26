package de.coldtea.verborum.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

actual fun defaultApiConfig(): ApiConfig = ApiConfig(
    baseUrl = "https://api.verborum.coldtea.de",
    enableLogging = true,
)

internal actual fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin, config)
