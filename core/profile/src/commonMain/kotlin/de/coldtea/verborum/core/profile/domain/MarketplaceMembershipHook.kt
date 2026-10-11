package de.coldtea.verborum.core.profile.domain

/**
 * Notified when the user joins or leaves the Forum.
 *
 * The join and the leave are both server-side events with consequences elsewhere in the app —
 * joining makes every one of the user's dictionaries public, leaving makes them all private — and
 * the module that owns those dictionaries cannot be reached from here without pointing the module
 * graph the wrong way. So it registers a hook instead.
 *
 * A hook must be quick: it is called while the user waits for the profile screen to confirm the
 * change, so it schedules work rather than doing it inline.
 */
fun interface MarketplaceMembershipHook {
    suspend fun onMembershipChanged(isMember: Boolean)
}

/**
 * The registered hooks, as a type of their own rather than a bare `List<MarketplaceMembershipHook>`.
 *
 * Same reason as `PostLogoutHooks`, and the two together are the reason it matters: Koin keys a
 * definition by its `KClass`, and on Kotlin/JS and Wasm the generic argument is not part of that, so
 * two bare `List<…>` bindings in one graph are one key. Each registry gets its own type instead.
 */
class MarketplaceMembershipHooks(val hooks: List<MarketplaceMembershipHook> = emptyList())
