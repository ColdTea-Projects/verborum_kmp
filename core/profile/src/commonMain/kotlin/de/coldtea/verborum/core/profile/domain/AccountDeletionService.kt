package de.coldtea.verborum.core.profile.domain

import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.profile.data.UserApi

/**
 * Deletes the signed-in account (`docs/account-deletion-guide.md`).
 *
 * An interface for the same reason as [ProfileService]: the screen's tests stand one up without the
 * `internal` collaborators of the real one.
 */
interface AccountDeletionService {

    /**
     * Asks the server to delete everything of this account, and only after it answered 200 wipes the
     * device. An unknown result (timeout, 5xx) therefore keeps all local data, and so does a login that
     * is too old — [VerborumError.requiresReauthentication] tells the two apart.
     */
    suspend fun deleteAccount(): Outcome<Unit>
}

internal class SessionAccountDeletionService(
    private val profileService: ProfileService,
    private val api: UserApi,
    private val authService: AuthService,
) : AccountDeletionService {

    override suspend fun deleteAccount(): Outcome<Unit> {
        // ms_user's own id, which the path takes — never the token's subject.
        val userId = profileService.userId() ?: return Outcome.Failure(VerborumError.Unauthorized)

        val outcome = api.deleteUser(userId)
        if (outcome !is Outcome.Success) return outcome

        authService.completeAccountDeletion()
        return outcome
    }
}

/**
 * Whether the server refused the deletion because the login is older than 5 minutes (SEC-13).
 *
 * It means "sign in again", never "failed" — and it is never retried on its own, because a refresh
 * cannot make a login recent. Branches on the status and the error name, never on the sentence.
 */
fun VerborumError.requiresReauthentication(): Boolean =
    this is VerborumError.Http && status == FORBIDDEN && code == REAUTHENTICATION_REQUIRED

private const val FORBIDDEN = 403
private const val REAUTHENTICATION_REQUIRED = "ReauthenticationRequiredException"
