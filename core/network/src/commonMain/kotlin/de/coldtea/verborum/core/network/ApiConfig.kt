package de.coldtea.verborum.core.network

/**
 * Where the backend lives for the current target. The value is supplied per
 * platform because the web build talks to a same-origin path while the iOS
 * build needs an absolute host.
 *
 * Three base URLs because the backend is three services — `ms_dictionary`, `ms_user` and
 * `ms_marketplace` — each on its own port locally. The API gateway (backend phase 5) collapses them
 * onto one origin **keeping the same paths**, so they are already written gateway-shaped: when that
 * lands, the three values simply become equal and nothing else changes.
 */
data class ApiConfig(
    /** `ms_dictionary` — dictionaries, words, tags. */
    val baseUrl: String,
    /** `ms_user` — the profile and the vault. */
    val userBaseUrl: String,
    /** `ms_marketplace` — Forum listings, imports and ratings. */
    val marketplaceBaseUrl: String,
    val enableLogging: Boolean = false,
)

expect fun defaultApiConfig(): ApiConfig
