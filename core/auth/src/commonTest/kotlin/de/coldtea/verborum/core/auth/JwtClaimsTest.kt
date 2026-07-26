package de.coldtea.verborum.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtClaimsTest {

    @Test
    fun `reads the subject from the access token and the profile from the id token`() {
        val accessToken = jwtWithClaims("""{"sub":"user-42","typ":"Bearer"}""")
        val idToken = jwtWithClaims("""{"email":"discipulus@example.test","name":"Marcus"}""")

        val identity = JwtClaims.identityOf(accessToken, idToken)

        assertEquals("user-42", identity?.subject)
        assertEquals("discipulus@example.test", identity?.email)
        assertEquals("Marcus", identity?.displayName)
    }

    @Test
    fun `falls back to preferred_username when the name claim is absent`() {
        val identity = JwtClaims.identityOf(
            accessToken = jwtWithClaims("""{"sub":"user-42"}"""),
            idToken = jwtWithClaims("""{"preferred_username":"marcus"}"""),
        )

        assertEquals("marcus", identity?.displayName)
    }

    @Test
    fun `an id token is optional`() {
        val identity = JwtClaims.identityOf(jwtWithClaims("""{"sub":"user-42"}"""))

        assertEquals("user-42", identity?.subject)
        assertNull(identity?.email)
        assertNull(identity?.displayName)
    }

    @Test
    fun `anything that is not a decodable jwt with a subject yields no identity`() {
        assertNull(JwtClaims.identityOf(null))
        assertNull(JwtClaims.identityOf(""))
        assertNull(JwtClaims.identityOf("not-a-jwt"))
        assertNull(JwtClaims.identityOf("header.@@@not-base64@@@.signature"))
        // Decodable, but carries no subject — so there is no identity to report.
        assertNull(JwtClaims.identityOf(jwtWithClaims("""{"email":"x@example.test"}""")))
    }
}
