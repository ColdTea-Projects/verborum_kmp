package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.network.VerborumJson
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecRandomDefault
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val SERVICE = "de.coldtea.verborum.auth"
private const val ACCOUNT = "tokens"

/** The legacy plaintext location this implementation migrates away from. */
private const val LEGACY_DEFAULTS_KEY = "de.coldtea.verborum.auth.tokens"

actual fun createTokenStorage(): TokenStorage = KeychainTokenStorage()

/**
 * Refresh tokens live in the Keychain, not `NSUserDefaults` — a plist in the app container is
 * readable from a filesystem dump and lands in unencrypted backups.
 *
 * `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`: `…ThisDeviceOnly` keeps the item out of
 * backups and off the user's other devices, while `AfterFirstUnlock` still allows a token refresh
 * behind a locked screen, which a background sync needs.
 */
@OptIn(ExperimentalForeignApi::class)
private class KeychainTokenStorage(
    private val legacyDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : TokenStorage {

    override suspend fun read(): AuthTokens? {
        migrateLegacyTokensIfPresent()
        return keychainRead()?.let(::decode)
    }

    override suspend fun write(tokens: AuthTokens) {
        val payload = VerborumJson.encodeToString(tokens)

        // The Keychain has no upsert: an existing item must go before the new one is added.
        keychainDelete()

        val query = newQuery()
        CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(payload.toNSData()))
        CFDictionaryAddValue(
            query,
            kSecAttrAccessible,
            kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        )
        SecItemAdd(query as CFDictionaryRef, null)
        CFRelease(query)
    }

    override suspend fun clear() {
        keychainDelete()
        legacyDefaults.removeObjectForKey(LEGACY_DEFAULTS_KEY)
    }

    private fun keychainRead(): String? = memScoped {
        val query = newQuery()
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)

        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
        CFRelease(query)

        if (status != errSecSuccess) return@memScoped null

        // CFBridgingRelease takes ownership of the copied data, so nothing leaks here.
        (CFBridgingRelease(result.value) as? NSData)?.toKotlinString()
    }

    private fun keychainDelete() {
        val query = newQuery()
        SecItemDelete(query as CFDictionaryRef)
        CFRelease(query)
    }

    /** The service/account pair identifying this app's single token item. */
    private fun newQuery(): CFMutableDictionaryRef {
        val query = requireNotNull(CFDictionaryCreateMutable(null, 0, null, null)) {
            "The Keychain query dictionary could not be allocated."
        }
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE as NSString))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(ACCOUNT as NSString))
        return query
    }

    /**
     * Moves a payload written by the previous `NSUserDefaults` implementation into the Keychain and
     * deletes the plaintext copy, so an upgrading user is neither signed out nor left with a
     * readable token on disk.
     */
    private suspend fun migrateLegacyTokensIfPresent() {
        val legacy = legacyDefaults.stringForKey(LEGACY_DEFAULTS_KEY) ?: return

        decode(legacy)?.let { tokens -> write(tokens) }
        legacyDefaults.removeObjectForKey(LEGACY_DEFAULTS_KEY)
    }

    private fun decode(raw: String): AuthTokens? =
        runCatching { VerborumJson.decodeFromString<AuthTokens>(raw) }.getOrNull()
}

@OptIn(ExperimentalForeignApi::class)
private fun String.toNSData(): NSData =
    (this as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: NSData()

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toKotlinString(): String? =
    NSString.create(data = this, encoding = NSUTF8StringEncoding) as String?

@OptIn(ExperimentalForeignApi::class)
internal actual fun secureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        SecRandomCopyBytes(kSecRandomDefault, size.toULong(), pinned.addressOf(0))
    }
    return bytes
}
