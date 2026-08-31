package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.Outcome
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
/**
 * Treats a delete the server answers with 404 as done.
 *
 * The row is not there, which is the state the request was asking for — a delete is idempotent, and
 * the second one reports the first one's success differently. Reading that as a failure is what the
 * two callers above do with it that hurts: the sync would keep the tombstone and replay the same
 * delete on every pass forever, and a screen-level delete would take 404 for a refusal, lift the
 * tombstone and make the row *reappear* while telling the user the delete failed.
 *
 * A row already gone from another device, or a delete whose local cleanup was interrupted after the
 * server had accepted it, both land here.
 */
internal fun Outcome<Unit>.orAlreadyGone(): Outcome<Unit> = when {
    this is Outcome.Failure && error.isNotFound() -> Outcome.Success(Unit)
    else -> this
}

private fun VerborumError.isNotFound(): Boolean = this is VerborumError.Http && status == 404

internal fun VerborumError.isWorthKeeping(): Boolean = when (this) {
    is VerborumError.Network -> true
    // 5xx is the server failing, not refusing: the payload may well be fine.
    is VerborumError.Http -> status >= 500
    is VerborumError.Serialization -> false
    VerborumError.Unauthorized -> false
    is VerborumError.Unknown -> false
}
