package de.coldtea.verborum.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ApiConfigWebTest {

    @Test
    fun `the dev server origin talks to the local gateway directly`() {
        assertEquals("http://localhost:8080", apiConfigFor("http://localhost:8280").baseUrl)
    }

    @Test
    fun `the gateway host follows the page host`() {
        assertEquals("http://127.0.0.1:8080", apiConfigFor("http://127.0.0.1:8280").baseUrl)
    }

    @Test
    fun `an IPv6 loopback keeps its brackets`() {
        assertEquals("http://[::1]:8080", apiConfigFor("http://[::1]:8280").baseUrl)
    }

    @Test
    fun `a deployed origin keeps calling its own api path`() {
        assertEquals("https://verborum.coldtea.de/api", apiConfigFor("https://verborum.coldtea.de").baseUrl)
    }

    @Test
    fun `a look-alike host is not treated as local`() {
        // "localhost" inside a longer name is somebody else's machine
        assertEquals(
            "https://localhost.example.com/api",
            apiConfigFor("https://localhost.example.com").baseUrl,
        )
    }

    @Test
    fun `web never logs the traffic`() {
        assertFalse(apiConfigFor("http://localhost:8280").enableLogging)
    }
}
