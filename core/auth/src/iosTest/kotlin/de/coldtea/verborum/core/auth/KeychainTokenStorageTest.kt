package de.coldtea.verborum.core.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Covers the half of the Keychain-backed storage a test binary can reach: the Core Foundation
 * ownership around every query.
 *
 * It deliberately does not assert a round trip. A Kotlin/Native test binary is not an app bundle and
 * carries no keychain entitlement, so the simulator refuses to store the item and every read comes
 * back empty — with the previous implementation exactly as with this one. A `write` then `read`
 * assertion would fail here for a reason that has nothing to do with this code, so the round trip is
 * verified by running the app itself, as the `ios-security` skill asks.
 *
 * What does run here is the part no compiler checks. Each call bridges the service and account
 * strings into a dictionary created with no callbacks — it retains nothing it is given — so every
 * `CFBridgingRetain` has to be released by hand, and releasing the dictionary does not do it. One
 * release too many aborts the process rather than failing an assertion, which makes the loop below
 * the assertion: reaching the end of it means the retains and releases balanced several hundred
 * times over, across all three operations.
 */
class KeychainTokenStorageTest {

    private val storage = createTokenStorage()

    private val tokens = AuthTokens(
        accessToken = "access-1",
        refreshToken = "refresh-1",
        expiresAtEpochSeconds = 1_700_000_000L,
        idToken = "id-1",
    )

    @AfterTest
    fun cleanUp() = runTest { storage.clear() }

    @Test
    fun `every query balances its retains against its releases`() = runTest {
        repeat(100) {
            storage.write(tokens)
            storage.read()
            storage.clear()
        }
    }

    @Test
    fun `an absent item reads as null rather than as a failure`() = runTest {
        storage.clear()

        assertNull(storage.read())
    }
}
