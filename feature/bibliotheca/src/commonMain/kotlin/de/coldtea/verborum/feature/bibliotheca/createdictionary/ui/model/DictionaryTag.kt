package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.model

import de.coldtea.verborum.core.localization.Strings

/**
 * A selectable dictionary tag. [code] is the stable identifier that is stored and sent — it never
 * changes with the label — and [label] is what the user reads.
 *
 * The catalogue below is the Android app's, code for code, so the two write the same values.
 */
internal data class DictionaryTag(val code: String, val label: String)

internal val LEVEL_TAGS: List<DictionaryTag> = listOf(
    DictionaryTag("basic", "Basic"),
    DictionaryTag("intermediate", "Intermediate"),
    DictionaryTag("advanced", "Advanced"),
    // Framework codes are lower-cased to match the backend's tag normalisation, so they round-trip.
    DictionaryTag("a1", "A1"), DictionaryTag("a2", "A2"), DictionaryTag("b1", "B1"),
    DictionaryTag("b2", "B2"), DictionaryTag("c1", "C1"), DictionaryTag("c2", "C2"),
    DictionaryTag("n5", "N5"), DictionaryTag("n4", "N4"), DictionaryTag("n3", "N3"),
    DictionaryTag("n2", "N2"), DictionaryTag("n1", "N1"),
    DictionaryTag("hsk1", "HSK 1"), DictionaryTag("hsk2", "HSK 2"), DictionaryTag("hsk3", "HSK 3"),
    DictionaryTag("hsk4", "HSK 4"), DictionaryTag("hsk5", "HSK 5"), DictionaryTag("hsk6", "HSK 6"),
    DictionaryTag("topik1", "TOPIK 1"), DictionaryTag("topik2", "TOPIK 2"),
    DictionaryTag("topik3", "TOPIK 3"), DictionaryTag("topik4", "TOPIK 4"),
    DictionaryTag("topik5", "TOPIK 5"), DictionaryTag("topik6", "TOPIK 6"),
)

internal val TOPIC_TAGS: List<DictionaryTag> = listOf(
    DictionaryTag("food_drink", "Food & drink"),
    DictionaryTag("home_appliances", "Home & appliances"),
    DictionaryTag("clothing", "Clothing"),
    DictionaryTag("family", "Family"),
    DictionaryTag("daily_routine", "Daily routine"),
    DictionaryTag("shopping", "Shopping"),
    DictionaryTag("money", "Money"),
    DictionaryTag("travel", "Travel"),
    DictionaryTag("transport", "Transport"),
    DictionaryTag("cars_parts", "Cars & parts"),
    DictionaryTag("directions", "Directions"),
    DictionaryTag("city", "City"),
    DictionaryTag("nature_weather", "Nature & weather"),
    DictionaryTag("animals", "Animals"),
    DictionaryTag("plants", "Plants"),
    DictionaryTag("body_health", "Body & health"),
    DictionaryTag("medicine", "Medicine"),
    DictionaryTag("emotions", "Emotions"),
    DictionaryTag("work_office", "Work & office"),
    DictionaryTag("business", "Business"),
    DictionaryTag("education", "Education"),
    DictionaryTag("it_technology", "IT & technology"),
    DictionaryTag("law", "Law"),
    DictionaryTag("science", "Science"),
    DictionaryTag("sports", "Sports"),
    DictionaryTag("music", "Music"),
    DictionaryTag("art_film", "Art & film"),
    DictionaryTag("culture_holidays", "Culture & holidays"),
    DictionaryTag("news_politics", "News & politics"),
    DictionaryTag("food_service", "Food service"),
)

internal val EXAM_TAGS: List<DictionaryTag> = listOf(
    DictionaryTag("goethe_testdaf", "Goethe/TestDaF (de)"),
    DictionaryTag("dele", "DELE (es)"),
    DictionaryTag("delf_dalf", "DELF/DALF (fr)"),
    DictionaryTag("cils", "CILS (it)"),
    DictionaryTag("ielts_cambridge", "IELTS/Cambridge (en)"),
    DictionaryTag("jlpt", "JLPT (ja)"),
    DictionaryTag("hsk", "HSK (zh)"),
    DictionaryTag("topik", "TOPIK (ko)"),
    DictionaryTag("torfl", "TORFL (ru)"),
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
internal fun tagLabelOf(code: String): String = tagsByCode[code]?.label ?: code
