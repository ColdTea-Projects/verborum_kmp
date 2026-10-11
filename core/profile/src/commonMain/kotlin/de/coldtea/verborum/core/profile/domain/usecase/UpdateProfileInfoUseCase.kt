package de.coldtea.verborum.core.profile.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.profile.data.ProfileInfoRequest
import de.coldtea.verborum.core.profile.data.UserApi
import de.coldtea.verborum.core.profile.domain.MarketplaceTerms

/**
 * Sends a partial profile change. Only what is passed is sent, because **an absent field means
 * "unchanged"** on this endpoint: sending the name back unchanged alongside a terms change would
 * re-run the backend's reserved-word rule against a name the user did not touch.
 *
 * Accepting the terms always carries the version with it — the backend refuses a `true` without one
 * from a user who is not a member yet — so callers never have to remember that pairing.
 */
internal class UpdateProfileInfoUseCase(private val api: UserApi) {

    /**
     * @param displayName the new name, `""` to remove it, or null to leave it alone.
     * @param accepted whether the user accepts the terms, or null to leave the agreement alone.
     */
    suspend operator fun invoke(displayName: String?, accepted: Boolean?): Outcome<Unit> =
        api.updateProfileInfo(
            ProfileInfoRequest(
                displayName = displayName,
                marketplaceAgreementAccepted = accepted,
                marketplaceAgreementVersion = MarketplaceTerms.VERSION.takeIf { accepted == true },
            ),
        )
}

/**
 * Whether a failed update is the backend explaining a broken profile rule — a reserved word, a name
 * too long, an email another account uses, terms accepted with no name — rather than a transport
 * fault.
 *
 * It matters because this is the one failure whose server text is written for people and may be
 * shown verbatim; everything else gets the app's own generic sentence. Branching on the **status**,
 * never on the text, is the contract (marketplace guide §7).
 */
fun VerborumError.invalidProfileDetail(): String? = when {
    this is VerborumError.Http && status == 400 -> message?.takeIf(String::isNotBlank)
    else -> null
}
