@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.coldtea.verborum.core.common

actual fun browserOrigin(): String = js("location.origin")
