package de.coldtea.verborum.core.common

internal actual fun isBrowserOnline(): Boolean = js("navigator.onLine") as Boolean

internal actual fun addConnectivityListener(onChange: () -> Unit): () -> Unit {
    // The listener is created in JS and handed back, so removal uses the very same reference.
    val listener = addWindowListeners(onChange)
    return { removeWindowListeners(listener) }
}

private fun addWindowListeners(onChange: () -> Unit): Any =
    js("(function () { var l = function () { onChange(); }; addEventListener('online', l); addEventListener('offline', l); return l; })()") as Any

private fun removeWindowListeners(listener: Any) {
    js("removeEventListener('online', listener); removeEventListener('offline', listener)")
}
