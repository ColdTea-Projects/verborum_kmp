package de.coldtea.verborum.feature.bibliotheca.common.data

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

/** The `words` endpoints of the dictionary service. Like dictionaries, they answer unenveloped. */
internal class WordApi(private val client: HttpClient) {

    suspend fun wordsOfDictionary(dictionaryId: String): Outcome<List<WordDto>> = plainApiCall {
        client.get("words/dictionary/$dictionaryId")
    }

    /**
     * Every word the user owns, in one request. That is what makes the dictionary list's word counts
     * affordable — the alternative is one request per dictionary on every list open.
     */
    suspend fun wordsOfUser(userId: String): Outcome<List<WordDto>> = plainApiCall {
        client.get("words/user/$userId")
    }

    suspend fun create(bundles: List<WordBundleRequest>): Outcome<Unit> = statusApiCall {
        client.post("words") {
            contentType(ContentType.Application.Json)
            setBody(bundles)
        }
    }

    /** Sends whole words, not a patch — the service replaces what it is given. */
    suspend fun update(bundles: List<WordBundleRequest>): Outcome<Unit> = statusApiCall {
        client.put("words") {
            contentType(ContentType.Application.Json)
            setBody(bundles)
        }
    }

    suspend fun delete(wordId: String): Outcome<Unit> = statusApiCall {
        client.delete("words/$wordId")
    }

    suspend fun deleteByDictionary(dictionaryId: String): Outcome<Unit> = statusApiCall {
        client.delete("words/dictionary/$dictionaryId")
    }
}
