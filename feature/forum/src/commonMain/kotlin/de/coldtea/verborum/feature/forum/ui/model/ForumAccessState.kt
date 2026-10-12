package de.coldtea.verborum.feature.forum.ui.model

/**
 * Whether this user may use the Forum at all.
 *
 * Derived from the session profile rather than from a 403, deliberately: the server enforces the
 * same gate, but letting the user hit it means showing them an error where an explanation belongs.
 * The server stays the authority — this only decides what to put on screen.
 */
internal enum class ForumAccessState {
    /** No profile yet, and no failure either. */
    LOADING,

    MEMBER,

    /** A profile that has not accepted the terms, or has withdrawn. */
    NOT_MEMBER,

    /** The profile could not be read **and** none was cached. A retry, not a verdict. */
    FAILED,
}
