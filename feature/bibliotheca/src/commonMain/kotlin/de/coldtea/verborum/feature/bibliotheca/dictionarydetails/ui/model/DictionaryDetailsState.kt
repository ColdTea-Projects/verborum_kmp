package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model

/** What the details screen is showing. */
internal sealed interface DictionaryDetailsState {

    data object Loading : DictionaryDetailsState

    data object Failed : DictionaryDetailsState

    /**
     * The dictionary is gone — deleted here, or removed elsewhere and reconciled by a sync. The
     * screen leaves on this rather than rendering a header for something that no longer exists.
     */
    data object Deleted : DictionaryDetailsState

    data class Success(
        val name: String,
        val languagePair: String,
        val words: List<WordUi>,
        val tags: List<String> = emptyList(),
        /**
         * Self practice needs at least one word; a test additionally needs enough distinct entries to
         * build wrong answers from. Decided here so the screen only renders the outcome — and so the
         * reason a mode is unavailable can be explained rather than left as a dead button.
         */
        val canSelfPractice: Boolean = false,
        val canTest: Boolean = false,
    ) : DictionaryDetailsState
}

/** Below this many distinct words a multiple-choice test cannot offer plausible wrong answers. */
internal const val REQUIRED_WORDS_FOR_TEST = 4
