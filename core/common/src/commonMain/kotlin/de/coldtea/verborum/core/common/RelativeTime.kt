package de.coldtea.verborum.core.common

/**
 * "3 days ago", "5 minutes ago" — the age label on list rows.
 *
 * Hand-rolled because there is no common Kotlin equivalent of Android's `DateUtils`, and English
 * only for now: the app ships no localisation yet, so a platform formatter would be the odd one out
 * rather than an improvement.
 */
object RelativeTime {

    fun ago(epochMillis: Long, nowEpochMillis: Long): String {
        val elapsed = nowEpochMillis - epochMillis

        // A timestamp in the future is a clock disagreement between server and device, not
        // something to render as "in -3 days".
        if (elapsed < MINUTE) return "just now"

        val (amount, unit) = when {
            elapsed < HOUR -> elapsed / MINUTE to "minute"
            elapsed < DAY -> elapsed / HOUR to "hour"
            elapsed < MONTH -> elapsed / DAY to "day"
            elapsed < YEAR -> elapsed / MONTH to "month"
            else -> elapsed / YEAR to "year"
        }

        return "$amount ${unit.pluralized(amount)} ago"
    }

    private fun String.pluralized(amount: Long): String = if (amount == 1L) this else "${this}s"

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    // Calendar-agnostic approximations: this labels an age, it does not do date arithmetic.
    private const val MONTH = 30 * DAY
    private const val YEAR = 365 * DAY
}

/** "1 word" / "12 words" — plural agreement without a plurals resource. */
fun pluralize(count: Int, singular: String, plural: String = "${singular}s"): String =
    "$count ${if (count == 1) singular else plural}"
