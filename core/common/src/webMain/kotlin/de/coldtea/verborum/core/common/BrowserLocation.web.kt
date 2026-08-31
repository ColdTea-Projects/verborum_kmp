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
fun isLocalDevelopmentOrigin(origin: String): Boolean = hostOf(origin) in LOCAL_HOSTS

/** The host alone, with any port dropped — `https://localhost:8080` and `http://[::1]` both. */
private fun hostOf(origin: String): String {
    val authority = origin.substringAfter("://", missingDelimiterValue = "").substringBefore('/')

    // An IPv6 literal is bracketed and made of colons, so its port is whatever follows the closing
    // bracket. Looking for the last colon instead finds one inside the address: `[::1]` would come
    // back as `[:`, and the loopback would read as somebody's remote host.
    return if (authority.startsWith('[')) {
        authority.substringBefore(']').removePrefix("[")
    } else {
        authority.substringBeforeLast(':')
    }
}

private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "::1")
