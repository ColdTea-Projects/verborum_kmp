@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.coldtea.verborum.core.common.logging

/**
 * `location` is absent under Node, where the unit tests run, so the check tolerates its absence
 * rather than throwing before any logging is configured.
 */
internal actual fun isDevelopmentHost(): Boolean =
    js("(typeof location !== 'undefined' && (location.hostname === 'localhost' || location.hostname === '127.0.0.1'))")
