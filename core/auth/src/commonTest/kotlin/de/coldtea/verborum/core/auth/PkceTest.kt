package de.coldtea.verborum.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class PkceTest {

    @Test
    fun `challenge matches the RFC 7636 appendix B vector`() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challengeOf("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test
    fun `sha256 matches the abc vector`() {
        val hex = Sha256.digest("abc".encodeToByteArray())
            .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex)
    }

    @Test
    fun `generated verifiers are 43 characters of base64url`() {
        val challenge = Pkce.generate()

        assertEquals(43, challenge.codeVerifier.length)
        assertEquals("S256", challenge.codeChallengeMethod)
        assertEquals(challenge.codeChallenge, Pkce.challengeOf(challenge.codeVerifier))
    }
}
