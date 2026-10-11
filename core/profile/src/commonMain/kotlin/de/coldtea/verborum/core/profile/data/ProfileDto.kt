package de.coldtea.verborum.core.profile.data

import de.coldtea.verborum.core.common.ApiTimestamp
import de.coldtea.verborum.core.profile.domain.UserProfile
import de.coldtea.verborum.core.profile.domain.VaultEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET users/me`. Only `id` is guaranteed: a profile may carry no name and has never accepted the
 * terms until the user does.
 */
@Serializable
internal data class UserProfileResponse(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("marketplaceAgreementAccepted") val marketplaceAgreementAccepted: Boolean = false,
    @SerialName("marketplaceAgreementVersion") val marketplaceAgreementVersion: String? = null,
) {
    /**
     * A blank name is the same as none — the backend trims and treats blank as unset, so carrying
     * `" "` into the app would make an empty field look like a set one and the Update button think
     * nothing had changed.
     */
    fun toUserProfile() = UserProfile(
        userId = id,
        email = email.orEmpty(),
        displayName = displayName?.takeIf(String::isNotBlank),
        isMarketplaceMember = marketplaceAgreementAccepted,
        marketplaceAgreementVersion = marketplaceAgreementVersion,
    )
}

/** `POST users/`. The id is the client's to invent; `keycloakId` must be the token's own subject. */
@Serializable
internal data class UserProfileRequest(
    @SerialName("userId") val userId: String,
    @SerialName("keycloakId") val keycloakId: String,
    @SerialName("email") val email: String,
    @SerialName("displayName") val displayName: String? = null,
)

/**
 * `PUT users/me/profile-info`. Every field is optional and **an absent one is left unchanged**,
 * which is why none of them may be encoded as `null`: `VerborumJson` omits nulls, so a field this
 * class leaves null never reaches the wire. `displayName = ""` is the one explicit value that
 * *removes* the name.
 */
@Serializable
internal data class ProfileInfoRequest(
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("marketplaceAgreementAccepted") val marketplaceAgreementAccepted: Boolean? = null,
    @SerialName("marketplaceAgreementVersion") val marketplaceAgreementVersion: String? = null,
)

/**
 * One entry of `GET users/{userId}/vault` — a reference to someone else's dictionary, not a copy.
 * `importedAt` is absent for an entry the backend has not stamped, and `vaultEntryId` is the
 * server's own bookkeeping the app never needs.
 */
@Serializable
internal data class VaultEntryResponse(
    @SerialName("vaultEntryId") val vaultEntryId: String? = null,
    @SerialName("dictionaryId") val dictionaryId: String,
    @SerialName("importedAt") val importedAt: String? = null,
) {
    fun toVaultEntry() = VaultEntry(
        dictionaryId = dictionaryId,
        importedAt = ApiTimestamp.parse(importedAt),
    )
}
