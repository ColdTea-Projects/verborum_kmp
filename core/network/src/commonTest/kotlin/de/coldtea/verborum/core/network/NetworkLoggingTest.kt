package de.coldtea.verborum.core.network

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestLogWriter
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.common.logging.initLogging
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalKermitApi::class)
class NetworkLoggingTest {

    private lateinit var writer: TestLogWriter

    @BeforeTest
    fun setUp() {
        writer = TestLogWriter(loggable = Severity.Verbose)
        initLogging(minSeverity = Severity.Verbose, logWriter = writer)
    }

    @Test
    fun `a failure is logged once as a warning`() {
        Outcome.Failure(VerborumError.Http(status = 500)).logFailure("/words")

        val entry = writer.logs.single()
        assertEquals(Severity.Warn, entry.severity)
        assertContains(entry.message, "/words")
        assertContains(entry.message, "500")
    }

    @Test
    fun `a success is not logged`() {
        Outcome.Success("body").logFailure("/words")

        assertTrue(writer.logs.isEmpty())
    }

    @Test
    fun `a failure without a known path still reports the error`() {
        Outcome.Failure(VerborumError.Unauthorized).logFailure()

        val entry = writer.logs.single()
        assertContains(entry.message, "Unauthorized")
    }

    @Test
    fun `the ktor adapter logs at debug so shipped builds stay silent`() {
        KtorKermitLogger.log("GET https://api.example.com/words")

        val entry = writer.logs.single()
        assertEquals(Severity.Debug, entry.severity)
        assertEquals("Verborum.Network", entry.tag)
    }
}
