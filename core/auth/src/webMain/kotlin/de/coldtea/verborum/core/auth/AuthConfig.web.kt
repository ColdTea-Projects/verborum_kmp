package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.browserOrigin

/**
 * Keycloak answers on the app's own origin under `/auth`, both deployed (a reverse proxy serves it
 * next to the app) and in development (the dev server proxies it — see
 * `composeApp/webpack.config.d/devServerProxy.js`).
 *
 * That is what keeps the token exchange same-origin: no CORS policy is involved, the host's
 * `connect-src 'self'` CSP holds, and the Keycloak client needs no Web origins entry.
 *
 * The redirect URI is the app's own root: Keycloak sends the browser back here with `?code=…`, which
 * is consumed and stripped from the URL on the next start.
 */
actual fun defaultAuthConfig(): AuthConfig {
    val origin = browserOrigin()

    return AuthConfig(
        issuer = issuerFor(origin),
        clientId = "verborum-app",
        redirectUri = "$origin/",
    )
}

/** Split out from [defaultAuthConfig] so the same-origin contract is testable without a browser. */
internal fun issuerFor(origin: String): String = "$origin/auth/realms/verborum"
