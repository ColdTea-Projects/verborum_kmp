package de.coldtea.verborum.core.localization

// The bridge only: `js(...)` bodies are illegal in an intermediate source set, and on Wasm each must
// be a single expression that is the whole function body — so everything with logic stays in webMain.

internal actual fun browserLanguageTag(): String = js("navigator.language || ''")

internal actual fun languageStorageGet(key: String): String =
    js("localStorage.getItem(key) || ''")

internal actual fun languageStorageSet(key: String, value: String) {
    js("localStorage.setItem(key, value)")
}

internal actual fun languageStorageRemove(key: String) {
    js("localStorage.removeItem(key)")
}
