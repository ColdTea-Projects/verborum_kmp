package de.coldtea.verborum.core.network

import de.coldtea.verborum.core.common.browserOrigin
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

/**
 * All three services answer on the app's own origin under `/api`, both deployed and in development —
 * the dev server routes `/api/users` to `ms_user`, `/api/marketplace` to `ms_marketplace` and
 * everything else to `ms_dictionary` (see `composeApp/webpack.config.d/devServerProxy.js`), which is
 * the same routing the API gateway will do.
 *
 * Same-origin by design: no CORS preflight on every call, and no service-side origin allowlist to
 * keep in step with the dev port — which matters more here than on iOS, because the services emit no
 * CORS headers at all yet.
 */
actual fun defaultApiConfig(): ApiConfig {
    val origin = "${browserOrigin()}/api"

    return ApiConfig(
        baseUrl = origin,
        userBaseUrl = origin,
        marketplaceBaseUrl = origin,
        enableLogging = false,
    )
}

internal actual fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Js, config)
