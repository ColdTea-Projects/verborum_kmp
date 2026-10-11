package de.coldtea.verborum.core.profile.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.network.plainApiCall
import de.coldtea.verborum.core.network.statusApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * The `ms_user` profile endpoints. Like `ms_dictionary`, this service answers with the payload
 * directly rather than the app's `Envelope`, so the calls go through [plainApiCall]; nothing
 * Ktor-shaped leaves this class.
 *
 * Statuses are the contract here, not an afterthought: **404 from [getMe] means "no profile yet"**
 * and is how the client learns to create one, and a 400 from [updateProfileInfo] carries the broken
 * rule as a sentence for the user. Both survive because the failed-status path reads the service's
 * error body.
 */
internal class UserApi(private val client: HttpClient) {

    suspend fun getMe(): Outcome<UserProfileResponse> = plainApiCall {
        client.get("users/me")
    }

    suspend fun createProfile(request: UserProfileRequest): Outcome<Unit> = statusApiCall {
        client.post("users/") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun updateProfileInfo(request: ProfileInfoRequest): Outcome<Unit> = statusApiCall {
        client.put("users/me/profile-info") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
