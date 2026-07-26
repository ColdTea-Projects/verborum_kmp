@file:OptIn(ExperimentalStdlibApi::class)

package de.coldtea.verborum.core.auth

internal actual fun sessionStorageGet(key: String): String =
    js("sessionStorage.getItem(key) || ''") as String

internal actual fun sessionStorageSet(key: String, value: String) {
    js("sessionStorage.setItem(key, value)")
}

internal actual fun sessionStorageRemove(key: String) {
    js("sessionStorage.removeItem(key)")
}

internal actual fun localStorageGet(key: String): String =
    js("localStorage.getItem(key) || ''") as String

internal actual fun localStorageSet(key: String, value: String) {
    js("localStorage.setItem(key, value)")
}

internal actual fun localStorageRemove(key: String) {
    js("localStorage.removeItem(key)")
}

internal actual fun browserSearch(): String = js("location.search") as String

internal actual fun browserNavigateTo(url: String) {
    js("location.assign(url)")
}

internal actual fun browserReplaceUrl(url: String) {
    js("history.replaceState(null, '', url)")
}

internal actual fun secureRandomBytes(size: Int): ByteArray = randomHex(size).hexToByteArray()

private fun randomHex(size: Int): String =
    js("Array.from(crypto.getRandomValues(new Uint8Array(size))).map(function (b) { return b.toString(16).padStart(2, '0'); }).join('')") as String

internal actual fun nowMillis(): Double = js("Date.now()") as Double
