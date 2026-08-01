package de.coldtea.verborum.core.localization

/**
 * A language the app's own interface is available in.
 *
 * Deliberately separate from `SupportedLanguage`, which is the set of languages a dictionary can be
 * *about*. The two happen to list the same codes today, but they answer different questions and live
 * on opposite sides of the module graph — a `core` module cannot reach into a feature.
 *
 * [endonym] is the language's name **in itself** — "Deutsch", not "German". A picker written that way
 * is readable to the person looking for their own language, which is the only person using it, and it
 * saves translating nineteen language names into nineteen languages.
 */
enum class UiLanguage(val code: String, val endonym: String) {
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français"),
    SPANISH("es", "Español"),
    ITALIAN("it", "Italiano"),
    PORTUGUESE("pt", "Português"),
    DUTCH("nl", "Nederlands"),
    LITHUANIAN("lt", "Lietuvių"),
    TURKISH("tr", "Türkçe"),
    AZERBAIJANI("az", "Azərbaycan"),
    POLISH("pl", "Polski"),
    UKRAINIAN("uk", "Українська"),
    RUSSIAN("ru", "Русский"),
    GREEK("el", "Ελληνικά"),
    ARABIC("ar", "العربية"),
    FARSI("fa", "فارسی"),
    JAPANESE("ja", "日本語"),
    CHINESE("zh", "中文"),
    KOREAN("ko", "한국어"),
    ;

    companion object {
        val Default = ENGLISH

        /**
         * Matches a platform locale tag — "de-AT", "pt_BR", "zh-Hans-CN" — to a language.
         *
         * Only the primary subtag is considered: the app has one German, and an Austrian asking for
         * "de-AT" wants it rather than English.
         */
        fun fromTag(tag: String?): UiLanguage? {
            val primary = tag?.substringBefore('-')?.substringBefore('_')?.lowercase()

            return entries.firstOrNull { it.code == primary }
        }
    }
}
