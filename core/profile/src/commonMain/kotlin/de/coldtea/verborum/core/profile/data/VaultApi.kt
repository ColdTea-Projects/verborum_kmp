package de.coldtea.verborum.core.profile.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.core.network.asPathSegment
import de.coldtea.verborum.core.network.plainApiCall
import de.coldtea.verborum.core.network.statusApiCall
import de.coldtea.verborum.core.profile.domain.VaultEntry
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get

/**
 * The vault: which dictionaries this user has downloaded from the Forum. Entries are references to
 * other people's dictionaries, so nothing here is content — only ids and when they arrived.
 *
 * The path takes **ms_user's own id** (`id` from `users/me`), never the token's subject. Mixing the
 * two is the single easiest mistake to make against this backend, which is why the parameter is
 * named for where it comes from.
 */
internal class VaultApi(private val client: HttpClient) {

    suspend fun vaultOf(userId: String): Outcome<List<VaultEntry>> =
        plainApiCall<List<VaultEntryResponse>> {
            client.get("users/${userId.asPathSegment()}/vault")
        }.map { entries -> entries.map(VaultEntryResponse::toVaultEntry) }

    /** 200 also when the entry was already gone, so a repeat removal is a success. */
    suspend fun removeFromVault(userId: String, dictionaryId: String): Outcome<Unit> = statusApiCall {
        client.delete("users/${userId.asPathSegment()}/vault/${dictionaryId.asPathSegment()}")
    }
}
