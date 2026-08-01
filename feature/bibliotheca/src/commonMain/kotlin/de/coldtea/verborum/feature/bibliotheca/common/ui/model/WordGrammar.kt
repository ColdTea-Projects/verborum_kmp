package de.coldtea.verborum.feature.bibliotheca.common.ui.model

/**
 * Grammatical gender. Stored in the meta as [metaCode] and shown either as the language's definite
 * article (der/die/das, el/la) or, where a language has no articles, as a plain label.
 */
internal enum class Gender(val metaCode: String, val label: String) {
    MASCULINE("m", "masculine"),
    FEMININE("f", "feminine"),
    NEUTER("n", "neuter"),
    COMMON("c", "common"),
}

/**
 * Which grammatical detail a language asks for, and how its nouns carry an article.
 *
 * Ported from the Android app's `LanguageGrammar`, which is the reference for what each language
 * needs. What is **not** ported: the per-language hint texts, and the Japanese verb/adjective class
 * pickers — those are choice lists whose options are themselves localised. Both are additive, so a
 * word saved here stays readable there.
 */
internal object WordGrammar {

    /** Articles by language, used to compose and strip the surface form of a noun. */
    private val articles: Map<String, Map<Gender, String>> = mapOf(
        "de" to mapOf(Gender.MASCULINE to "der", Gender.FEMININE to "die", Gender.NEUTER to "das"),
        "fr" to mapOf(Gender.MASCULINE to "le", Gender.FEMININE to "la"),
        "es" to mapOf(Gender.MASCULINE to "el", Gender.FEMININE to "la"),
        "it" to mapOf(Gender.MASCULINE to "il", Gender.FEMININE to "la"),
        "pt" to mapOf(Gender.MASCULINE to "o", Gender.FEMININE to "a"),
        "nl" to mapOf(Gender.COMMON to "de", Gender.NEUTER to "het"),
        "el" to mapOf(Gender.MASCULINE to "ο", Gender.FEMININE to "η", Gender.NEUTER to "το"),
    )

    /** Languages that mark gender at all — including those without articles to show for it. */
    private val genderedWithoutArticles = setOf("lt", "ru", "uk", "pl", "ar", "fa")

    /** The genders a language's nouns can take; empty when it does not mark gender. */
    fun genderOptions(languageCode: String): List<Gender> {
        val code = languageCode.lowercase()

        articles[code]?.let { return it.keys.toList() }

        return if (code in genderedWithoutArticles) {
            listOf(Gender.MASCULINE, Gender.FEMININE, Gender.NEUTER)
        } else {
            emptyList()
        }
    }

    /** The article to show on a gender chip, or the gender's own name where there is none. */
    fun genderLabel(languageCode: String, gender: Gender): String =
        articles[languageCode.lowercase()]?.get(gender) ?: gender.label

    /**
     * The grammatical forms this language asks for at this word type — the fields the form shows
     * beyond the word itself.
     */
    fun fieldsFor(languageCode: String, wordType: WordType): List<FieldKey> {
        val code = languageCode.lowercase()

        // A word in a non-alphabetic script is unreadable without one, whatever its part of speech.
        val reading = if (code in readingLanguages) listOf(FieldKey.READING) else emptyList()

        return reading + when (wordType) {
            WordType.NOUN -> buildList {
                if (code !in noPluralLanguages) add(FieldKey.PLURAL)
                if (code in rootLanguages) add(FieldKey.ROOT)
                if (code in measureLanguages) add(FieldKey.MEASURE)
            }

            WordType.VERB -> verbFields(code)

            WordType.ADJECTIVE -> buildList {
                if (code in feminineAdjectiveLanguages) add(FieldKey.FEMININE)
                if (code !in noComparisonLanguages) {
                    add(FieldKey.COMPARATIVE)
                    add(FieldKey.SUPERLATIVE)
                }
                if (code == "ko") add(FieldKey.POLITE)
            }

            // Adverbs and the closed classes capture the word itself and nothing more.
            else -> emptyList()
        }
    }

    /** The auxiliary is a choice, not free text: German picks between two verbs. */
    fun auxiliaryOptions(languageCode: String): List<String> =
        if (languageCode.lowercase() == "de") listOf("haben", "sein") else emptyList()

    /**
     * The stored surface form: gender plus base word with the language's article applied, so a
     * German noun is stored as "der Apfel" and read back the same way.
     */
    fun composeSurface(languageCode: String, gender: Gender?, word: String): String {
        val trimmed = word.trim()
        if (trimmed.isEmpty() || gender == null) return trimmed

        val code = languageCode.lowercase()
        val article = articles[code]?.get(gender) ?: return trimmed

        // French elides before a vowel: "l'eau", never "la eau".
        if (code == "fr" && trimmed.startsWithVowelSound()) return "l'$trimmed"

        return "$article $trimmed"
    }

    /** Inverse of [composeSurface], so an edit form shows "Apfel" rather than "der Apfel". */
    fun extractBaseWord(languageCode: String, surface: String): String {
        val code = languageCode.lowercase()
        val trimmed = surface.trim()

        if (code == "fr" || code == "it") {
            trimmed.removePrefix("l'").takeIf { it != trimmed }?.let { return it }
        }

        articles[code]?.values?.forEach { article ->
            if (trimmed.startsWith("$article ")) return trimmed.removePrefix("$article ")
        }

        return trimmed
    }

    private fun verbFields(code: String): List<FieldKey> = when (code) {
        "en" -> listOf(FieldKey.PAST, FieldKey.PARTICIPLE)
        "de" -> listOf(FieldKey.PAST, FieldKey.PARTICIPLE, FieldKey.AUXILIARY)
        "nl" -> listOf(FieldKey.PAST, FieldKey.PARTICIPLE)
        "fr", "es", "it", "pt" -> listOf(FieldKey.PAST, FieldKey.PARTICIPLE)
        // Slavic verbs pair an aspect counterpart with the present.
        "pl", "uk", "ru" -> listOf(FieldKey.PRESENT_3RD, FieldKey.ASPECT)
        "el" -> listOf(FieldKey.PAST)
        "ar" -> listOf(FieldKey.PRESENT_3RD, FieldKey.ROOT)
        "fa" -> listOf(FieldKey.STEM)
        "ko" -> listOf(FieldKey.POLITE)
        "tr", "az" -> listOf(FieldKey.PAST)
        else -> listOf(FieldKey.PAST)
    }

    /** Scripts that do not spell out their own pronunciation: pinyin, kana, romanisation. */
    private val readingLanguages = setOf("zh", "ja")

    /** Languages whose script or grammar makes a written plural form unhelpful here. */
    private val noPluralLanguages = setOf("ja", "zh", "ko")
    private val rootLanguages = setOf("ar")
    private val measureLanguages = setOf("zh", "ja")
    private val feminineAdjectiveLanguages = setOf("fr", "es", "it", "pt", "ru", "uk", "pl", "lt")
    private val noComparisonLanguages = setOf("ja", "zh", "ko", "ar", "fa")

    private fun String.startsWithVowelSound(): Boolean = first().lowercaseChar() in "aeiouhéèêà"
}
