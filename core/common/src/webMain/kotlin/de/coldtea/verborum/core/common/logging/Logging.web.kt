package de.coldtea.verborum.core.common.logging

import co.touchlab.kermit.Severity

/**
 * The browser has no notion of a debug build, so the host stands in for one: the dev server runs on
 * localhost, everything else is someone's real visit. Getting this wrong only ever costs noise in a
 * console, never correctness.
 */
internal actual fun defaultMinSeverity(): Severity =
    if (isDevelopmentHost()) Severity.Verbose else Severity.Warn

/**
 * Reads `location.hostname` through a per-target `js(...)` bridge, because `js(...)` bodies are not
 * allowed in a shared intermediate source set.
 */
internal expect fun isDevelopmentHost(): Boolean
