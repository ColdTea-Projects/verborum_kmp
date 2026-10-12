package de.coldtea.verborum.feature.forum.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.getOrNull
import de.coldtea.verborum.core.marketplace.DictionaryLanguages
import de.coldtea.verborum.core.marketplace.OwnedDictionariesSource
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.core.profile.domain.VaultEntry
import de.coldtea.verborum.feature.forum.data.SharedDictionaryApi
import de.coldtea.verborum.feature.forum.domain.LanguagePairRanking
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter

/**
 * The filter the Forum opens on: the user's three most-used language pairs.
 *
 * Without it the Forum opens on every dictionary in existence, most of them in languages the user
 * does not study. With it, the first thing they see is plausible — and the chips are removable, so
 * it is a starting point rather than a restriction.
 *
 * Both sources are **best effort**: a failure contributes nothing rather than failing the whole
 * thing. An empty result is a perfectly good answer — a user with no dictionaries gets no filter,
 * and sees everything.
 */
internal class GetDefaultFilterUseCase(
    private val ownedDictionaries: OwnedDictionariesSource,
    private val profileService: ProfileService,
    private val sharedDictionaryApi: SharedDictionaryApi,
) {
    suspend operator fun invoke(): MarketplaceListingFilter {
        val languages = ownedLanguages() + vaultLanguages()

        return MarketplaceListingFilter(
            languagePairs = LanguagePairRanking.topPairs(
                dictionaries = languages,
                limit = LanguagePairRanking.DEFAULT_PAIR_LIMIT,
            ),
        )
    }

    private suspend fun ownedLanguages(): List<DictionaryLanguages> =
        runCatchingLanguages { ownedDictionaries.ownedLanguages() }

    /**
     * What the user has downloaded counts too — those are languages they chose to study. Read in
     * **one** batch request rather than one per entry, and unreadable ids are dropped by the server
     * rather than failing the call.
     */
    private suspend fun vaultLanguages(): List<DictionaryLanguages> = runCatchingLanguages {
        val vault = (profileService.vault() as? Outcome.Success)?.data.orEmpty()
        if (vault.isEmpty()) return@runCatchingLanguages emptyList()

        val importedAt = vault.associate { entry -> entry.dictionaryId to entry.importedAt }
        val dictionaries = sharedDictionaryApi
            .dictionaries(vault.map(VaultEntry::dictionaryId))
            .getOrNull()
            .orEmpty()

        dictionaries.map { dictionary ->
            DictionaryLanguages(
                fromLang = dictionary.fromLang,
                toLang = dictionary.toLang,
                lastActivityAt = importedAt[dictionary.dictionaryId] ?: dictionary.createdAt,
            )
        }
    }

    /**
     * A source that throws contributes nothing. Deliberately broad: this runs before the Forum can
     * show anything, and no failure in it is worth blocking the whole screen over.
     */
    private suspend inline fun runCatchingLanguages(
        block: () -> List<DictionaryLanguages>,
    ): List<DictionaryLanguages> = try {
        block()
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        emptyList()
    }
}
