package de.coldtea.verborum.feature.forum.domain

import de.coldtea.verborum.core.marketplace.DictionaryLanguages

/** Picks the language pairs a user works in most, to pre-fill the Forum's filter. */
internal object LanguagePairRanking {

    /**
     * Up to [limit] pairs, ranked by how many dictionaries use them, a tie going to the pair with
     * the most recent activity.
     *
     * **Direction does not count** in the ranking — DE→EN and EN→DE are one pair, which is how the
     * Forum's own filter matches — but each pair is *displayed* in the direction of its most
     * recently used dictionary, so the chip reads the way the user thinks of it. A dictionary with
     * the same language twice is ignored: the backend rejects `EN-EN` outright.
     */
    fun topPairs(dictionaries: List<DictionaryLanguages>, limit: Int): List<MarketplaceLanguagePair> =
        dictionaries
            .map { it.copy(fromLang = it.fromLang.uppercase(), toLang = it.toLang.uppercase()) }
            .filter { it.fromLang.isNotBlank() && it.toLang.isNotBlank() && it.fromLang != it.toLang }
            .groupBy { setOf(it.fromLang, it.toLang) }
            .values
            .sortedWith(
                compareByDescending<List<DictionaryLanguages>> { group -> group.size }
                    .thenByDescending { group -> group.maxOf(DictionaryLanguages::lastActivityAt) },
            )
            .take(limit)
            .map { group ->
                val latest = group.maxBy(DictionaryLanguages::lastActivityAt)
                MarketplaceLanguagePair(first = latest.fromLang, second = latest.toLang)
            }

    /** Three fills a row of chips without burying the list under its own filter. */
    const val DEFAULT_PAIR_LIMIT = 3
}
