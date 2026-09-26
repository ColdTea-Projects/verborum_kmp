package de.coldtea.verborum.core.common

actual fun browserOrigin(): String = js("location.origin") as String

actual fun browserFragment(): String = js("location.hash") as String

actual fun browserHistoryState(): String =
    js("typeof history.state === 'string' ? history.state : ''") as String

actual fun decodeUriComponent(value: String): String = js("decodeURIComponent(value)") as String

actual fun clearBrowserFragment() {
    js("history.replaceState(history.state, '', location.pathname + location.search)")
}
