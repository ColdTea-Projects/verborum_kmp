package de.coldtea.verborum.feature.forum.domain

/**
 * One dictionary on offer in the Forum.
 *
 * **Every string here was written by another user** — the name, the publisher's name, the tags.
 * They are rendered as plain text and never interpreted, and [publisherId] is never shown at all:
 * it is the owner's token subject, used only to recognise the caller's own listings.
 */
internal data class MarketplaceListing(
    val dictionaryId: String,
    val publisherId: String,
    /** Null when the publisher has no usable name; the card then says "Unknown publisher". */
    val publisherName: String?,
    val name: String,
    val fromLang: String,
    val toLang: String,
    val tags: List<String> = emptyList(),
    val importCount: Int = 0,
    /** Null while nobody has rated it — which is **not** zero stars, and is shown differently. */
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
    val publishedAt: Long = 0L,
)

internal data class MarketplaceListingPage(
    val listings: List<MarketplaceListing>,
    val page: Int,
    val hasNext: Boolean,
)

/** Which listing endpoint to ask. "Top rated" exists on the backend; the product does not use it. */
internal enum class MarketplaceListingOrder(val path: String) {
    /** Newest first. */
    FRESH("marketplace/dictionaries"),

    /** Most imported first, newest among equals. */
    POPULAR("marketplace/dictionaries/popular"),
}

/**
 * A language pair as the Forum matches it: **direction does not count.** `EN-TR` finds English→
 * Turkish and Turkish→English alike, which is why a pair is stored as two codes rather than a
 * from/to, and why two pairs with the same languages in either order are the same pair.
 */
internal data class MarketplaceLanguagePair(val first: String, val second: String) {

    fun isSamePairAs(other: MarketplaceLanguagePair): Boolean =
        setOf(first, second) == setOf(other.first, other.second)
}

/**
 * What the user is filtering by. The limits are the backend's, and the client holds to them so a
 * 400 never happens: more than ten pairs or tags, or a publisher name of one or two characters,
 * are all refusals rather than empty results.
 */
internal data class MarketplaceListingFilter(
    val languagePairs: List<MarketplaceLanguagePair> = emptyList(),
    val tags: Set<String> = emptySet(),
    val publisherName: String? = null,
) {
    internal companion object {
        const val MAX_LANGUAGE_PAIRS = 10
        const val MAX_TAGS = 10

        /** Below this the server answers 400 rather than searching. */
        const val MIN_PUBLISHER_NAME_LENGTH = 3
        const val MAX_PUBLISHER_NAME_LENGTH = 255
    }
}

/** Someone else's dictionary, read from `ms_dictionary`. Read-only in every sense. */
internal data class SharedDictionary(
    val dictionaryId: String,
    val publisherId: String,
    val name: String,
    val fromLang: String,
    val toLang: String,
    val createdAt: Long,
)

/** One of its words. No level: on another user's word that value is the owner's, and arrives null. */
internal data class SharedWord(
    val wordId: String,
    val word: String,
    val wordMeta: String,
    val translation: String,
    val translationMeta: String,
)

/** What came of importing a listing. */
internal enum class ImportResult {
    IMPORTED,

    /** Made private, deleted, or its publisher left since the list was loaded. */
    UNAVAILABLE,

    /** The caller is not a member — membership may have changed on another device. */
    NOT_MEMBER,

    FAILED,
}
