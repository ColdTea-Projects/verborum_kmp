package de.coldtea.verborum.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthConfigWebTest {

    @Test
    fun `keycloak is addressed on the app's own origin, so the exchange stays same-origin`() {
        assertEquals(
            "https://verborum.coldtea.de/auth/realms/verborum",
            issuerFor("https://verborum.coldtea.de"),
        )
    }

    @Test
    fun `development uses the same path, which the dev server proxies`() {
        // No localhost special case: the proxy is what makes dev match production, so a changed dev
        // port cannot reintroduce a cross-origin token call.
        assertEquals(
            "http://localhost:8280/auth/realms/verborum",
            issuerFor("http://localhost:8280"),
        )
    }

    @Test
    fun `the issuer never points at another origin`() {
        val origin = "http://localhost:8280"

        assertTrue(issuerFor(origin).startsWith("$origin/"))
    }
}
