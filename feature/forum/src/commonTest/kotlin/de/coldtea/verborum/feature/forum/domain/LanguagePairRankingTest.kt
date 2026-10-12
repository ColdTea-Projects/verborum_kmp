package de.coldtea.verborum.feature.forum.domain

import de.coldtea.verborum.core.marketplace.DictionaryLanguages
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The default filter the Forum opens on.
 *
 * Direction is the whole subtlety: the server matches a pair **both ways**, so DE→EN and EN→DE are
 * one group when counting — but the chip is shown in the direction of the dictionary the user
 * touched last, because that is the one they think in.
 */
class LanguagePairRankingTest {

    private fun languages(from: String, to: String, at: Long) =
        DictionaryLanguages(fromLang = from, toLang = to, lastActivityAt = at)

    @Test
    fun `direction does not split a pair`() {
        val top = LanguagePairRanking.topPairs(
            listOf(
                languages("DE", "EN", at = 1),
                languages("EN", "DE", at = 2),
                languages("FR", "TR", at = 3),
            ),
            limit = 3,
        )

        // Three dictionaries, two pairs — and the DE/EN one leads on count despite being older.
        assertEquals(2, top.size)
        assertEquals(setOf("DE", "EN"), setOf(top[0].first, top[0].second))
    }

    @Test
    fun `the pair is shown in the direction of its most recent dictionary`() {
        val top = LanguagePairRanking.topPairs(
            listOf(languages("DE", "EN", at = 1), languages("EN", "DE", at = 5)),
            limit = 3,
        )

        assertEquals(MarketplaceLanguagePair("EN", "DE"), top.single())
    }

    @Test
    fun `a tie on count goes to the most recent activity`() {
        val top = LanguagePairRanking.topPairs(
            listOf(languages("DE", "EN", at = 10), languages("FR", "TR", at = 20)),
            limit = 3,
        )

        assertEquals(listOf("FR", "DE"), top.map { it.first })
    }

    @Test
    fun `a dictionary with one language twice is ignored`() {
        // `EN-EN` is a 400 from the server, so it never reaches a filter.
        val top = LanguagePairRanking.topPairs(
            listOf(languages("EN", "EN", at = 1), languages("en", "EN", at = 2)),
            limit = 3,
        )

        assertEquals(emptyList(), top)
    }

    @Test
    fun `blank codes are ignored`() {
        val top = LanguagePairRanking.topPairs(
            listOf(languages("", "EN", at = 1), languages("DE", "  ", at = 2)),
            limit = 3,
        )

        assertEquals(emptyList(), top)
    }

    @Test
    fun `codes are compared and sent uppercase`() {
        val top = LanguagePairRanking.topPairs(
            listOf(languages("de", "en", at = 1), languages("DE", "EN", at = 2)),
            limit = 3,
        )

        // One pair, not two: the wire's codes are uppercase, and case must not split a group.
        assertEquals(listOf(MarketplaceLanguagePair("DE", "EN")), top)
    }

    @Test
    fun `only the top few are taken`() {
        val top = LanguagePairRanking.topPairs(
            listOf(
                languages("DE", "EN", at = 1),
                languages("FR", "TR", at = 2),
                languages("ES", "IT", at = 3),
                languages("JA", "KO", at = 4),
            ),
            limit = 3,
        )

        assertEquals(3, top.size)
    }

    @Test
    fun `a user with no dictionaries gets no filter rather than an empty result`() {
        assertEquals(emptyList(), LanguagePairRanking.topPairs(emptyList(), limit = 3))
    }
}
