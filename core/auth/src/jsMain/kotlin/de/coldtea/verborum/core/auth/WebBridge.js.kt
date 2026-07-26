@file:OptIn(ExperimentalStdlibApi::class)

package de.coldtea.verborum.core.auth

internal actual fun localStorageGet(key: String): String =
    js("localStorage.getItem(key) || ''") as String

internal actual fun localStorageSet(key: String, value: String) {
    js("localStorage.setItem(key, value)")
}

internal actual fun localStorageRemove(key: String) {
    js("localStorage.removeItem(key)")
}

internal actual fun secureRandomBytes(size: Int): ByteArray = randomHex(size).hexToByteArray()

private fun randomHex(size: Int): String =
    js("Array.from(crypto.getRandomValues(new Uint8Array(size))).map(function (b) { return b.toString(16).padStart(2, '0'); }).join('')") as String

internal actual fun nowMillis(): Double = js("Date.now()") as Double
