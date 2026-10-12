package de.coldtea.verborum.feature.forum.ui.search

import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.localization.SupportedLanguage
import de.coldtea.verborum.feature.forum.domain.MarketplaceLanguagePair
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter

/**
 * The search screen's **draft**.
 *
 * A draft, not the live filter: Back changes nothing, and only Search hands it over. That is also
 * why it is initialised once, from the route — re-initialising on every recomposition would undo
 * the user's edits the moment the device rotated.
 */
internal data class ForumSearchUiState(
    val languagePairs: List<MarketplaceLanguagePair> = emptyList(),
    val tags: Set<String> = emptySet(),
    val publisherName: String = "",
    /** The two-step picker: null when closed, a language once the first has been chosen. */
    val isPickingPair: Boolean = false,
    val pickedFirstLanguage: SupportedLanguage? = null,
) {
    /** Trimmed, because that is what will be sent and what the length rule is about. */
    private val trimmedPublisher: String get() = publisherName.trim()

    /**
     * One or two characters is a 400 from the server, not a narrower search. Blank is no filter at
     * all, which is fine — so only a *short, non-empty* name is an error.
     */
    val isPublisherNameTooShort: Boolean
        get() = trimmedPublisher.length in 1 until MarketplaceListingFilter.MIN_PUBLISHER_NAME_LENGTH

    val canSearch: Boolean get() = !isPublisherNameTooShort

    val canAddLanguagePair: Boolean
        get() = languagePairs.size < MarketplaceListingFilter.MAX_LANGUAGE_PAIRS

    fun canAddTag(tag: String): Boolean =
        tag in tags || tags.size < MarketplaceListingFilter.MAX_TAGS

    fun toFilter() = MarketplaceListingFilter(
        languagePairs = languagePairs,
        tags = tags,
        publisherName = trimmedPublisher.takeIf(String::isNotEmpty),
    )
}

internal class ForumSearchViewModel(
    initialPairs: List<String>,
    initialTags: Set<String>,
    initialPublisherName: String?,
) : BaseViewModel<ForumSearchUiState, Nothing>(
    ForumSearchUiState(
        languagePairs = initialPairs.mapNotNull(::parsePair),
        tags = initialTags,
        publisherName = initialPublisherName.orEmpty(),
    ),
) {

    fun openLanguagePicker() = setState {
        copy(isPickingPair = true, pickedFirstLanguage = null)
    }

    fun dismissLanguagePicker() = setState {
        copy(isPickingPair = false, pickedFirstLanguage = null)
    }

    fun pickFirstLanguage(language: SupportedLanguage) = setState {
        copy(pickedFirstLanguage = language)
    }

    /**
     * Completes a pair, unless it would be invalid or a duplicate.
     *
     * Rejected rather than sent and refused: `EN-EN` is a 400, and the same two languages in either
     * order is the same pair the server already matches both ways — so adding it would widen
     * nothing while using up one of the ten slots.
     */
    fun pickSecondLanguage(language: SupportedLanguage) {
        val first = currentState.pickedFirstLanguage ?: return

        if (language == first) return

        val pair = MarketplaceLanguagePair(
            first = first.code.uppercase(),
            second = language.code.uppercase(),
        )

        setState {
            val isDuplicate = languagePairs.any { it.isSamePairAs(pair) }

            copy(
                languagePairs = if (isDuplicate || !canAddLanguagePair) {
                    languagePairs
                } else {
                    languagePairs + pair
                },
                isPickingPair = false,
                pickedFirstLanguage = null,
            )
        }
    }

    fun removeLanguagePair(pair: MarketplaceLanguagePair) = setState {
        copy(languagePairs = languagePairs.filterNot { it.isSamePairAs(pair) })
    }

    fun toggleTag(tag: String) = setState {
        when {
            tag in tags -> copy(tags = tags - tag)
            canAddTag(tag) -> copy(tags = tags + tag)
            else -> this
        }
    }

    fun changePublisherName(name: String) = setState {
        copy(publisherName = name.take(MarketplaceListingFilter.MAX_PUBLISHER_NAME_LENGTH))
    }
}

/** "EN-TR" back into a pair; null for anything else, which simply does not appear as a chip. */
private fun parsePair(encoded: String): MarketplaceLanguagePair? {
    val codes = encoded.split("-")
    if (codes.size != 2 || codes.any(String::isBlank)) return null

    return MarketplaceLanguagePair(first = codes[0], second = codes[1])
}
