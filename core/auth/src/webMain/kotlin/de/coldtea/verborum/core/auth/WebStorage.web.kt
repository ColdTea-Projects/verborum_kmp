package de.coldtea.verborum.core.auth

/**
 * The browser is reached through per-target `js(...)` bridges because `js(...)` bodies are not
 * allowed in a shared intermediate source set. Every bridge here stays data-only — no `innerHTML`,
 * no `eval`, nothing that could turn a string into code.
 */
internal expect fun sessionStorageGet(key: String): String

internal expect fun sessionStorageSet(key: String, value: String)

internal expect fun sessionStorageRemove(key: String)

/** `window.location.origin`, e.g. `https://verborum.coldtea.de`. */
internal expect fun browserOrigin(): String

/** `window.location.search`, including the leading `?` when non-empty. */
internal expect fun browserSearch(): String

/** A top-level navigation — the only way to hand the user to Keycloak's login page. */
internal expect fun browserNavigateTo(url: String)

/** Rewrites the current URL without navigating, to drop a consumed authorization code. */
internal expect fun browserReplaceUrl(url: String)
