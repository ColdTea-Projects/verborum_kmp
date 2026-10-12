package de.coldtea.verborum.feature.forum.data

import de.coldtea.verborum.core.network.VerborumJson
import de.coldtea.verborum.feature.forum.domain.MarketplaceListing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Turning the wire into listings.
 *
 * Every value here was written by another user or computed by a service this client does not own,
 * so the conversion is where it stops being arbitrary: a rating outside the scale would draw six
 * stars, a repeated id crashes a keyed lazy list, and a blank publisher renders as "by ".
 */
class MarketplaceDtoTest {

    private fun page(json: String) =
        VerborumJson.decodeFromString<MarketplacePageResponse>(json).toPage()

    @Test
    fun `a duplicate id within a page is dropped`() {
        // The order is stable, but a listing published mid-scroll shifts everything after it.
        val result = page(
            """{"items":[{"dictionaryId":"a"},{"dictionaryId":"b"},{"dictionaryId":"a"}],""" +
                """"page":0,"size":15,"hasNext":true}""",
        )

        assertEquals(listOf("a", "b"), result.listings.map(MarketplaceListing::dictionaryId))
        assertTrue(result.hasNext)
    }

    @Test
    fun `a listing with no id is dropped rather than shown as a blank row`() {
        val result = page("""{"items":[{"name":"nameless"},{"dictionaryId":"a"}]}""")

        assertEquals(listOf("a"), result.listings.map(MarketplaceListing::dictionaryId))
    }

    @Test
    fun `an out-of-range rating is clamped to the scale`() {
        val result = page("""{"items":[{"dictionaryId":"a","ratingAverage":9.9,"ratingCount":-4}]}""")

        assertEquals(5.0, result.listings.single().ratingAverage)
        assertEquals(0, result.listings.single().ratingCount)
    }

    @Test
    fun `an unrated listing keeps a null average rather than becoming zero`() {
        val result = page("""{"items":[{"dictionaryId":"a","ratingCount":0}]}""")

        // Null is what makes the card show nothing; zero would draw five empty stars as a verdict.
        assertNull(result.listings.single().ratingAverage)
    }

    @Test
    fun `a blank publisher name becomes null`() {
        val result = page("""{"items":[{"dictionaryId":"a","publisherName":"   "}]}""")

        assertNull(result.listings.single().publisherName)
    }

    @Test
    fun `tags are lowercased and trimmed and de-duplicated`() {
        val result = page("""{"items":[{"dictionaryId":"a","tags":[" Travel ","TRAVEL","","food"]}]}""")

        assertEquals(listOf("travel", "food"), result.listings.single().tags)
    }

    @Test
    fun `language codes come through uppercase`() {
        val result = page("""{"items":[{"dictionaryId":"a","fromLang":"en","toLang":"de"}]}""")

        assertEquals("EN" to "DE", result.listings.single().let { it.fromLang to it.toLang })
    }

    @Test
    fun `an unparseable publish time reads as zero rather than failing the page`() {
        val result = page("""{"items":[{"dictionaryId":"a","publishedAt":"not a date"}]}""")

        assertEquals(0L, result.listings.single().publishedAt)
    }

    @Test
    fun `unknown fields are ignored so the service can add some`() {
        val result = page(
            """{"items":[{"dictionaryId":"a","somethingNew":true}],"page":0,"size":15,""" +
                """"hasNext":false,"total":99}""",
        )

        assertEquals(1, result.listings.size)
    }
}
