package de.coldtea.verborum.feature.bibliotheca.dictionarylist.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.network.plainApiCall
import de.coldtea.verborum.core.network.statusApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * The `ms_dictionary` endpoints. This service answers with the payload directly rather than the
 * app's `Envelope`, so the calls go through [plainApiCall]; nothing Ktor-shaped leaves this class.
 */
internal class DictionaryApi(private val client: HttpClient) {

    suspend fun dictionariesOf(userId: String): Outcome<List<DictionaryDto>> = plainApiCall {
        client.get("dictionaries/$userId")
    }

    suspend fun create(request: DictionaryRequest): Outcome<Unit> = statusApiCall {
        client.post("dictionaries/") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun update(request: DictionaryRequest): Outcome<Unit> = statusApiCall {
        client.put("dictionaries/") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun delete(dictionaryId: String): Outcome<Unit> = statusApiCall {
        client.delete("dictionaries/$dictionaryId")
    }
}
