package de.coldtea.verborum.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiTimestampTest {

    @Test
    fun `reads an offset timestamp at the instant it names`() {
        // 2026-07-19T21:27:20.400+02:00 is 19:27:20.400 UTC.
        val millis = ApiTimestamp.parse("2026-07-19T21:27:20.400672+02:00")

        assertEquals(ApiTimestamp.parse("2026-07-19T19:27:20.400Z"), millis)
    }

    @Test
    fun `truncates sub-millisecond precision rather than misreading it`() {
        // The columns carry microseconds; reading .906614 as milliseconds would land minutes late.
        val withMicros = ApiTimestamp.parse("2026-07-19T17:01:20.906614Z")
        val withMillis = ApiTimestamp.parse("2026-07-19T17:01:20.906Z")

        assertEquals(withMillis, withMicros)
    }

    @Test
    fun `a value without an offset is read as UTC`() {
        assertEquals(
            ApiTimestamp.parse("2026-07-19T17:01:20.906Z"),
            ApiTimestamp.parse("2026-07-19T17:01:20.906614"),
        )
    }

    @Test
    fun `epoch millis on the wire are taken as they are`() {
        assertEquals(1_784_000_000_000L, ApiTimestamp.parse("1784000000000"))
    }

    @Test
    fun `anything unparseable is null rather than a wrong instant`() {
        assertNull(ApiTimestamp.parse(null))
        assertNull(ApiTimestamp.parse(""))
        assertNull(ApiTimestamp.parse("   "))
        assertNull(ApiTimestamp.parse("last tuesday"))
        assertNull(ApiTimestamp.parse("2026-07-19"))
    }

    @Test
    fun `formatting round-trips through parsing`() {
        val millis = 1_784_000_000_000L

        assertEquals(millis, ApiTimestamp.parse(ApiTimestamp.format(millis)))
    }
}
