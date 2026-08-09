package de.coldtea.verborum.core.common.logging

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestLogWriter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalKermitApi::class)
class LoggingTest {

    private fun writer() = TestLogWriter(loggable = Severity.Verbose)

    @Test
    fun `messages below the configured floor are not written`() {
        val writer = writer()
        initLogging(minSeverity = Severity.Warn, logWriter = writer)

        logger("Test").d { "chatter" }
        logger("Test").w { "trouble" }

        assertEquals(1, writer.logs.size)
        assertEquals("trouble", writer.logs.single().message)
    }

    @Test
    fun `every area logs under the app tag`() {
        val writer = writer()
        initLogging(minSeverity = Severity.Verbose, logWriter = writer)

        logger("Network").i { "up" }

        assertEquals("$LOG_TAG.Network", writer.logs.single().tag)
    }

    @Test
    fun `an untagged logger falls back to the app tag`() {
        val writer = writer()
        initLogging(minSeverity = Severity.Verbose, logWriter = writer)

        Logger.i { "up" }

        assertEquals(LOG_TAG, writer.logs.single().tag)
    }

    @Test
    fun `redacted never reveals the secret`() {
        val token = "eyJhbGciOiJIUzI1NiJ9.payload.signature"

        val rendered = redacted(token)

        assertFalse(rendered.contains(token))
        assertContains(rendered, token.length.toString())
        assertEquals("absent", redacted(null))
        assertEquals("empty", redacted(""))
    }
}
