package de.coldtea.verborum.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/**
 * A debug binary talks to the local `ms_gateway` on `:8080` over plain http, which the
 * `NSAllowsLocalNetworking` exception in `Info.plist` permits. Gating on the binary kind keeps a
 * release build on the production `https` base URL, where the same gateway answers.
 *
 * The same gate decides whether the HTTP traffic is logged. A shipped app's console is readable from
 * any Mac its device is plugged into, and request lines carry the URL — which is where the user's
 * own ids sit. The web target answers this question the same way, with a plain `false`.
 */
@OptIn(ExperimentalNativeApi::class)
actual fun defaultApiConfig(): ApiConfig = if (Platform.isDebugBinary) {
    ApiConfig(
        baseUrl = "http://localhost:8080",
        enableLogging = true,
    )
} else {
    ApiConfig(
        baseUrl = PRODUCTION_API,
        enableLogging = false,
    )
}

internal actual fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin, config)

private const val PRODUCTION_API = "https://api.verborum.coldtea.de"
