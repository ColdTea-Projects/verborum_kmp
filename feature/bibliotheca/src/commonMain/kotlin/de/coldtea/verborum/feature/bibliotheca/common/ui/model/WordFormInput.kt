package de.coldtea.verborum.feature.bibliotheca.common.ui.model

import de.coldtea.verborum.core.network.VerborumJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the user typed for one language side of one meaning. A word can carry several alternatives
 * (*kaufen/erwerben*), so a form holds a list of these and their field lists stay index-aligned.
 */
internal data class WordFormInput(
    val text: String = "",
    val gender: Gender? = null,
    val fields: Map<FieldKey, String> = emptyMap(),
) {
    fun field(key: FieldKey): String = fields[key].orEmpty()

    fun withField(key: FieldKey, value: String): WordFormInput = copy(fields = fields + (key to value))
}

@Serializable
private data class StoredMeta(
    @SerialName("lang") val lang: String,
    @SerialName("type") val type: String? = null,
    @SerialName("genders") val genders: List<String> = emptyList(),
    @SerialName("fields") val fields: Map<String, List<String>> = emptyMap(),
)

/**
 * The stored surface column: a JSON array of the meanings' surfaces, `["der Apfel"]`. JSON keeps any
 * user text safe — including a "/" — and the array stays index-aligned with the meta's field lists.
 */
internal fun composeWordText(languageCode: String, inputs: List<WordFormInput>): String =
    inputs
        .map { input -> WordGrammar.composeSurface(languageCode, input.gender, input.text).trim() }
        .filter { it.isNotBlank() }
        .let { surfaces -> VerborumJson.encodeToString(surfaces) }

/**
 * The grammatical detail of every meaning, as the meta blob the rest of the app reads:
 * `{"lang":"de","type":"verb","fields":{"past":["kaufte"]}}`. A field blank in every meaning is
 * left out entirely, and free text carries no type.
 */
internal fun composeWordMeta(
    languageCode: String,
    wordType: WordType?,
    inputs: List<WordFormInput>,
): String {
    val genders = inputs.map { it.gender?.metaCode.orEmpty() }
    val fields = buildMap {
        FieldKey.entries.forEach { key ->
            val values = inputs.map { it.field(key).trim() }
            if (values.any { it.isNotBlank() }) put(key.metaKey, values)
        }
    }

    val meta = StoredMeta(
        lang = languageCode.lowercase(),
        type = wordType?.metaType,
        genders = if (genders.any { it.isNotBlank() }) genders else emptyList(),
        fields = fields,
    )

    return VerborumJson.encodeToString(meta)
}

/**
 * The inverse: rebuilds the editable inputs from a stored word, so opening a word for editing shows
 * what was typed rather than what was stored — "Apfel" with its gender chip selected, not
 * "der Apfel".
 */
internal fun parseWordFormInputs(
    languageCode: String,
    storedText: String,
    meta: String,
): List<WordFormInput> {
    val surfaces = WordSurfaces.split(storedText)
    val bundle = parseWordMeta(meta)
    val meanings = bundle?.meanings.orEmpty()
    val genders = bundle?.genderCodes.orEmpty()

    val count = maxOf(surfaces.size, meanings.size).coerceAtLeast(1)

    return (0 until count).map { index ->
        WordFormInput(
            text = surfaces.getOrNull(index)
                ?.let { surface -> WordGrammar.extractBaseWord(languageCode, surface) }
                .orEmpty(),
            gender = genders.getOrNull(index)
                ?.let { code -> Gender.entries.firstOrNull { it.metaCode == code } },
            fields = meanings.getOrNull(index)?.fields.orEmpty(),
        )
    }
}
