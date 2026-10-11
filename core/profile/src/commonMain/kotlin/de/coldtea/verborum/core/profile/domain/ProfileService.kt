package de.coldtea.verborum.core.profile.domain

import de.coldtea.verborum.core.auth.PostLogoutHook
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.common.logging.logger
import de.coldtea.verborum.core.common.onSuccess
import de.coldtea.verborum.core.profile.data.VaultApi
import de.coldtea.verborum.core.profile.domain.usecase.EnsureUserProfileUseCase
import de.coldtea.verborum.core.profile.domain.usecase.GetSessionIdentityUseCase
import de.coldtea.verborum.core.profile.domain.usecase.UpdateProfileInfoUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The session's one copy of the profile — what the Profile screen edits, what the Forum gate reads,
 * and what decides whether downloaded dictionaries are shown.
 *
 * An interface because the implementation's collaborators are `internal` to this module, so a screen
 * in another module could not otherwise stand one up in a test. Same split, and the same reason, as
 * `DictionaryRepository` / `NetworkDictionaryRepository`.
 *
 * Deliberately **not** persisted. Membership can change on another device, and a stale "yes" would
 * show a non-member a Forum that answers 403 to everything; so the cache lives for the session and
 * every entry point that depends on it refreshes it. Equally, nothing here is ever queued: a profile
 * change the server did not take did not happen.
 */
interface ProfileService {

    /**
     * Null until the first successful load, and that third state is load-bearing: "not known yet"
     * is not "not a member", and an offline cold start must not strip a member of the dictionaries
     * they downloaded.
     */
    val profile: Flow<UserProfile?>

    /** What is cached right now, for a caller with no composition to collect in. */
    val current: UserProfile?

    /**
     * Re-reads the profile, creating it if this account has none. Called on every entry point that
     * depends on it rather than once at login, because another device may have changed it.
     */
    suspend fun refreshProfile(): Outcome<UserProfile>

    /**
     * ms_user's own id, which is what the vault paths take — never the token's subject. Loads the
     * profile first when it is not known yet, so a caller does not have to order the two calls.
     */
    suspend fun userId(): String?

    /**
     * Applies a profile change and leaves the cache holding **what the server says**, not what was
     * asked for: the backend trims the name, owns the membership flag, and may have refused part of
     * it. Only what is passed is sent — an absent field means "unchanged".
     */
    suspend fun updateProfile(displayName: String?, accepted: Boolean?): Outcome<Unit>

    /** Which dictionaries this user has downloaded from the Forum. */
    suspend fun vault(): Outcome<List<VaultEntry>>

    /**
     * Drops a downloaded dictionary from the vault. The server has to agree first — the caller
     * deletes its local copy only after this succeeds, so a failed call leaves nothing inconsistent.
     */
    suspend fun removeFromVault(dictionaryId: String): Outcome<Unit>

    /**
     * Forgets the cached profile. Registered as a post-logout hook: the next account to sign in on
     * this device must never see the previous one's name, email or membership.
     */
    fun clear()
}

internal class SessionProfileService(
    private val sessionIdentity: GetSessionIdentityUseCase,
    private val ensureUserProfile: EnsureUserProfileUseCase,
    private val updateProfileInfo: UpdateProfileInfoUseCase,
    private val vaultApi: VaultApi,
    private val membershipHooks: List<MarketplaceMembershipHook>,
) : ProfileService {

    private val log = logger("Profile")

    private val _profile = MutableStateFlow<UserProfile?>(null)

    /**
     * Distinct by nature: a `StateFlow` only emits a value that differs from the last one, so a
     * refresh that finds nothing changed costs no downstream work.
     */
    override val profile: Flow<UserProfile?> = _profile.asStateFlow()

    override val current: UserProfile? get() = _profile.value

    /** Refreshes are serialised, so two screens entering at once make one round trip each, in order. */
    private val mutex = Mutex()

    override suspend fun refreshProfile(): Outcome<UserProfile> = mutex.withLock {
        val identity = sessionIdentity()
            ?: return@withLock Outcome.Failure(VerborumError.Unauthorized)

        ensureUserProfile(identity).onSuccess { loaded -> _profile.value = loaded }
    }

    override suspend fun userId(): String? =
        current?.userId ?: (refreshProfile() as? Outcome.Success)?.data?.userId

    override suspend fun updateProfile(displayName: String?, accepted: Boolean?): Outcome<Unit> {
        val outcome = updateProfileInfo(displayName = displayName, accepted = accepted)
        if (outcome !is Outcome.Success) return outcome

        // Re-reading is also what makes "saved" visible on screen: the form resets from every
        // profile the service publishes.
        refreshProfile()

        if (accepted != null) notifyMembershipChanged(accepted)

        return outcome
    }

    override suspend fun vault(): Outcome<List<VaultEntry>> {
        val userId = userId() ?: return Outcome.Failure(VerborumError.Unauthorized)

        return vaultApi.vaultOf(userId)
    }

    override suspend fun removeFromVault(dictionaryId: String): Outcome<Unit> {
        val userId = userId() ?: return Outcome.Failure(VerborumError.Unauthorized)

        return vaultApi.removeFromVault(userId, dictionaryId)
    }

    override fun clear() {
        _profile.value = null
    }

    /** Each hook isolated, so one failing never fails the update the user actually asked for. */
    private suspend fun notifyMembershipChanged(isMember: Boolean) {
        membershipHooks.forEach { hook ->
            try {
                hook.onMembershipChanged(isMember)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                log.w { "membership hook ${hook::class.simpleName} failed: ${throwable::class.simpleName}" }
            }
        }
    }
}

/** The post-logout registration for [ProfileService.clear]. */
fun ProfileService.asPostLogoutHook(): PostLogoutHook = PostLogoutHook { clear() }
