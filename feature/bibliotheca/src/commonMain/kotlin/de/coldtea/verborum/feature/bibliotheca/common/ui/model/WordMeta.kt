package de.coldtea.verborum.feature.bibliotheca.common.ui.model

import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.core.network.VerborumJson
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A grammatical form stored alongside the base word. [metaKey] is the key in the meta blob; the
 * **declaration order is the display order**, so a verb reads "go · went · gone".
 */
internal enum class FieldKey(val metaKey: String) {
    // Reading leads so kana/pinyin show before every other form.
    READING("reading"),
    PLURAL("plural"),
    FEMININE("feminine"),
    COMPARATIVE("comparative"),
    SUPERLATIVE("superlative"),
    PRESENT_3RD("present"),
    PAST("past"),
    PAST_3RD("past3"),
    PARTICIPLE("participle"),
    AUXILIARY("aux"),
    ASPECT("aspect"),
    ROOT("root"),
    STEM("stem"),
    MEASURE("measure"),
    CLASS("class"),
    POLITE("polite"),
    ;

    fun label(strings: Strings): String = when (this) {
        READING -> strings.reading
        PLURAL -> strings.plural
        FEMININE -> strings.feminine
        COMPARATIVE -> strings.comparative
        SUPERLATIVE -> strings.superlative
        PRESENT_3RD -> strings.present
        PAST -> strings.past
        PAST_3RD -> strings.pastForm
        PARTICIPLE -> strings.participle
        AUXILIARY -> strings.auxiliary
        ASPECT -> strings.aspect
        ROOT -> strings.root
        STEM -> strings.stem
        MEASURE -> strings.measure
        CLASS -> strings.wordClass
        POLITE -> strings.polite
    }
}

/**
 * The top-level choice the form offers. The four open classes carry grammatical forms of their own;
 * every closed class collapses into [OTHER], which then names itself.
 */
internal enum class WordCategory {
    NOUN,
    VERB,
    ADJECTIVE,
    ADVERB,
    OTHER,
    ;

    fun label(strings: Strings): String = when (this) {
        NOUN -> strings.noun
        VERB -> strings.verb
        ADJECTIVE -> strings.adjective
        ADVERB -> strings.adverb
        OTHER -> strings.other
    }.replaceFirstChar(Char::uppercaseChar)
}

/**
 * The part of speech, shown after the word. English only, as the rest of this app is for now.
 *
 * [FREE_TEXT] stores no type at all — that is what makes a word saved before sub-types existed load
 * back as free text rather than as something it never claimed to be.
 */
internal enum class WordType(
    val metaType: String?,
    val category: WordCategory,
) {
    NOUN("noun", WordCategory.NOUN),
    VERB("verb", WordCategory.VERB),
    ADJECTIVE("adjective", WordCategory.ADJECTIVE),
    ADVERB("adverb", WordCategory.ADVERB),
    FREE_TEXT(null, WordCategory.OTHER),
    PREPOSITION("preposition", WordCategory.OTHER),
    PRONOUN("pronoun", WordCategory.OTHER),
    NUMERAL("numeral", WordCategory.OTHER),
    CONJUNCTION("conjunction", WordCategory.OTHER),
    INTERJECTION("interjection", WordCategory.OTHER),
    ARTICLE("article", WordCategory.OTHER),
    ;

    /** Trailing a word — "gehen (verb)" — so it reads lower case. */
    fun label(strings: Strings): String = when (this) {
        NOUN -> strings.noun
        VERB -> strings.verb
        ADJECTIVE -> strings.adjective
        ADVERB -> strings.adverb
        FREE_TEXT -> strings.freeText
        PREPOSITION -> strings.preposition
        PRONOUN -> strings.pronoun
        NUMERAL -> strings.numeral
        CONJUNCTION -> strings.conjunction
        INTERJECTION -> strings.interjection
        ARTICLE -> strings.article
    }

    /** The same name where it heads a control rather than trailing a word. */
    fun chipLabel(strings: Strings): String = label(strings).replaceFirstChar(Char::uppercaseChar)

    companion object {
        /** The sub-types offered under "Other", free text first. */
        val otherTypes: List<WordType> = entries.filter { it.category == WordCategory.OTHER }

        /**
         * Null for a word that records no type. Deliberately *not* [FREE_TEXT]: a word saved before
         * types existed has no part of speech to show, and labelling every one of them "free text"
         * would be inventing an answer. The form maps the same absence to [FREE_TEXT], where a
         * concrete choice is what the control needs.
         */
        fun fromMeta(metaType: String?): WordType? =
            metaType?.let { type -> entries.firstOrNull { it.metaType == type } }
    }
}

/** What a freshly-picked category starts on; "Other" opens on free text. */
internal val WordCategory.defaultType: WordType
    get() = when (this) {
        WordCategory.NOUN -> WordType.NOUN
        WordCategory.VERB -> WordType.VERB
        WordCategory.ADJECTIVE -> WordType.ADJECTIVE
        WordCategory.ADVERB -> WordType.ADVERB
        WordCategory.OTHER -> WordType.FREE_TEXT
    }

/**
 * A parsed meta blob. A word can carry several alternative meanings (*kaufen/erwerben*); each
 * [Meaning] is one alternative, and its field lists are index-aligned with the stored surfaces.
 */
internal data class WordMetaBundle(
    val languageCode: String,
    val wordType: WordType?,
    val meanings: List<Meaning>,
    /** Per meaning, in the same order — kept raw so the edit form can restore the chips. */
    val genderCodes: List<String> = emptyList(),
) {
    data class Meaning(val fields: Map<FieldKey, String>)
}

@Serializable
private data class WordMetaDto(
    @SerialName("lang") val lang: String = "",
    @SerialName("type") val type: String? = null,
    @SerialName("genders") val genders: List<String> = emptyList(),
    @SerialName("fields") val fields: Map<String, List<String>> = emptyMap(),
)

/**
 * Reads `{"lang":"de","type":"verb","fields":{"past":["ging"]}}` into its meanings. Unknown keys are
 * ignored so a newer meta format stays readable, and an unparseable blob yields null rather than
 * failing the word.
 */
internal fun parseWordMeta(meta: String): WordMetaBundle? {
    val trimmed = meta.trim()
    if (trimmed.isEmpty()) return null

    val dto = runCatching { VerborumJson.decodeFromString<WordMetaDto>(trimmed) }.getOrNull()
        ?: return null

    val meaningCount = maxOf(
        dto.genders.size,
        dto.fields.values.maxOfOrNull { it.size } ?: 0,
    ).coerceAtLeast(1)

    val meanings = (0 until meaningCount).map { index ->
        val fields = buildMap {
            dto.fields.forEach { (metaKey, values) ->
                val key = FieldKey.entries.firstOrNull { it.metaKey == metaKey } ?: return@forEach
                values.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() }?.let { put(key, it) }
            }
        }
        WordMetaBundle.Meaning(fields)
    }

    return WordMetaBundle(
        languageCode = dto.lang,
        wordType = WordType.fromMeta(dto.type),
        meanings = meanings,
        genderCodes = dto.genders,
    )
}

/**
 * One form as the learner sees it. The past participle carries its auxiliary — "(sein) gegangen" —
 * so the auxiliary is never a form of its own, and the stored class code is not a form at all.
 */
internal fun displayForm(fields: Map<FieldKey, String>, key: FieldKey): String? {
    if (key == FieldKey.AUXILIARY || key == FieldKey.CLASS) return null

    val value = fields[key]?.takeIf { it.isNotBlank() } ?: return null
    if (key != FieldKey.PARTICIPLE) return value

    return fields[FieldKey.AUXILIARY]?.takeIf { it.isNotBlank() }?.let { aux -> "($aux) $value" }
        ?: value
}

/**
 * The word's forms as separate columns: the surfaces first, then each grammatical form, with
 * alternatives joined inside a column — `["go", "went", "gone"]`.
 *
 * Returned as a list rather than a joined string because the two designs lay it out differently: the
 * mobile card puts the columns beside each other, the web flip card stacks them.
 */
internal fun displayColumns(surfaces: String, meta: String): List<String> {
    val bundle = parseWordMeta(meta)
    val languageCode = bundle?.languageCode.orEmpty()
    val separator = alternativeSeparatorFor(languageCode)

    val base = WordSurfaces.split(surfaces).joinToString(separator)
    val forms = FieldKey.entries.mapNotNull { key ->
        bundle?.meanings
            ?.mapNotNull { meaning -> displayForm(meaning.fields, key) }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator)
    }

    return (listOf(base) + forms).filter { it.isNotBlank() }
}

/** The same columns on one line — "gehen · ging · (sein) gegangen" — for the mobile card. */
internal fun displayLine(surfaces: String, meta: String): String {
    val languageCode = parseWordMeta(meta)?.languageCode.orEmpty()

    return displayColumns(surfaces, meta).joinToString(columnSeparatorFor(languageCode))
}

/**
 * The first meaning's grammatical forms, keyed by form. The quiz asks about one form at a time, so
 * unlike the display helpers it needs them apart rather than joined into a line.
 */
internal fun formsOf(meta: String): Map<FieldKey, String> {
    val fields = parseWordMeta(meta)?.meanings?.firstOrNull()?.fields.orEmpty()

    return FieldKey.entries.mapNotNull { key ->
        displayForm(fields, key)?.let { form -> key to form }
    }.toMap()
}

/** The part of speech to show after the word, if the meta records one. */
internal fun wordTypeLabel(meta: String, strings: Strings): String? =
    parseWordMeta(meta)?.wordType?.label(strings)

internal fun Word.wordColumns(): List<String> = displayColumns(word, wordMeta)

internal fun Word.translationColumns(): List<String> = displayColumns(translation, translationMeta)

internal fun Word.wordLine(): String = displayLine(word, wordMeta)

internal fun Word.translationLine(): String = displayLine(translation, translationMeta)

/** A presentation line must never mix directions, so each script keeps its own punctuation. */
private fun alternativeSeparatorFor(languageCode: String): String =
    when (languageCode.lowercase()) {
        "ar", "fa" -> "،"
        "ja" -> "・"
        "zh" -> "、"
        else -> "/"
    }

private fun columnSeparatorFor(languageCode: String): String =
    when (languageCode.lowercase()) {
        "ar", "fa" -> "؛ "
        "ja" -> "、"
        "zh" -> "；"
        else -> " · "
    }
