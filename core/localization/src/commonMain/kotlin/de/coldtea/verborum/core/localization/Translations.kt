package de.coldtea.verborum.core.localization

/**
 * Every language the interface is available in.
 *
 * A language absent from this map, or a string it has not overridden, falls back to English rather
 * than to a blank or a crash — see [Strings].
 */
internal val translations: Map<UiLanguage, Strings> = mapOf(
    UiLanguage.ENGLISH to EnglishStrings,
    UiLanguage.GERMAN to GermanStrings,
    UiLanguage.FRENCH to FrenchStrings,
    UiLanguage.SPANISH to SpanishStrings,
    UiLanguage.ITALIAN to ItalianStrings,
    UiLanguage.PORTUGUESE to PortugueseStrings,
    UiLanguage.DUTCH to DutchStrings,
    UiLanguage.LITHUANIAN to LithuanianStrings,
    UiLanguage.TURKISH to TurkishStrings,
    UiLanguage.AZERBAIJANI to AzerbaijaniStrings,
    UiLanguage.POLISH to PolishStrings,
    UiLanguage.UKRAINIAN to UkrainianStrings,
    UiLanguage.RUSSIAN to RussianStrings,
    UiLanguage.GREEK to GreekStrings,
    UiLanguage.ARABIC to ArabicStrings,
    UiLanguage.FARSI to PersianStrings,
    UiLanguage.JAPANESE to JapaneseStrings,
    UiLanguage.CHINESE to ChineseStrings,
    UiLanguage.KOREAN to KoreanStrings,
)
