@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.coldtea.verborum.core.common

internal actual fun isBrowserOnline(): Boolean = js("navigator.onLine")

internal actual fun addConnectivityListener(onChange: () -> Unit): () -> Unit {
    // The listener is created in JS and handed back, so removal uses the very same reference.
    val listener = addWindowListeners(onChange)
    return { removeWindowListeners(listener) }
}

private fun addWindowListeners(onChange: () -> Unit): JsAny =
    js("(function () { var l = function () { onChange(); }; addEventListener('online', l); addEventListener('offline', l); return l; })()")

private fun removeWindowListeners(listener: JsAny) {
    js("{ removeEventListener('online', listener); removeEventListener('offline', listener); }")
}
