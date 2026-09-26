@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.coldtea.verborum.core.common

actual fun setDocumentTitle(title: String) {
    js("document.title = title")
}
