package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model

/** What the details screen is showing. */
internal sealed interface DictionaryDetailsState {

    data object Loading : DictionaryDetailsState

    data object Failed : DictionaryDetailsState

    /**
     * The dictionary is gone — deleted here, removed elsewhere and reconciled by a sync,
     * unsubscribed from, or hidden because the user left the Forum. The screen leaves on this rather
     * than rendering a header for something that is no longer there.
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
        /**
         * Downloaded from the Forum, and therefore read-only: no add-word button, no editing or
         * deleting words, no sharing switch, and Unsubscribe instead of Delete. One flag rather than
         * several because the whole screen changes together, and a screen that got *some* of it
         * right would offer the user writes that answer 403.
         */
        val isImported: Boolean = false,
        /** Only known for a dictionary this device downloaded; drives the "by …" subtitle. */
        val publisherName: String? = null,
        /** The sharing switch belongs to members, and only for their own dictionaries. */
        val isSharingVisible: Boolean = false,
        val isShared: Boolean = false,
        /**
         * The member's only shared dictionary. The switch stays on and disabled: the server will not
         * let them unshare it, and a switch that moved and sprang back would read as a bug.
         */
        val isLastShared: Boolean = false,
        /** A sharing request is in flight; the switch is disabled until it settles. */
        val isUpdatingSharing: Boolean = false,
        /** Confirmation before unsharing — people who downloaded it lose access. */
        val isUnshareConfirmationShown: Boolean = false,
        /** Confirmation before removing a download. */
        val isUnsubscribeConfirmationShown: Boolean = false,
        /**
         * The user's own rating of this download, and whether to offer the control at all.
         *
         * Hidden until the rating is actually known: empty stars shown to someone who is merely
         * offline would tell them they had not rated something they had.
         */
        val isRatingVisible: Boolean = false,
        val myRating: Int? = null,
        val isRatingUpdating: Boolean = false,
    ) : DictionaryDetailsState {

        /** On and locked, or in flight: either way the user cannot move it right now. */
        val canToggleSharing: Boolean get() = !isUpdatingSharing && !(isShared && isLastShared)
    }
}

/** Below this many distinct words a multiple-choice test cannot offer plausible wrong answers. */
internal const val REQUIRED_WORDS_FOR_TEST = 4
