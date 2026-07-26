package de.coldtea.verborum.core.common

/**
 * The result type every repository and use case returns. Named [Outcome] rather
 * than `Result` so it never collides with `kotlin.Result` at a call site.
 */
sealed interface Outcome<out T> {

    data class Success<out T>(val data: T) : Outcome<T>

    data class Failure(val error: VerborumError) : Outcome<Nothing>

    data object Loading : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(data))
    is Outcome.Failure -> this
    Outcome.Loading -> Outcome.Loading
}

inline fun <T> Outcome<T>.onSuccess(action: (T) -> Unit): Outcome<T> = apply {
    if (this is Outcome.Success) action(data)
}

inline fun <T> Outcome<T>.onFailure(action: (VerborumError) -> Unit): Outcome<T> = apply {
    if (this is Outcome.Failure) action(error)
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.data
