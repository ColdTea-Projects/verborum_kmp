package de.coldtea.verborum.core.common.logging

import co.touchlab.kermit.Severity
import kotlin.experimental.ExperimentalNativeApi

/**
 * The Kotlin/Native runtime knows whether it was linked as a debug binary, which is exactly the
 * distinction wanted here: everything while running from Xcode, warnings and worse in a build
 * handed to anyone else.
 */
@OptIn(ExperimentalNativeApi::class)
internal actual fun defaultMinSeverity(): Severity =
    if (kotlin.native.Platform.isDebugBinary) Severity.Verbose else Severity.Warn
