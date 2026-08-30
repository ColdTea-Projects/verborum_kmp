package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.logging.logger
import de.coldtea.verborum.core.network.VerborumJson
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
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

    private val log = logger("Auth")

    override suspend fun read(): AuthTokens? {
        migrateLegacyTokensIfPresent()
        return keychainRead()?.let(::decode)
    }

    override suspend fun write(tokens: AuthTokens) {
        val payload = CFBridgingRetain(VerborumJson.encodeToString(tokens).toNSData())

        try {
            // The Keychain has no upsert: an existing item must go before the new one is added.
            keychainDelete()

            val status = withQuery(
                kSecValueData to payload,
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            ) { query -> SecItemAdd(query, null) }

            // Not worth crashing over, but silence would be worse: the app goes on looking signed
            // in until the process ends, then finds the session it never persisted gone. The status
            // alone — the payload is the token.
            if (status != errSecSuccess) {
                log.w { "the Keychain refused the token item (status $status)" }
            }
        } finally {
            CFRelease(payload)
        }
    }

    override suspend fun clear() {
        keychainDelete()
        legacyDefaults.removeObjectForKey(LEGACY_DEFAULTS_KEY)
    }

    private fun keychainRead(): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(kSecReturnData to kCFBooleanTrue) { query ->
            SecItemCopyMatching(query, result.ptr)
        }

        if (status != errSecSuccess) return@memScoped null

        // CFBridgingRelease takes ownership of the copied data, so nothing leaks here.
        (CFBridgingRelease(result.value) as? NSData)?.toKotlinString()
    }

    private fun keychainDelete() {
        withQuery { query -> SecItemDelete(query) }
    }

    /**
     * Runs [block] against the query identifying this app's single token item — the service/account
     * pair, plus whatever [extra] the call adds — and tears the query down afterwards.
     *
     * The dictionary is created with no callbacks, so it never retains what it is given: each
     * `CFBridgingRetain` below has to be released by hand, and releasing the dictionary does not do
     * it. Anything owned elsewhere — the Security constants, and the payload [write] bridges — is
     * left alone here. Getting this wrong leaks once per call, and [read] runs on every request.
     */
    private fun <R> withQuery(
        vararg extra: Pair<COpaquePointer?, COpaquePointer?>,
        block: (CFMutableDictionaryRef) -> R,
    ): R {
        val service = CFBridgingRetain(SERVICE)
        val account = CFBridgingRetain(ACCOUNT)
        val query = requireNotNull(CFDictionaryCreateMutable(null, 0, null, null)) {
            "The Keychain query dictionary could not be allocated."
        }

        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service)
        CFDictionaryAddValue(query, kSecAttrAccount, account)
        extra.forEach { (key, value) -> CFDictionaryAddValue(query, key, value) }

        return try {
            block(query)
        } finally {
            CFRelease(query)
            CFRelease(service)
            CFRelease(account)
        }
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
    val status = bytes.usePinned { pinned ->
        SecRandomCopyBytes(kSecRandomDefault, size.toULong(), pinned.addressOf(0))
    }

    // Failing outright is the only safe answer. Ignoring the status leaves the array as it was
    // allocated — all zeroes — which is a fully predictable PKCE verifier and `state`, and those are
    // the two values the whole flow's security rests on.
    check(status == errSecSuccess) { "SecRandomCopyBytes failed with status $status." }

    return bytes
}
