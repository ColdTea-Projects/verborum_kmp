package de.coldtea.verborum.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/**
 * A debug binary talks to the local `ms_dictionary` on `:8085` — the same target the web dev server
 * proxies `/api` to — over plain http, which the `NSAllowsLocalNetworking` exception in `Info.plist`
 * permits. Gating on the binary kind keeps a release build on the production `https` base URL.
 */
@OptIn(ExperimentalNativeApi::class)
actual fun defaultApiConfig(): ApiConfig = ApiConfig(
    baseUrl = if (Platform.isDebugBinary) {
        "http://localhost:8085"
    } else {
        "https://api.verborum.coldtea.de"
    },
    enableLogging = true,
)

internal actual fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin, config)
