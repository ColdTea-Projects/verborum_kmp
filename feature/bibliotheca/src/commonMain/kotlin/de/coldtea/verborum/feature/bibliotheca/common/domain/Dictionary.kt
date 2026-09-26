package de.coldtea.verborum.feature.bibliotheca.common.domain


/**
 * A dictionary as the app reasons about it: the API's shape mapped onto app types, with timestamps
 * as epoch millis rather than ISO-8601 strings.
 *
 * [isSynced] and [isDeleted] carry the offline-first bookkeeping the Android app keeps in its local
 * database. Here they mark rows the server has not confirmed yet — see `DictionaryStore`.
 */
data class Dictionary(
    val dictionaryId: String,
    val userId: String,
    val name: String,
    val isPublic: Boolean,
    val fromLang: String,
    val toLang: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    /**
     * Tag codes. The API keeps tags in a sub-resource of their own, so they never come with the
     * dictionary itself: they are fetched and written separately, and a row read straight from a
     * dictionary pull carries whatever the device already knew.
     */
    val tags: List<String> = emptyList(),
)
