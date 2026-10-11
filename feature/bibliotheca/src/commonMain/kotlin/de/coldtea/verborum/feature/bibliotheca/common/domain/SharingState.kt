package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Who may share what, right now.
 *
 * The rule it exists for is the server's: **a Forum member who has dictionaries must keep at least
 * one of them shared.** Breaking it is a 400, and the client mirrors it rather than relying on the
 * refusal, because the two places it bites are a switch and a delete — and finding out from the
 * server that a delete was refused means having already hidden the row.
 *
 * `isMember` is false while the profile is unknown. That direction is the safe one here: the rule
 * only ever *restricts*, so an unknown profile offers more than it should rather than blocking
 * something the user is entitled to do — and the server still has the final say.
 */
internal data class SharingState(
    val isMember: Boolean = false,
    val sharedDictionaryIds: Set<String> = emptySet(),
) {
    /**
     * Whether unsharing or deleting [dictionaryId] would leave a member with nothing shared.
     *
     * Note what is *not* here: a member whose only dictionary this is may delete it. The rule is
     * about being left with dictionaries and none of them shared, not about owning none at all.
     */
    fun isLastShared(dictionaryId: String): Boolean =
        isMember && sharedDictionaryIds == setOf(dictionaryId)

    /** A member with nothing shared yet must make their next dictionary public. */
    val needsFirstShare: Boolean get() = isMember && sharedDictionaryIds.isEmpty()
}

/**
 * The live [SharingState], from the session profile and the user's own dictionaries.
 *
 * Downloaded dictionaries are deliberately absent: they are someone else's, so they count for
 * nothing towards the user's own sharing obligation.
 */
internal class ObserveSharingStateUseCase(
    private val repository: DictionaryRepository,
    private val profileService: ProfileService,
) {
    operator fun invoke(): Flow<SharingState> = combine(
        profileService.profile,
        repository.observeDictionaries(),
    ) { profile, own ->
        SharingState(
            isMember = profile?.isMarketplaceMember == true,
            sharedDictionaryIds = own.filter(Dictionary::isPublic)
                .map(Dictionary::dictionaryId)
                .toSet(),
        )
    }.distinctUntilChanged()
}
