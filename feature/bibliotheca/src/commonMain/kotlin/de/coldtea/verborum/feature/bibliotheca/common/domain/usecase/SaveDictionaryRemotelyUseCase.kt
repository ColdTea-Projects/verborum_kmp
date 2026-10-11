package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryApi
import de.coldtea.verborum.feature.bibliotheca.common.data.toRequest
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary

/**
 * Pushes a dictionary to the server **without ever sending a stale `isPublic`**.
 *
 * The problem this solves: the server owns sharing. Joining the Forum makes every one of the user's
 * dictionaries public and leaving makes them all private, both by event, within about a second — so
 * the local copy of that flag is routinely out of date, and an upload that included it would quietly
 * undo a join or a leave the device had not heard about yet.
 *
 * Omitting it is therefore the default, and the two fallbacks exist only because the server will not
 * accept that in every case:
 *
 * 1. Omitted. `null` is "unchanged", which is right for a dictionary the server already has.
 * 2. **400 `InvalidRequestException`** means the server does not have it: a create requires an
 *    explicit value, so the local one is sent.
 * 3. **400 `SharingRequiredException`** on that create means the user is a member with nothing
 *    shared, and the server will not let them create a private dictionary as their only one. Sent
 *    once more as public — the state the server was going to insist on anyway.
 *
 * Each step runs only on the specific code the one before it answered with, so an ordinary refusal
 * is never retried with different data.
 */
internal class SaveDictionaryRemotelyUseCase(
    private val api: DictionaryApi,
) {
    suspend operator fun invoke(dictionary: Dictionary): Outcome<Unit> {
        val omitted = api.create(dictionary.toRequest())
        if (omitted !is Outcome.Failure || !omitted.error.isInvalidRequest()) return omitted

        val withLocalValue = api.create(dictionary.toRequest(isPublic = dictionary.isPublic))

        val mustShare = withLocalValue is Outcome.Failure &&
            withLocalValue.error.isSharingRequired() &&
            !dictionary.isPublic

        return if (mustShare) api.create(dictionary.toRequest(isPublic = true)) else withLocalValue
    }
}

/** The server's name for "this dictionary is new to me, so tell me everything about it". */
private fun VerborumError.isInvalidRequest(): Boolean =
    this is VerborumError.Http && status == 400 && code == INVALID_REQUEST_EXCEPTION

private const val INVALID_REQUEST_EXCEPTION = "InvalidRequestException"
