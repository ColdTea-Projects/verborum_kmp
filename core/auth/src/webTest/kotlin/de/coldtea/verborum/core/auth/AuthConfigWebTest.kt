package de.coldtea.verborum.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthConfigWebTest {

    @Test
    fun `a localhost origin talks to the local Keycloak, which the dev server does not proxy`() {
        assertEquals(
            "http://localhost:8180/realms/verborum",
            issuerFor("http://localhost:8080"),
        )
        assertEquals("http://localhost:8180/realms/verborum", issuerFor("http://127.0.0.1:8080"))
    }

    @Test
    fun `a deployed origin stays same-origin behind the proxy`() {
        assertEquals(
            "https://verborum.coldtea.de/auth/realms/verborum",
            issuerFor("https://verborum.coldtea.de"),
        )
    }

    @Test
    fun `a host that merely looks local is treated as remote`() {
        // A substring check would point this origin's users at a developer's machine.
        assertEquals(
            "https://localhost.example.com/auth/realms/verborum",
            issuerFor("https://localhost.example.com"),
        )
    }
}
