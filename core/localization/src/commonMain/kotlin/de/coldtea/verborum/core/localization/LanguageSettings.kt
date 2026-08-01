package de.coldtea.verborum.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The language the interface speaks, and the one place that decides it.
 *
 * The platform's own locale is the default, because the user has already told their device what
 * language they read and asking a second time is a worse answer than reading the first one. An
 * explicit choice in Options overrides it and is remembered.
 */
class LanguageSettings(
    private val storage: LanguageStorage = createLanguageStorage(),
    private val platformLanguage: () -> String? = ::platformLanguageTag,
) {
    private val _language = MutableStateFlow(resolve())

    val language: StateFlow<UiLanguage> = _language.asStateFlow()

    private val _chosen = MutableStateFlow(UiLanguage.fromTag(storage.read()))

    /**
     * The language explicitly picked, or null while the app is following the device — which is the
     * state a fresh install is in, and the one the picker shows as "system language".
     */
    val chosen: StateFlow<UiLanguage?> = _chosen.asStateFlow()

    fun choose(language: UiLanguage) {
        storage.write(language.code)
        _chosen.value = language
        _language.value = language
    }

    /** Hands the interface back to the device's own setting. */
    fun followDevice() {
        storage.clear()
        _chosen.value = null
        _language.value = resolve()
    }

    private fun resolve(): UiLanguage =
        UiLanguage.fromTag(storage.read())
            ?: UiLanguage.fromTag(platformLanguage())
            ?: UiLanguage.Default
}

/** Where the chosen language is kept between launches. Not a secret, so ordinary storage will do. */
interface LanguageStorage {
    fun read(): String?
    fun write(code: String)
    fun clear()
}

internal expect fun createLanguageStorage(): LanguageStorage

/**
 * The device's language tag — "de-AT", "ja-JP" — or null where the platform will not say.
 *
 * Read from the browser or the operating system, never from the network. An IP address is a guess at
 * *where* someone is, which is not the same question as what they read: it is wrong for travellers,
 * wrong behind a VPN, and would mean sending the user's address to a third party on first launch.
 */
internal expect fun platformLanguageTag(): String?

/** The strings the tree renders with. Defaults to English so a preview needs no provider. */
val LocalStrings = staticCompositionLocalOf<Strings> { EnglishStrings }

/**
 * The language the interface is in — the one the user demonstrably reads.
 *
 * Separate from [LocalStrings] because some screens need the *code*, not the words: a pronunciation
 * note is written in whatever script its reader can read, which is this one.
 */
val LocalUiLanguage = staticCompositionLocalOf { UiLanguage.Default }

/** The strings for a language, falling back to English for anything it has not translated yet. */
fun stringsFor(language: UiLanguage): Strings = translations[language] ?: EnglishStrings

/**
 * The current language's words, readable from any composable: `strings.back`.
 *
 * Shaped like `MaterialTheme.colorScheme` on purpose — a screen should reach for a word the same way
 * it reaches for a colour, without first declaring anything.
 */
val strings: Strings
    @Composable
    @ReadOnlyComposable
    get() = LocalStrings.current
