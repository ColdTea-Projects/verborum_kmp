package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.VerborumError

/**
 * Whether a write that failed should stay on the device as pending rather than be undone.
 *
 * The question is not "did it fail" but "could the same request succeed later". A request that
 * never arrived will, so the row keeps `isSynced = false` and the next upload carries it. One the
 * server actively refused would fail identically every time, so it is rolled back instead of
 * retrying forever against a payload the backend has already judged.
 *
 * [VerborumError.Unauthorized] is deliberately *not* retryable. The session can come back as a
 * different person, and uploading one user's rows under another's subject is a worse outcome than
 * losing an unsent edit.
 */
internal fun VerborumError.isWorthKeeping(): Boolean = when (this) {
    is VerborumError.Network -> true
    // 5xx is the server failing, not refusing: the payload may well be fine.
    is VerborumError.Http -> status >= 500
    is VerborumError.Serialization -> false
    VerborumError.Unauthorized -> false
    is VerborumError.Unknown -> false
}
