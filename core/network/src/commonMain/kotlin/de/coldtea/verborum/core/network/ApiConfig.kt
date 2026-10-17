package de.coldtea.verborum.core.network

/**
 * Where the backend lives for the current target. The value is supplied per platform because the web
 * build and the iOS build reach the gateway at different addresses.
 *
 * One base URL: since the API gateway (backend Phase 5) every call — dictionaries, words, tags, the
 * profile, the vault, the Forum — goes to the same origin, and the gateway routes by the first path
 * segment (`/dictionaries`, `/words`, `/users`, `/marketplace`, `/notifications`).
 */
data class ApiConfig(
    /** The gateway origin, without a trailing slash. */
    val baseUrl: String,
    val enableLogging: Boolean = false,
)

expect fun defaultApiConfig(): ApiConfig
