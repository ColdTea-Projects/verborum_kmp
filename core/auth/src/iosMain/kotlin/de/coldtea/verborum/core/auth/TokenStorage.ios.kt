package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.network.VerborumJson
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSUserDefaults
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

private const val TOKENS_KEY = "de.coldtea.verborum.auth.tokens"

actual fun createTokenStorage(): TokenStorage = UserDefaultsTokenStorage()

private class UserDefaultsTokenStorage(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : TokenStorage {

    override suspend fun read(): AuthTokens? =
        defaults.stringForKey(TOKENS_KEY)?.let { raw ->
            runCatching { VerborumJson.decodeFromString<AuthTokens>(raw) }.getOrNull()
        }

    override suspend fun write(tokens: AuthTokens) {
        defaults.setObject(VerborumJson.encodeToString(tokens), TOKENS_KEY)
    }

    override suspend fun clear() {
        defaults.removeObjectForKey(TOKENS_KEY)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun secureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        SecRandomCopyBytes(kSecRandomDefault, size.toULong(), pinned.addressOf(0))
    }
    return bytes
}
