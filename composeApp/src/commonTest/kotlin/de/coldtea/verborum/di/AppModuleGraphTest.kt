package de.coldtea.verborum.di

import de.coldtea.verborum.core.auth.AuthConfig
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.AuthSession
import de.coldtea.verborum.core.auth.AuthorizationLauncher
import de.coldtea.verborum.core.auth.AuthorizationResult
import de.coldtea.verborum.core.auth.InMemoryTokenStorage
import de.coldtea.verborum.core.auth.PendingAuthorization
import de.coldtea.verborum.core.auth.PendingAuthorizationStore
import de.coldtea.verborum.core.auth.PostLogoutHooks
import de.coldtea.verborum.core.auth.TokenStorage
import de.coldtea.verborum.core.database.LocalCache
import de.coldtea.verborum.core.network.ApiConfig
import de.coldtea.verborum.core.profile.di.USER_HTTP_CLIENT
import de.coldtea.verborum.core.profile.di.profileModule
import de.coldtea.verborum.core.profile.domain.MarketplaceMembershipHooks
import de.coldtea.verborum.core.profile.domain.ProfileService
import io.ktor.client.HttpClient
import org.koin.core.Koin
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Builds the wiring the app starts with.
 *
 * The regression this exists for: signing out has to forget the profile, and the profile has to read
 * the session — wired carelessly that is a cycle, and Koin answers a cycle by recursing until the
 * runtime gives up ("too much recursion"). Nothing in a compile or a unit test sees it, because
 * every individual definition is fine. It appears only when the graph is actually built, which
 * without this test first happens on the user's screen.
 *
 * The platform leaves are substituted — token storage, the browser/ASWebAuthenticationSession
 * launcher, the settings store, and both sets of endpoints — because each of those reaches for a real
 * device or a real browser (`defaultApiConfig` and `defaultAuthConfig` both read `location` on web,
 * which does not exist under Node). What is under test is the shape of the graph between them, which
 * is where the cycle was.
 */
class AppModuleGraphTest {

    @Test
    fun `the core wiring resolves without walking into a cycle`() {
        val koin = graph()

        // The two ends of the loop. Between them they pull in the session, the token storage and all
        // three service clients, so resolving both walks nearly the whole of `coreModule`.
        assertNotNull(koin.get<AuthService>())
        assertNotNull(koin.get<ProfileService>())
    }

    @Test
    fun `one session and one client per service are shared by everything that asks`() {
        val koin = graph()

        // Singles, not factories: two `AuthSession`s would be two token lifecycles, and the mutex
        // that collapses a burst of parallel requests into one refresh would no longer be shared.
        assertSame(koin.get<AuthSession>(), koin.get<AuthSession>())
        assertSame(koin.get<HttpClient>(), koin.get<HttpClient>())

        // The dictionary client and the `ms_user` client are different objects on purpose: each
        // carries its own base URL, which is what sends a profile call to :8086 and not to :8085.
        assertTrue(koin.get<HttpClient>() !== koin.get<HttpClient>(named(USER_HTTP_CLIENT)))
    }

    @Test
    fun `signing out has something registered to forget`() {
        val koin = graph()

        // The hook list is what makes the device safe to hand to the next person; an empty one would
        // leave the previous account's name and membership in front of them.
        assertNotNull(koin.get<PostLogoutHooks>().hooks.firstOrNull())
    }

    @Test
    fun `each hook registry has a key of its own`() {
        val koin = graph()

        // Koin keys on `KClass`, and on JS and Wasm a generic argument is not part of it — so two
        // registries bound as bare `List<…>` would be one definition, and a lookup for either would
        // answer with whichever was declared last. That is not a style point: `profileModule` asks
        // for the membership hooks with `getOrNull`, which would then hand back the post-logout list
        // and re-enter the service being built, recursing until the runtime gives up.
        assertNotNull(koin.get<PostLogoutHooks>())
        assertTrue(koin.getOrNull<MarketplaceMembershipHooks>() == null)
    }

    /**
     * The real `coreModule` and `profileModule`, with the platform leaves overridden. Declared last
     * so the substitutes win.
     */
    private fun graph(): Koin = koinApplication {
        modules(coreModule, profileModule, platformLeaves)
    }.koin
}

private val platformLeaves = module {
    single<AuthConfig> {
        AuthConfig(
            issuer = "https://auth.test/realms/verborum",
            clientId = "verborum-app",
            redirectUri = "https://app.test/",
        )
    }
    single<ApiConfig> {
        ApiConfig(
            baseUrl = "https://dictionary.test",
            userBaseUrl = "https://user.test",
            marketplaceBaseUrl = "https://marketplace.test",
        )
    }
    single<TokenStorage> { InMemoryTokenStorage() }
    single<LocalCache> { NoOpLocalCache }
    single<AuthorizationLauncher> { NoOpAuthorizationLauncher }
    single<PendingAuthorizationStore> { NoOpPendingAuthorizationStore }
}

private object NoOpLocalCache : LocalCache {
    override suspend fun put(key: String, value: String) = Unit
    override suspend fun get(key: String): String? = null
    override suspend fun remove(key: String) = Unit
    override suspend fun clear() = Unit
}

private object NoOpAuthorizationLauncher : AuthorizationLauncher {
    override suspend fun authorize(url: String, redirectUri: String): AuthorizationResult =
        AuthorizationResult.Cancelled

    override fun consumeRedirect(): AuthorizationResult? = null
}

private object NoOpPendingAuthorizationStore : PendingAuthorizationStore {
    override fun save(pending: PendingAuthorization) = Unit
    override fun consume(): PendingAuthorization? = null
}
