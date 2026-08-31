package de.coldtea.verborum.core.common

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [isLocalDevelopmentOrigin], whose whole job is to be stricter than the substring check it
 * replaced — deciding whether the app talks to local services or to the deployed ones.
 */
class BrowserLocationTest {

    @Test
    fun `a development origin is local with or without a port`() {
        assertTrue(isLocalDevelopmentOrigin("http://localhost:8080"))
        assertTrue(isLocalDevelopmentOrigin("http://localhost"))
        assertTrue(isLocalDevelopmentOrigin("http://127.0.0.1:8080"))
        assertTrue(isLocalDevelopmentOrigin("https://127.0.0.1"))
    }

    @Test
    fun `an IPv6 loopback is local whether or not a port follows the brackets`() {
        // The regression: hunting for the last colon found one inside the address itself, so a
        // bracketed loopback with no port came back as `[:` and read as a remote host.
        assertTrue(isLocalDevelopmentOrigin("http://[::1]"))
        assertTrue(isLocalDevelopmentOrigin("http://[::1]:8080"))
    }

    @Test
    fun `a host that merely contains a local name is remote`() {
        // The trap this function exists for: a substring check hands somebody else's server the
        // treatment meant for a developer's machine.
        assertFalse(isLocalDevelopmentOrigin("https://localhost.example.com"))
        assertFalse(isLocalDevelopmentOrigin("https://not-localhost.dev"))
        assertFalse(isLocalDevelopmentOrigin("https://127.0.0.1.example.com"))
    }

    @Test
    fun `a deployed origin is remote`() {
        assertFalse(isLocalDevelopmentOrigin("https://verborum.coldtea.de"))
        assertFalse(isLocalDevelopmentOrigin("https://api.verborum.coldtea.de:443"))
    }

    @Test
    fun `an origin carrying a path is read by its host alone`() {
        assertTrue(isLocalDevelopmentOrigin("http://localhost:8080/auth/realms/verborum"))
        assertFalse(isLocalDevelopmentOrigin("https://verborum.coldtea.de/localhost"))
    }

    @Test
    fun `something that is not an origin is not local`() {
        assertFalse(isLocalDevelopmentOrigin(""))
        assertFalse(isLocalDevelopmentOrigin("localhost"))
        assertFalse(isLocalDevelopmentOrigin("gibberish"))
    }
}
