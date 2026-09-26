@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.coldtea.verborum.core.common

actual fun browserOrigin(): String = js("location.origin")

actual fun browserFragment(): String = js("location.hash")

actual fun browserHistoryState(): String =
    js("typeof history.state === 'string' ? history.state : ''")

actual fun decodeUriComponent(value: String): String = js("decodeURIComponent(value)")

actual fun clearBrowserFragment() {
    js("history.replaceState(history.state, '', location.pathname + location.search)")
}
