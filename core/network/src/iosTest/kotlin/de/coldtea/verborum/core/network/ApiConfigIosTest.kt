package de.coldtea.verborum.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiConfigIosTest {

    @Test
    fun `a debug binary talks to the local gateway`() {
        // Tests run as a debug binary, which is what selects the local address
        assertEquals("http://localhost:8080", defaultApiConfig().baseUrl)
    }

    @Test
    fun `a debug binary logs the traffic - a shipped app must not`() {
        assertTrue(defaultApiConfig().enableLogging)
    }
}
