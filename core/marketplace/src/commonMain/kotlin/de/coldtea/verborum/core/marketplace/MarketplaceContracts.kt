package de.coldtea.verborum.core.marketplace

import de.coldtea.verborum.core.common.Outcome
import kotlinx.coroutines.flow.Flow

/**
 * The seams between the Forum and the library.
 *
 * They exist in a module of their own because the two features need each other in **both**
 * directions — the Forum writes a downloaded dictionary into the library and reads which ones are
 * already there; the library shows a rating card for a dictionary the Forum owns the rating of — and
 * a feature may never depend on a feature. Declaring the interfaces here and binding each
 * implementation in its own feature's Koin module keeps the graph pointing one way.
 *
 * Nothing here is implemented in this module. It is the contract and nothing else.
 */

/**
 * Rating a downloaded dictionary. Implemented by the Forum, which owns the marketplace endpoints;
 * consumed by the library's details screen, which shows the same card.
 *
 * Online-only, like everything else about the Forum: a rating is never queued.
 */
interface MarketplaceRatings {

    /**
     * The user's own rating, or `Outcome.Success(null)` when they have not rated it — which the
     * backend reports as a 404 and is **not** a failure.
     *
     * The three states are all load-bearing: a star count, "not rated", and "could not find out".
     * The last one has to keep the control hidden rather than showing an empty five stars, or an
     * offline user would be told they had not rated something they had.
     */
    suspend fun myRating(dictionaryId: String): Outcome<Int?>

    suspend fun setRating(dictionaryId: String, stars: Int): RatingOutcome

    suspend fun removeRating(dictionaryId: String): RatingOutcome
}

/**
 * What came of a rating write. An enum rather than `Outcome` because each case is a different
 * sentence to the user rather than a different error to reason about.
 */
enum class RatingOutcome {

    DONE,

    /** The owner made it private or deleted it, so it can no longer be rated. */
    UNAVAILABLE,

    /** Their own dictionary, one they have not downloaded, or they are not a member. */
    NOT_ALLOWED,

    /** Unreachable, or anything else. */
    FAILED,
}

/**
 * The library's record of what has been downloaded. Implemented by the library; consumed by the
 * Forum, for the "Downloaded" badge and to put a dictionary in front of the user the moment the
 * import succeeds.
 */
interface ImportedDictionariesStore {

    /**
     * The ids of every downloaded dictionary. Deliberately **not** hidden from a non-member, unlike
     * the library's own reads: the Forum needs it to stop offering a dictionary the user already has,
     * and a non-member cannot see the Forum anyway.
     */
    fun observeDictionaryIds(): Flow<List<String>>

    /**
     * Copies a dictionary the user has just imported into the library, so it is there before the
     * next sync.
     *
     * Best effort by design: the import is what decides whether the download succeeded, and the next
     * vault refresh repairs a copy that failed. [publisherName] comes from the listing and exists
     * nowhere else — it is the only source of the "by …" line in the library.
     */
    suspend fun addImportedDictionary(dictionaryId: String, publisherName: String?)
}

/**
 * The language pairs the user already works in. Implemented by the library; consumed by the Forum to
 * choose a sensible default filter instead of opening on every dictionary in existence.
 */
interface OwnedDictionariesSource {
    suspend fun ownedLanguages(): List<DictionaryLanguages>
}

/**
 * One dictionary reduced to what ranking its language pair needs.
 *
 * [lastActivityAt] is `max(updatedAt, createdAt)` rather than `updatedAt` alone, because `updatedAt`
 * is 0 until the server first stamps the row — so a dictionary created offline would otherwise rank
 * as the oldest thing the user owns.
 */
data class DictionaryLanguages(
    val fromLang: String,
    val toLang: String,
    val lastActivityAt: Long,
)
