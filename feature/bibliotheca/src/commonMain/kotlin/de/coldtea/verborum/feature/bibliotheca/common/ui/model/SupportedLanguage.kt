package de.coldtea.verborum.feature.bibliotheca.common.ui.model

/**
 * The languages a dictionary can translate between. [code] is the stable identifier stored on the
 * dictionary and sent to the API; [displayName] is what the user sees.
 *
 * The Android app resolves the name from a string resource in 19 locales. This app has no
 * localisation yet, so the English name is inlined — when resources arrive, only this enum changes.
 */
enum class SupportedLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    GERMAN("de", "German"),
    FRENCH("fr", "French"),
    SPANISH("es", "Spanish"),
    ITALIAN("it", "Italian"),
    PORTUGUESE("pt", "Portuguese"),
    DUTCH("nl", "Dutch"),
    LITHUANIAN("lt", "Lithuanian"),
    TURKISH("tr", "Turkish"),
    AZERBAIJANI("az", "Azerbaijani"),
    POLISH("pl", "Polish"),
    UKRAINIAN("uk", "Ukrainian"),
    RUSSIAN("ru", "Russian"),
    GREEK("el", "Greek"),
    ARABIC("ar", "Arabic"),
    FARSI("fa", "Farsi"),
    JAPANESE("ja", "Japanese"),
    CHINESE("zh", "Chinese"),
    KOREAN("ko", "Korean"),
    ;

    companion object {
        fun fromCode(code: String): SupportedLanguage? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

        /** The language's name when the code is known, else the raw code — never an empty label. */
        fun displayNameOf(code: String): String = fromCode(code)?.displayName ?: code
    }
}
