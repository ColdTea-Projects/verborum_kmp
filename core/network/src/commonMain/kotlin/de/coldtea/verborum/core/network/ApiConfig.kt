package de.coldtea.verborum.core.network

/**
 * Where the backend lives for the current target. The value is supplied per
 * platform because the web build talks to a same-origin path while the iOS
 * build needs an absolute host.
 */
data class ApiConfig(
    val baseUrl: String,
    val enableLogging: Boolean = false,
)

expect fun defaultApiConfig(): ApiConfig
