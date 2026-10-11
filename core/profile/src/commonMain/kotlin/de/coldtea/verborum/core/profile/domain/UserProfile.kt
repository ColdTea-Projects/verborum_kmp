package de.coldtea.verborum.core.profile.domain

/**
 * The signed-in user's profile, as the app reasons about it.
 *
 * Two ids belong to one account and must never be swapped: [userId] is **ms_user's own id**, the one
 * `users/{userId}/vault` takes, while the dictionary and marketplace services key everything on the
 * token's subject (`publisherId`, a dictionary's `userId`). Only ms_user's id lives here, because
 * the subject is already the auth session's to answer for.
 *
 * [isMarketplaceMember] is the whole membership question: the backend grants the Forum to a user who
 * has a display name *and* has accepted the terms, and reports the result as this one flag.
 */
data class UserProfile(
    val userId: String,
    val email: String,
    /** Null when the user has no name — which is legal for anyone who is not a Forum member. */
    val displayName: String? = null,
    val isMarketplaceMember: Boolean = false,
    /** Which terms text was last accepted; kept after a withdrawal, null if never accepted. */
    val marketplaceAgreementVersion: String? = null,
)

/** A dictionary this user downloaded from the Forum, as the vault records it. */
data class VaultEntry(
    val dictionaryId: String,
    /** Null for an entry the backend has not stamped yet; the caller substitutes "now". */
    val importedAt: Long? = null,
)
