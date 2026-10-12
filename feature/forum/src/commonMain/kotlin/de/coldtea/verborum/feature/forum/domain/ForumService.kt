package de.coldtea.verborum.feature.forum.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.marketplace.ImportedDictionariesStore
import de.coldtea.verborum.core.profile.domain.usecase.GetSessionIdentityUseCase
import de.coldtea.verborum.feature.forum.data.SharedDictionaryApi
import de.coldtea.verborum.feature.forum.domain.usecase.GetDefaultFilterUseCase
import de.coldtea.verborum.feature.forum.domain.usecase.GetListingsUseCase
import de.coldtea.verborum.feature.forum.domain.usecase.ImportDictionaryUseCase
import kotlinx.coroutines.flow.Flow

/**
 * What the Forum screens talk to. The use cases stay one concern each; this composes them and holds
 * the two pieces of session state they share.
 *
 * The Forum has **no database**. Everything here is read live, and the only things remembered are
 * in memory for the session: which dictionaries have been downloaded (which the library actually
 * owns), and the listings seen so far — see [loadedListings].
 */
internal class ForumService(
    private val getListings: GetListingsUseCase,
    private val importDictionary: ImportDictionaryUseCase,
    private val getDefaultFilter: GetDefaultFilterUseCase,
    private val sharedDictionaryApi: SharedDictionaryApi,
    private val importedDictionaries: ImportedDictionariesStore,
    private val sessionIdentity: GetSessionIdentityUseCase,
) {
    /**
     * The listings loaded this session, by id.
     *
     * The marketplace has **no single-listing endpoint**, so a details screen cannot ask for what
     * the card already showed — the publisher's name, the rating, the import count. Remembering the
     * pages as they are fetched is what lets the details screen show them; a dictionary reached by
     * a direct link has none of it and degrades, which is the documented behaviour rather than a
     * gap.
     */
    private val seenListings = mutableMapOf<String, MarketplaceListing>()

    fun listing(dictionaryId: String): MarketplaceListing? = seenListings[dictionaryId]

    suspend fun listings(
        order: MarketplaceListingOrder,
        filter: MarketplaceListingFilter,
        page: Int,
    ): Outcome<MarketplaceListingPage> = getListings(order, filter, page).also { outcome ->
        if (outcome is Outcome.Success) {
            outcome.data.listings.forEach { listing -> seenListings[listing.dictionaryId] = listing }
        }
    }

    suspend fun defaultFilter(): MarketplaceListingFilter = getDefaultFilter()

    /**
     * Imports a listing and, on success, copies it into the library so it is there immediately.
     *
     * The copy is **best effort and does not affect the result**: the import is what decides whether
     * the download happened, and the next vault refresh repairs a copy that failed. [publisherName]
     * comes from the listing and exists nowhere else — it is the only source of the "by …" line the
     * library shows.
     */
    suspend fun download(listing: MarketplaceListing): ImportResult {
        val result = importDictionary(listing.dictionaryId)
        if (result != ImportResult.IMPORTED) return result

        importedDictionaries.addImportedDictionary(listing.dictionaryId, listing.publisherName)

        return result
    }

    /** The ids already on the device, for the "Downloaded" badge. */
    fun downloadedDictionaryIds(): Flow<List<String>> = importedDictionaries.observeDictionaryIds()

    /**
     * The caller's own token subject, which is what a listing's `publisherId` is compared against.
     * **Never shown** — it identifies a person, and the Forum shows display names.
     *
     * Own listings essentially never appear in browse results since the backend started excluding
     * them, so this is belt-and-braces: cheap, and it keeps the "Yours" badge honest if that ever
     * changes.
     */
    val currentSubject: String? get() = sessionIdentity()?.subject

    suspend fun dictionary(dictionaryId: String): Outcome<SharedDictionary> =
        sharedDictionaryApi.dictionary(dictionaryId)

    suspend fun words(dictionaryId: String): Outcome<List<SharedWord>> =
        sharedDictionaryApi.words(dictionaryId)

    /** Only needed for a dictionary this session never saw a listing for. */
    suspend fun tags(dictionaryId: String): Outcome<List<String>> =
        sharedDictionaryApi.tags(dictionaryId)
}
