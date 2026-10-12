package de.coldtea.verborum.feature.forum.ui.list

import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.core.localization.SupportedLanguage
import de.coldtea.verborum.core.localization.tagLabelOf
import de.coldtea.verborum.feature.forum.domain.MarketplaceLanguagePair
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter

/**
 * The active filter as removable chips.
 *
 * Shared by both platforms because what each chip *says* is the same question in a browser and on a
 * phone; only how they are laid out differs. Each carries the filter it would leave behind, so
 * removing one is a plain substitution rather than a per-chip handler.
 */
internal data class ForumFilterChip(
    val label: String,
    val removeDescription: String,
    val without: MarketplaceListingFilter,
    /**
     * The two language names, when this chip is a pair.
     *
     * Carried apart from [label] because the arrow between them is **drawn, not written**: the web
     * build's bundled faces have no `\u21C4`, so a chip that spelled it would show a tofu box in
     * the browser. [label] keeps the spelled form, which is what a screen reader should hear.
     */
    val languages: Pair<String, String>? = null,
)

internal fun MarketplaceListingFilter.toChips(strings: Strings): List<ForumFilterChip> = buildList {
    languagePairs.forEach { pair ->
        val label = strings.forumLanguagePairChip(
            strings.languageName(pair.first.lowercase()),
            strings.languageName(pair.second.lowercase()),
        )

        add(
            ForumFilterChip(
                label = label,
                removeDescription = strings.forumRemoveLanguagePair(label),
                without = copy(languagePairs = languagePairs.filterNot { it.isSamePairAs(pair) }),
                languages = strings.languageName(pair.first.lowercase()) to
                    strings.languageName(pair.second.lowercase()),
            ),
        )
    }

    if (tags.isNotEmpty()) {
        val label = strings.forumFilterTagsChip(
            tags.joinToString(", ") { tag -> tagLabelOf(tag, strings) },
        )

        add(
            ForumFilterChip(
                label = label,
                removeDescription = strings.forumRemoveLanguagePair(label),
                without = copy(tags = emptySet()),
            ),
        )
    }

    publisherName?.takeIf(String::isNotBlank)?.let { name ->
        val label = strings.forumFilterUserChip(name)

        add(
            ForumFilterChip(
                label = label,
                removeDescription = strings.forumRemoveLanguagePair(label),
                without = copy(publisherName = null),
            ),
        )
    }
}

/** "English ⇄ German", for a pair shown outside the chip row. */
internal fun MarketplaceLanguagePair.label(strings: Strings): String = strings.forumLanguagePairChip(
    strings.languageName(first.lowercase()),
    strings.languageName(second.lowercase()),
)

/** The pair as the two languages the picker deals in, or null if either code is unsupported. */
internal fun MarketplaceLanguagePair.toLanguages(): Pair<SupportedLanguage, SupportedLanguage>? {
    val first = SupportedLanguage.fromCode(first) ?: return null
    val second = SupportedLanguage.fromCode(second) ?: return null

    return first to second
}
