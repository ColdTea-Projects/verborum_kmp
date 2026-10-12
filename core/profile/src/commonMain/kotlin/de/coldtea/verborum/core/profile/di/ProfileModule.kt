package de.coldtea.verborum.core.profile.di

import de.coldtea.verborum.core.network.ApiConfig
import de.coldtea.verborum.core.network.createHttpClient
import de.coldtea.verborum.core.profile.data.UserApi
import de.coldtea.verborum.core.profile.data.VaultApi
import de.coldtea.verborum.core.profile.domain.MarketplaceMembershipHooks
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.core.profile.domain.SessionProfileService
import de.coldtea.verborum.core.profile.domain.usecase.EnsureUserProfileUseCase
import de.coldtea.verborum.core.profile.domain.usecase.UpdateProfileInfoUseCase
import de.coldtea.verborum.core.profile.domain.usecase.GetSessionIdentityUseCase
import de.coldtea.verborum.core.profile.domain.usecase.getSessionIdentityUseCase
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** The named client for `ms_user`; the shell resolves it to reach the profile and vault endpoints. */
const val USER_HTTP_CLIENT: String = "userHttpClient"

/**
 * Wiring for the profile slice.
 *
 * Its own [HttpClient] because the base URL is stamped on by the client rather than written at each
 * call site, and ms_user answers on a different origin from ms_dictionary until the API gateway
 * lands. The bearer token and the refresh-on-401 behaviour are the same session as every other call.
 *
 * `getOrNull` for the membership hooks: the list is contributed by whichever feature owns
 * dictionaries, and `core:profile` must keep working — in a test, or before that feature is wired —
 * when nothing has contributed one.
 */
val profileModule: Module = module {
    single(named(USER_HTTP_CLIENT)) {
        val config = get<ApiConfig>()

        createHttpClient(
            baseUrl = config.userBaseUrl,
            enableLogging = config.enableLogging,
            tokenProvider = get(),
        )
    }

    // Published as a binding, not only used internally: the Forum needs the caller's own subject
    // to recognise their own listings, and reading the auth session directly from a feature would
    // put the token lifecycle in reach of a screen.
    factory<GetSessionIdentityUseCase> { getSessionIdentityUseCase(session = get()) }

    single { UserApi(client = get(named(USER_HTTP_CLIENT))) }
    single { VaultApi(client = get(named(USER_HTTP_CLIENT))) }

    single<ProfileService> {
        SessionProfileService(
            sessionIdentity = get(),
            ensureUserProfile = EnsureUserProfileUseCase(api = get()),
            updateProfileInfo = UpdateProfileInfoUseCase(api = get()),
            vaultApi = get(),
            membershipHooks = getOrNull<MarketplaceMembershipHooks>()?.hooks.orEmpty(),
        )
    }
}
