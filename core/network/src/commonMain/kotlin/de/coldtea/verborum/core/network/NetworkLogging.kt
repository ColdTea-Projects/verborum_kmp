package de.coldtea.verborum.core.network

import co.touchlab.kermit.Logger
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.logging.logger
import de.coldtea.verborum.core.common.onFailure
import io.ktor.client.plugins.logging.Logger as KtorLogger

/**
 * The network area's logger. `@PublishedApi internal` rather than private because the `apiCall`
 * family is `inline` — a public inline function can only reach declarations marked this way.
 */
@PublishedApi
internal val networkLog: Logger = logger("Network")

/**
 * Routes Ktor's own plugin output into Kermit, so HTTP lines carry the app's tag and obey the same
 * severity floor as everything else instead of going to `println`.
 *
 * Ktor logs at one level, and it is request traffic rather than a fault, so it lands at debug and
 * disappears entirely in a shipped build.
 */
internal object KtorKermitLogger : KtorLogger {
    override fun log(message: String) = networkLog.d { message }
}

/**
 * Logs a failed request once, where the failure becomes an [Outcome] — so a repository that simply
 * hands the failure on still leaves a trace, and nothing has to log defensively further up.
 *
 * Only the path is recorded, never the query string or the body: those carry user content, and a
 * warning line is not the place for it.
 */
@PublishedApi
internal fun <T> Outcome<T>.logFailure(path: String? = null): Outcome<T> =
    onFailure { error ->
        networkLog.w { "request failed${path?.let { " ($it)" }.orEmpty()}: $error" }
    }
