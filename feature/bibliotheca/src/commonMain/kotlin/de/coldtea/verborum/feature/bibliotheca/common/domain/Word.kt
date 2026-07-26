package de.coldtea.verborum.feature.bibliotheca.common.domain

/**
 * A word as the app reasons about it. Shared by the whole feature: the dictionary list counts them,
 * the details screen lists them, and the practice screens will read them.
 *
 * [word] and [translation] are stored as a JSON array of alternatives (`["buy","purchase"]`), and
 * the meta blobs carry the language code and grammatical forms the create-word screen writes. Both
 * stay opaque here — see `WordSurfaces` for the display side.
 */
data class Word(
    val wordId: String,
    val dictionaryId: String,
    val word: String,
    val wordMeta: String,
    val translation: String,
    val translationMeta: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Practice progress, 0..7. Anything outside that range is not a level the app wrote. */
    val level: Int = 0,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
) {
    companion object {
        /** The practice ladder's top rung; the progress bar is drawn against it. */
        const val MAX_LEVEL = 7
    }
}
