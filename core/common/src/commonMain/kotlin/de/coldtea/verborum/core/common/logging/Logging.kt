package de.coldtea.verborum.core.common.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter

/** Prefix on every tag, so the app's own lines are one filter away in Xcode and the browser. */
const val LOG_TAG: String = "Verborum"

/**
 * Configures the single logger tree the whole app writes to. Called once per launch, from
 * `initKoin()`, before anything can log.
 *
 * The default floor is [defaultMinSeverity], which is verbose while developing and `Warn` in a
 * shipped build — the console of a released app is readable by anyone with the device, so it
 * carries only what someone diagnosing a fault needs.
 */
fun initLogging(
    minSeverity: Severity = defaultMinSeverity(),
    logWriter: LogWriter = platformLogWriter(),
) {
    Logger.setLogWriters(logWriter)
    Logger.setMinSeverity(minSeverity)
    Logger.setTag(LOG_TAG)
}

/**
 * A logger for one area of the app — `logger("Network")`, `logger("Auth")`. The area becomes part
 * of the tag rather than of every message, so lines stay short and filtering stays possible.
 */
fun logger(area: String): Logger = Logger.withTag("$LOG_TAG.$area")

/**
 * Renders a credential as its length alone. Never log a token, an authorization code or a PKCE
 * verifier: on iOS the device console is readable from any connected Mac, and on web the browser
 * console is readable by anything running on the origin. This exists so "is it there and does it
 * look right" can be answered without the value.
 */
fun redacted(secret: String?): String = when {
    secret == null -> "absent"
    secret.isEmpty() -> "empty"
    else -> "present(${secret.length} chars)"
}

/**
 * Verbose while developing, `Warn` in a shipped build. What counts as "developing" is a per-target
 * question: a debug binary on iOS, a development host on web.
 */
internal expect fun defaultMinSeverity(): Severity
