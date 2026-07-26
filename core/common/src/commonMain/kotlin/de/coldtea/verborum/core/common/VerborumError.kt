package de.coldtea.verborum.core.common

/**
 * The platform-independent error model. Transport-level failures are mapped into
 * this type by `core:network`, so features never see Ktor or HTTP details.
 */
sealed interface VerborumError {

    /** The request never reached the backend (offline, DNS, TLS, timeout). */
    data class Network(val cause: String? = null) : VerborumError

    /** The backend answered, but with a non-2xx status. */
    data class Http(
        val status: Int,
        val code: String? = null,
        val message: String? = null,
    ) : VerborumError

    /** The response body could not be decoded into the expected shape. */
    data class Serialization(val cause: String? = null) : VerborumError

    /** No valid credentials, or the refresh flow failed. */
    data object Unauthorized : VerborumError

    data class Unknown(val cause: String? = null) : VerborumError
}
