package de.coldtea.verborum.feature.bibliotheca.common.ui.model

import de.coldtea.verborum.core.network.VerborumJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The display side of a stored word. A word's surface is a JSON array of alternative meanings
 * (`["buy","purchase"]`), and its meta blob carries the language code the create-word screen wrote.
 *
 * Only what the details screen needs is ported here: the grammatical forms in the meta blob belong
 * to the create-word screen and stay unparsed until that screen exists.
 */
object WordSurfaces {

    /** `["buy","purchase"]` → `["buy", "purchase"]`; a plain string stays a single surface. */
    fun split(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        if (trimmed.startsWith("[")) {
            // A value that is not a JSON array is legitimate input here, not a programmer error.
            runCatching { VerborumJson.decodeFromString<List<String>>(trimmed) }
                .getOrNull()
                ?.let { surfaces -> return surfaces.map(String::trim).filter(String::isNotBlank) }
        }

        return listOf(trimmed)
    }

    /**
     * The surface column as the learner sees it: `["buy","purchase"]` → "buy/purchase", with the
     * separator chosen by script so an RTL or CJK line never mixes directions with Latin punctuation.
     */
    fun display(text: String, languageCode: String): String =
        split(text).joinToString(alternativeSeparatorFor(languageCode))

    /** The language recorded in a meta blob, or empty when it is missing or unparseable. */
    fun languageCodeOf(meta: String): String =
        parse(meta)?.get("lang")?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun parse(meta: String): JsonObject? =
        meta.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { VerborumJson.parseToJsonElement(raw).jsonObject }.getOrNull()
        }

    /** Storage is unaffected by this — surfaces are always a JSON array, whatever the script. */
    private fun alternativeSeparatorFor(languageCode: String): String =
        when (languageCode.lowercase()) {
            "ar", "fa" -> "،"
            "ja" -> "・"
            "zh" -> "、"
            else -> "/"
        }
}
