package de.coldtea.verborum.core.auth

import io.ktor.http.Url

/**
 * In production Keycloak is expected behind the same reverse proxy that serves the app, under
 * `/auth` — the same arrangement `ApiConfig` relies on for `/api`. That keeps the token exchange
 * same-origin, so no CORS policy is involved and the host's `connect-src 'self'` CSP holds.
 *
 * The dev server proxies nothing, so on a localhost origin the app talks to a local Keycloak
 * directly, mirroring the Android debug build. That needs `http://localhost:8280` — the dev server
 * port set in `composeApp/build.gradle.kts` — in the client's **Web origins** (for the token call's
 * CORS preflight) and the same origin, wildcard path, in its **Valid redirect URIs**.
 *
 * The redirect URI is the app's own root either way: Keycloak sends the browser back here with
 * `?code=…`, which is consumed and stripped from the URL on the next start.
 */
actual fun defaultAuthConfig(): AuthConfig {
    val origin = browserOrigin()

    return AuthConfig(
        issuer = issuerFor(origin),
        clientId = "verborum-app",
        redirectUri = "$origin/",
    )
}

/**
 * Split out from [defaultAuthConfig] so the localhost decision is testable without a browser.
 *
 * Matching on the parsed host, not on a substring: `https://localhost.example.com` is a remote
 * origin that a `contains("localhost")` check would happily point at a developer's machine.
 */
internal fun issuerFor(origin: String): String {
    val host = runCatching { Url(origin).host }.getOrNull()

    return if (host in LOCAL_HOSTS) LOCAL_ISSUER else "$origin/auth/realms/verborum"
}

/** Keycloak's own origin in local development, as the Android debug build also uses. */
private const val LOCAL_ISSUER = "http://localhost:8180/realms/verborum"

private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")
