package de.coldtea.verborum.feature.bibliotheca.common.ui.model

import de.coldtea.verborum.core.network.VerborumJson
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A grammatical form stored alongside the base word. [metaKey] is the key in the meta blob; the
 * **declaration order is the display order**, so a verb reads "go · went · gone".
 */
internal enum class FieldKey(val metaKey: String, val label: String) {
    // Reading leads so kana/pinyin show before every other form.
    READING("reading", "Reading"),
    PLURAL("plural", "Plural"),
    FEMININE("feminine", "Feminine"),
    COMPARATIVE("comparative", "Comparative"),
    SUPERLATIVE("superlative", "Superlative"),
    PRESENT_3RD("present", "Present"),
    PAST("past", "Past"),
    PAST_3RD("past3", "Past form"),
    PARTICIPLE("participle", "Participle"),
    AUXILIARY("aux", "Auxiliary"),
    ASPECT("aspect", "Aspect"),
    ROOT("root", "Root"),
    STEM("stem", "Stem"),
    MEASURE("measure", "Measure"),
    CLASS("class", "Class"),
    POLITE("polite", "Polite"),
}

/** The part of speech, shown after the word. English only, as the rest of this app is for now. */
internal enum class WordType(val metaType: String, val label: String) {
    NOUN("noun", "noun"),
    VERB("verb", "verb"),
    ADJECTIVE("adjective", "adjective"),
    ADVERB("adverb", "adverb"),
    PREPOSITION("preposition", "preposition"),
    PRONOUN("pronoun", "pronoun"),
    NUMERAL("numeral", "numeral"),
    ;

    companion object {
        fun fromMeta(metaType: String?): WordType? =
            entries.firstOrNull { it.metaType == metaType }
    }
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
internal fun wordTypeLabel(meta: String): String? = parseWordMeta(meta)?.wordType?.label

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
