package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.model

import de.coldtea.verborum.core.localization.Strings

/**
 * A selectable dictionary tag.
 *
 * [code] is the stable identifier to persist and send — it never changes with the UI language. The
 * display label is one of two kinds, matching the Android client:
 * - a [localized] resolver for proficiency levels and topics (follows the app language)
 * - a [fixed] proper-noun label shown as-is in every language (framework codes like A1 / N5 /
 *   HSK 3 and exam names like "DELE (es)")
 *
 * Exactly one of the two is set.
 */
internal data class DictionaryTag(
    val code: String,
    private val fixed: String? = null,
    private val localized: (Strings.() -> String)? = null,
) {
    fun label(strings: Strings): String = fixed ?: localized!!.invoke(strings)
}

private fun translated(code: String, label: Strings.() -> String) =
    DictionaryTag(code = code, localized = label)

private fun fixed(code: String, name: String) =
    DictionaryTag(code = code, fixed = name)

// --- Provisional tag taxonomy (UI; codes are the eventual storage keys) ---

internal val LEVEL_TAGS: List<DictionaryTag> = listOf(
    // General proficiency — translatable.
    translated("basic") { tagLevelBasic },
    translated("intermediate") { tagLevelIntermediate },
    translated("advanced") { tagLevelAdvanced },
    // Framework codes — fixed names. Codes are lower-cased to match the backend's tag
    // normalisation (trimmed + lower-cased on write), so they round-trip unchanged.
    fixed("a1", "A1"), fixed("a2", "A2"), fixed("b1", "B1"),
    fixed("b2", "B2"), fixed("c1", "C1"), fixed("c2", "C2"),
    fixed("n5", "N5"), fixed("n4", "N4"), fixed("n3", "N3"),
    fixed("n2", "N2"), fixed("n1", "N1"),
    fixed("hsk1", "HSK 1"), fixed("hsk2", "HSK 2"), fixed("hsk3", "HSK 3"),
    fixed("hsk4", "HSK 4"), fixed("hsk5", "HSK 5"), fixed("hsk6", "HSK 6"),
    fixed("topik1", "TOPIK 1"), fixed("topik2", "TOPIK 2"), fixed("topik3", "TOPIK 3"),
    fixed("topik4", "TOPIK 4"), fixed("topik5", "TOPIK 5"), fixed("topik6", "TOPIK 6"),
)

internal val TOPIC_TAGS: List<DictionaryTag> = listOf(
    translated("food_drink") { tagTopicFoodDrink },
    translated("home_appliances") { tagTopicHomeAppliances },
    translated("clothing") { tagTopicClothing },
    translated("family") { tagTopicFamily },
    translated("daily_routine") { tagTopicDailyRoutine },
    translated("shopping") { tagTopicShopping },
    translated("money") { tagTopicMoney },
    translated("travel") { tagTopicTravel },
    translated("transport") { tagTopicTransport },
    translated("cars_parts") { tagTopicCarsParts },
    translated("directions") { tagTopicDirections },
    translated("city") { tagTopicCity },
    translated("nature_weather") { tagTopicNatureWeather },
    translated("animals") { tagTopicAnimals },
    translated("plants") { tagTopicPlants },
    translated("body_health") { tagTopicBodyHealth },
    translated("medicine") { tagTopicMedicine },
    translated("emotions") { tagTopicEmotions },
    translated("work_office") { tagTopicWorkOffice },
    translated("business") { tagTopicBusiness },
    translated("education") { tagTopicEducation },
    translated("it_technology") { tagTopicItTechnology },
    translated("law") { tagTopicLaw },
    translated("science") { tagTopicScience },
    translated("sports") { tagTopicSports },
    translated("music") { tagTopicMusic },
    translated("art_film") { tagTopicArtFilm },
    translated("culture_holidays") { tagTopicCultureHolidays },
    translated("news_politics") { tagTopicNewsPolitics },
    translated("food_service") { tagTopicFoodService },
)

internal val EXAM_TAGS: List<DictionaryTag> = listOf(
    fixed("goethe_testdaf", "Goethe/TestDaF (de)"),
    fixed("dele", "DELE (es)"),
    fixed("delf_dalf", "DELF/DALF (fr)"),
    fixed("cils", "CILS (it)"),
    fixed("ielts_cambridge", "IELTS/Cambridge (en)"),
    fixed("jlpt", "JLPT (ja)"),
    fixed("hsk", "HSK (zh)"),
    fixed("topik", "TOPIK (ko)"),
    fixed("torfl", "TORFL (ru)"),
)

/**
 * The three groups the tags are offered in, in the order the picker shows them.
 *
 * The taxonomy is the Android app's, section for section and code for code — the tags are a
 * cross-client contract, and a dictionary tagged on one client has to read the same on the others.
 */
internal enum class TagSection {
    LEVEL,
    TOPIC,
    EXAM,
    ;

    /**
     * Looked up rather than held in the constructor: an entry that captured its list would be
     * initialised while this file's own properties still were, and read them as null.
     */
    val tags: List<DictionaryTag>
        get() = when (this) {
            LEVEL -> LEVEL_TAGS
            TOPIC -> TOPIC_TAGS
            EXAM -> EXAM_TAGS
        }

    fun title(strings: Strings): String = when (this) {
        LEVEL -> strings.tagSectionLevel
        TOPIC -> strings.tagSectionTopic
        EXAM -> strings.tagSectionExam
    }
}

/** Every known tag, and the lookup that turns a stored code back into something readable. */
internal val ALL_TAGS: List<DictionaryTag> = TagSection.entries.flatMap { it.tags }

private val tagsByCode: Map<String, DictionaryTag> = ALL_TAGS.associateBy(DictionaryTag::code)

/** An unknown code still shows — as itself — rather than disappearing from an edited dictionary. */
internal fun tagLabelOf(code: String, strings: Strings): String =
    tagsByCode[code]?.label(strings) ?: code
