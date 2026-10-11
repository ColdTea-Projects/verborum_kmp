package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.logging.logger
import kotlinx.coroutines.CancellationException

/**
 * Something that has to be forgotten when a session ends.
 *
 * The device is shared: the next person to sign in must never see the previous account's profile,
 * downloaded dictionaries or cached library. Rather than letting `core:auth` know about every store
 * there is — which would point the module graph the wrong way — each owner registers a hook and
 * `AuthService` runs the list.
 *
 * A hook must be quick and must not need the network: it runs while the user is watching the app
 * return to the login wall, and the tokens are already gone by then.
 */
fun interface PostLogoutHook {
    suspend fun onSignedOut()
}

/**
 * The registered hooks, as a type of their own rather than a bare `List<PostLogoutHook>`.
 *
 * Not cosmetic. Koin keys a definition by its `KClass`, and on Kotlin/JS and Wasm a generic
 * argument is not part of that: `List<PostLogoutHook>` and `List<AnythingElse>` are the *same* key.
 * Bound as a bare list, this registry answers every other list lookup in the graph too — which is
 * how a `getOrNull` for an unrelated hook list ends up resolving this one, re-entering whatever
 * built it, and recursing until the runtime gives up. A wrapper gives it a key of its own, and the
 * compiler enforces it.
 */
class PostLogoutHooks(val hooks: List<PostLogoutHook> = emptyList())

/**
 * Runs every hook, each isolated: one store failing to clear must not leave the rest holding the
 * previous account's data, which is the whole reason the wipe exists.
 */
internal suspend fun List<PostLogoutHook>.runAll() {
    val log = logger("Auth")

    forEach { hook ->
        try {
            hook.onSignedOut()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            // The class name only: what a store failed to clear is a diagnostic, its contents are not.
            log.w { "post-logout hook ${hook::class.simpleName} failed: ${throwable::class.simpleName}" }
        }
    }
}
