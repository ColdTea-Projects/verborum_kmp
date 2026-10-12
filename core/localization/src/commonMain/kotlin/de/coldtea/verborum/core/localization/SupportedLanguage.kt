package de.coldtea.verborum.core.localization

/**
 * The languages a dictionary can translate between. [code] is the stable identifier stored on the
 * dictionary and sent to the API; [displayName] is what the user sees.
 *
 * In `core:localization` rather than in a feature, because two of them need it: the library writes
 * a dictionary's pair, and the Forum filters listings by one. [displayName] is the English name;
 * `Strings.languageName` is what a screen shows, and follows the app's language.
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
