package de.coldtea.verborum.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {

    private val now = 1_800_000_000_000L

    private fun agoBy(millis: Long) = RelativeTime.ago(now - millis, now)

    @Test
    fun `anything under a minute is just now`() {
        assertEquals("just now", agoBy(0))
        assertEquals("just now", agoBy(59_000))
    }

    @Test
    fun `picks the largest unit that fits`() {
        assertEquals("1 minute ago", agoBy(60_000))
        assertEquals("59 minutes ago", agoBy(59 * 60_000L))
        assertEquals("1 hour ago", agoBy(60 * 60_000L))
        assertEquals("3 days ago", agoBy(3 * 24 * 60 * 60_000L))
        assertEquals("2 months ago", agoBy(60 * 24 * 60 * 60_000L))
        assertEquals("1 year ago", agoBy(400 * 24 * 60 * 60_000L))
    }

    @Test
    fun `a timestamp in the future reads as just now, not as a negative age`() {
        // Server and device clocks disagreeing must not produce "in -3 days".
        assertEquals("just now", RelativeTime.ago(now + 60_000, now))
    }

    @Test
    fun `pluralize agrees with its count`() {
        assertEquals("1 word", pluralize(1, "word"))
        assertEquals("0 words", pluralize(0, "word"))
        assertEquals("12 words", pluralize(12, "word"))
    }
}
