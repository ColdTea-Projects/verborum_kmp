package de.coldtea.verborum.core.profile.domain

/**
 * The Forum terms the client presents, and the version it records when a user accepts them.
 *
 * The backend stores whatever version string it is sent and never interprets it, so the value is
 * ours to define — and it must stay **identical to the Android client's**, or one account would look
 * like it had accepted different terms depending on which device it signed in from. Bump it only
 * when the terms text itself changes.
 */
object MarketplaceTerms {

    const val VERSION: String = "2026-10-04"

    /** The backend rejects anything longer with a 400, so the field is capped before it is sent. */
    const val MAX_DISPLAY_NAME_LENGTH: Int = 255
}
