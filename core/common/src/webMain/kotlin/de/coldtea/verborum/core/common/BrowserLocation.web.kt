package de.coldtea.verborum.core.common

/**
 * `window.location.origin`, e.g. `https://verborum.coldtea.de`. Reached through per-target
 * `js(...)` bridges because `js(...)` bodies are not allowed in a shared intermediate source set.
 */
expect fun browserOrigin(): String

/**
 * Whether [origin] is a local development origin, which decides whether the app talks to local
 * services or to the deployed ones behind the reverse proxy.
 *
 * Matches on the parsed host rather than a substring: `https://localhost.example.com` is a remote
 * origin that a `contains("localhost")` check would happily treat as a developer's machine.
 */
fun isLocalDevelopmentOrigin(origin: String): Boolean =
    origin.substringAfter("://", missingDelimiterValue = "")
        .substringBefore('/')
        .substringBeforeLast(':')
        .removeSurrounding("[", "]") in LOCAL_HOSTS

private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "::1")
