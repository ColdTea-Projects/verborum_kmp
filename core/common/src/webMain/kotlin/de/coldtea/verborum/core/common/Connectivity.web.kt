package de.coldtea.verborum.core.common

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * `navigator.onLine` only says whether the browser has a network interface, not whether the API is
 * reachable, so it is a hint rather than a guarantee — good enough for a banner, and it errs
 * towards "online" exactly as the contract asks.
 */
actual fun observeConnectivity(): Flow<Boolean> = callbackFlow {
    trySend(isBrowserOnline())

    val unsubscribe = addConnectivityListener { trySend(isBrowserOnline()) }

    awaitClose(unsubscribe)
}.distinctUntilChanged()

/**
 * The browser is reached through per-target `js(...)` bridges because `js(...)` bodies are not
 * allowed in a shared intermediate source set.
 */
internal expect fun isBrowserOnline(): Boolean

/** Subscribes to the window's `online`/`offline` events; the returned lambda unsubscribes. */
internal expect fun addConnectivityListener(onChange: () -> Unit): () -> Unit
