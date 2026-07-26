package de.coldtea.verborum.core.auth

/** Cryptographically secure random bytes from the platform RNG. */
internal expect fun secureRandomBytes(size: Int): ByteArray

/** A PKCE verifier/challenge pair (RFC 7636). Only the challenge leaves the device first. */
data class PkceChallenge(
    val codeVerifier: String,
    val codeChallenge: String,
) {
    val codeChallengeMethod: String get() = "S256"
}

object Pkce {

    /** RFC 7636 allows 43–128 characters; 32 random bytes encode to 43. */
    private const val VERIFIER_BYTES = 32

    fun generate(): PkceChallenge {
        val verifier = base64UrlEncode(secureRandomBytes(VERIFIER_BYTES))
        return PkceChallenge(
            codeVerifier = verifier,
            codeChallenge = challengeOf(verifier),
        )
    }

    /** `BASE64URL(SHA256(ASCII(code_verifier)))`, per RFC 7636 §4.2. */
    fun challengeOf(codeVerifier: String): String =
        base64UrlEncode(Sha256.digest(codeVerifier.encodeToByteArray()))

    /** Base64url without padding, as every PKCE field requires. */
    internal fun base64UrlEncode(bytes: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val out = StringBuilder((bytes.size + 2) / 3 * 4)

        var index = 0
        while (index + 2 < bytes.size) {
            val chunk = ((bytes[index].toInt() and 0xff) shl 16) or
                ((bytes[index + 1].toInt() and 0xff) shl 8) or
                (bytes[index + 2].toInt() and 0xff)
            out.append(alphabet[(chunk ushr 18) and 0x3f])
            out.append(alphabet[(chunk ushr 12) and 0x3f])
            out.append(alphabet[(chunk ushr 6) and 0x3f])
            out.append(alphabet[chunk and 0x3f])
            index += 3
        }

        when (bytes.size - index) {
            1 -> {
                val chunk = (bytes[index].toInt() and 0xff) shl 16
                out.append(alphabet[(chunk ushr 18) and 0x3f])
                out.append(alphabet[(chunk ushr 12) and 0x3f])
            }

            2 -> {
                val chunk = ((bytes[index].toInt() and 0xff) shl 16) or
                    ((bytes[index + 1].toInt() and 0xff) shl 8)
                out.append(alphabet[(chunk ushr 18) and 0x3f])
                out.append(alphabet[(chunk ushr 12) and 0x3f])
                out.append(alphabet[(chunk ushr 6) and 0x3f])
            }
        }

        return out.toString()
    }
}
