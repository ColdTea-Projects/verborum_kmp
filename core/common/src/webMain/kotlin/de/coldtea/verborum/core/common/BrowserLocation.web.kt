package de.coldtea.verborum.core.common

/**
 * `window.location.origin`, e.g. `https://verborum.coldtea.de`. Reached through per-target
 * `js(...)` bridges because `js(...)` bodies are not allowed in a shared intermediate source set.
 */
expect fun browserOrigin(): String

/** `window.location.hash`, including the leading `#`, or empty when the URL carries no fragment. */
expect fun browserFragment(): String

/**
 * What the history entry the page was loaded on has stored against it, or empty when it has none —
 * a fresh tab, or a link someone else sent. A reload keeps it, which is what makes it worth reading.
 *
 * Only a string is returned: anything else stored there belongs to someone other than this app.
 */
expect fun browserHistoryState(): String

/** `decodeURIComponent`, for values that were escaped on their way into a URL. */
expect fun decodeUriComponent(value: String): String

/**
 * Drops the fragment from the address bar without navigating, keeping the history entry's state — the
 * serialized nav back stack lives there, and replacing it with null would lose the stack a reload is
 * about to restore.
 *
 * Narrow on purpose: it takes no URL, so there is no caller-supplied target that would have to be
 * validated against the app's own origin.
 */
expect fun clearBrowserFragment()

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
