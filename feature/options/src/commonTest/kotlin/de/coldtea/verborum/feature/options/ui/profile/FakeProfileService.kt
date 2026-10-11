package de.coldtea.verborum.feature.options.ui.profile

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.core.profile.domain.UserProfile
import de.coldtea.verborum.core.profile.domain.VaultEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal val annaMember = UserProfile(
    userId = "4140c0de-0000-4000-8000-0000000000bb",
    email = "anna@example.test",
    displayName = "Anna Bauer",
    isMarketplaceMember = true,
    marketplaceAgreementVersion = "2026-10-04",
)

internal val annaNotMember = annaMember.copy(isMarketplaceMember = false, marketplaceAgreementVersion = null)

/**
 * The session profile, scripted.
 *
 * [updateResult] is what the server is pretending to answer. On a success the fake does what the
 * real service does and re-publishes the profile the update implies — that republication is what
 * resets the form, so a test that skipped it would not be exercising the screen's "saved" path.
 */
internal class FakeProfileService(
    initial: UserProfile? = null,
    private val refreshResult: Outcome<UserProfile>? = null,
    private val updateResult: Outcome<Unit> = Outcome.Success(Unit),
) : ProfileService {

    private val _profile = MutableStateFlow(initial)

    override val profile: Flow<UserProfile?> = _profile.asStateFlow()

    override val current: UserProfile? get() = _profile.value

    /** Every update asked for, in the order it was asked — `null` meaning "leave this alone". */
    val updates = mutableListOf<Pair<String?, Boolean?>>()

    var refreshCount = 0
        private set

    override suspend fun refreshProfile(): Outcome<UserProfile> {
        refreshCount++

        val outcome: Outcome<UserProfile> = refreshResult
            ?: _profile.value?.let { Outcome.Success(it) }
            ?: Outcome.Failure(VerborumError.Network())

        if (outcome is Outcome.Success) _profile.value = outcome.data

        return outcome
    }

    override suspend fun userId(): String? = _profile.value?.userId

    override suspend fun updateProfile(displayName: String?, accepted: Boolean?): Outcome<Unit> {
        updates += displayName to accepted

        if (updateResult is Outcome.Success) {
            _profile.value = _profile.value?.copy(
                displayName = displayName?.takeIf(String::isNotBlank) ?: _profile.value?.displayName,
                isMarketplaceMember = accepted ?: _profile.value?.isMarketplaceMember ?: false,
            )
        }

        return updateResult
    }

    override suspend fun vault(): Outcome<List<VaultEntry>> = Outcome.Success(emptyList())

    override suspend fun removeFromVault(dictionaryId: String): Outcome<Unit> = Outcome.Success(Unit)

    override fun clear() {
        _profile.value = null
    }
}
