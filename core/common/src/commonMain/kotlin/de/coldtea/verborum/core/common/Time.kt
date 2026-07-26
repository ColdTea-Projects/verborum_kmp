package de.coldtea.verborum.core.common

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Wall-clock access, injected rather than called directly so anything time-dependent stays
 * testable — the same reason `AuthSession` takes a `nowEpochSeconds` lambda.
 */
fun interface TimeProvider {
    fun nowEpochMillis(): Long
}

@OptIn(ExperimentalTime::class)
val SystemTimeProvider: TimeProvider = TimeProvider { Clock.System.now().toEpochMilliseconds() }

/**
 * Converts between the backend's ISO-8601 timestamps (written by Hibernate's `@CreationTimestamp` /
 * `@UpdateTimestamp`) and the epoch millis the app works in.
 *
 * Tolerated on the wire, because the server may narrow the shape later:
 * - with an offset — `2026-07-19T21:27:20.400672+02:00`, or a trailing `Z`
 * - without an offset — `2026-07-19T17:01:20.906614`, read as UTC (see below)
 * - epoch millis as a digits-only string
 */
@OptIn(ExperimentalTime::class)
object ApiTimestamp {

    /** Epoch millis, or null when [raw] is absent or unparseable. */
    fun parse(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null

        // Already epoch millis.
        value.toLongOrNull()?.let { return it }

        // A value with no offset is ambiguous. It is read as UTC rather than in the device's zone:
        // the server stores UTC, so this is right whenever the offset is simply missing, and it
        // cannot drift by the device's own timezone setting.
        val normalized = if (value.hasOffset()) value else "${value}Z"

        return runCatching { Instant.parse(normalized).toEpochMilliseconds() }.getOrNull()
    }

    /** Renders epoch millis as ISO-8601 in UTC, which is what the API expects. */
    fun format(epochMillis: Long): String = Instant.fromEpochMilliseconds(epochMillis).toString()

    private fun String.hasOffset(): Boolean =
        endsWith("Z", ignoreCase = true) ||
            // Only the time part can carry an offset, so ignore the date's own hyphens.
            substringAfter('T', missingDelimiterValue = "").let { time ->
                time.contains('+') || time.contains('-')
            }
}
