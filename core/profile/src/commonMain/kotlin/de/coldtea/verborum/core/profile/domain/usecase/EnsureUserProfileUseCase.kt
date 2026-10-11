package de.coldtea.verborum.core.profile.domain.usecase

import de.coldtea.verborum.core.auth.UserIdentity
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.common.logging.logger
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.core.profile.data.UserApi
import de.coldtea.verborum.core.profile.data.UserProfileRequest
import de.coldtea.verborum.core.profile.domain.MarketplaceTerms
import de.coldtea.verborum.core.profile.domain.UserProfile
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The profile this account already has, creating it first if it has none.
 *
 * `GET users/me` is the only source of truth, and its **404 is the signal** — not an error — that
 * this account has never had a profile. Creation happens lazily, the first time something needs the
 * profile, rather than at login: a sign-in that cannot reach ms_user still signs the user in.
 *
 * Two things here look like belt-and-braces and are not:
 *
 * - The `userId` is a **fresh random UUID**, never the token's subject. The two ids are separate by
 *   design (see [UserProfile]), and reusing the subject would wire them together in a way nothing
 *   could undo later.
 * - A **400 with a name** is almost always the backend's reserved-word rule ("admin", "support",
 *   "verborum"…). Retrying without the name — with a *new* id, because the first one may have been
 *   consumed — gets the user a profile they can then name themselves on the Profile screen, instead
 *   of leaving them locked out of the Forum by whatever their identity provider happened to call
 *   them.
 */
internal class EnsureUserProfileUseCase(private val api: UserApi) {

    private val log = logger("Profile")

    suspend operator fun invoke(identity: UserIdentity): Outcome<UserProfile> {
        when (val existing = api.getMe()) {
            is Outcome.Success -> return Outcome.Success(existing.data.toUserProfile())

            // Anything but "no profile" is a real failure: creating one on top of, say, a 500 would
            // be guessing at the state of the account.
            is Outcome.Failure -> if (!existing.error.isNotFound()) return existing

            Outcome.Loading -> return Outcome.Loading
        }

        val email = identity.email?.trim()?.takeIf(String::isNotEmpty)
            ?: return Outcome.Failure(VerborumError.Http(status = 0, code = NO_EMAIL_CLAIM))

        val name = identity.displayName
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.take(MarketplaceTerms.MAX_DISPLAY_NAME_LENGTH)

        when (val created = create(email = email, subject = identity.subject, name = name)) {
            is Outcome.Success -> Unit
            Outcome.Loading -> return Outcome.Loading

            is Outcome.Failure -> when {
                // The account gained a profile while this ran — another device, or a login that was
                // retried. Posting again would only conflict again; the read below finds it.
                created.error.isConflict() ->
                    log.i { "profile already existed, reading it instead of creating one" }

                created.error.isBadRequest() && name != null -> {
                    log.i { "profile creation refused the token's name, retrying without it" }

                    val retry = create(email = email, subject = identity.subject, name = null)
                    if (retry is Outcome.Failure && !retry.error.isConflict()) return retry
                }

                else -> return created
            }
        }

        // The profile is read back rather than assembled from the request: the server decides the
        // membership flag and may have trimmed the name. A 404 now really is a failure.
        return api.getMe().map { response -> response.toUserProfile() }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun create(email: String, subject: String, name: String?): Outcome<Unit> =
        api.createProfile(
            UserProfileRequest(
                userId = Uuid.random().toString(),
                keycloakId = subject,
                email = email,
                displayName = name,
            ),
        )

    internal companion object {
        /** Not an HTTP failure at all, so it carries no status — only a code a test can name. */
        const val NO_EMAIL_CLAIM = "NoEmailClaim"
    }
}

private fun VerborumError.isNotFound(): Boolean = this is VerborumError.Http && status == 404

private fun VerborumError.isConflict(): Boolean = this is VerborumError.Http && status == 409

private fun VerborumError.isBadRequest(): Boolean = this is VerborumError.Http && status == 400
