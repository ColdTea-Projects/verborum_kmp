package de.coldtea.verborum.core.localization

/**
 * Every word the app says to the user.
 *
 * English lives here as the **default of each property**, and a language overrides only what it has
 * translated. That is deliberate: a partly translated language falls back to English string by
 * string instead of failing, so a translation can land a screen at a time and nothing is ever blank
 * or crashes for a missing key. Adding a string here needs no change to the other eighteen files.
 *
 * Anything with a value in it is a function rather than a property, so a translation can put the
 * number where its own grammar wants it.
 */
interface Strings {

    // ---- shared chrome -------------------------------------------------------------------------
    val appName: String get() = "Verborum"
    val back: String get() = "Back"
    val retry: String get() = "Retry"
    val cancel: String get() = "Cancel"
    val delete: String get() = "Delete"
    val edit: String get() = "Edit"
    val remove: String get() = "Remove"
    val offline: String get() = "You are offline. Changes are saved locally and sync when you reconnect."

    // ---- sign in -------------------------------------------------------------------------------
    val signIn: String get() = "Sign in"
    val createAccount: String get() = "Create account"
    val signInTagline: String get() = "Your dictionaries, on every device."

    // ---- tabs ----------------------------------------------------------------------------------
    val bibliotheca: String get() = "Bibliotheca"
    val forum: String get() = "Forum"
    val options: String get() = "Options"
    val comingSoon: String get() = "Coming soon"
    val forumFailed: String get() = "The forum could not be loaded."

    // ---- options -------------------------------------------------------------------------------
    val yourAccount: String get() = "Your account"
    val signOut: String get() = "Sign out"
    val signingOut: String get() = "Signing out…"
    val howToUseTheApp: String get() = "How to use the app"
    val appLanguage: String get() = "App language"
    val appLanguageSubtitle: String get() = "The language Verborum itself speaks."
    val systemLanguage: String get() = "System language"

    // ---- dictionary list -----------------------------------------------------------------------
    val yourDictionaries: String get() = "Your Dictionaries"
    val yourDictionariesSubtitle: String get() = "Every language pair you're building, in one place."
    val searchDictionaries: String get() = "Search dictionaries…"
    val searchDictionariesAction: String get() = "Search dictionaries"
    val clearSearch: String get() = "Clear search"
    val clear: String get() = "Clear"
    val sortBy: String get() = "Sort by"
    val anyLanguage: String get() = "Any"
    val moreOptions: String get() = "More options"
    val createDictionary: String get() = "Create Dictionary"
    val noDictionariesMatchSearch: String get() = "No dictionaries match your search."
    val noDictionaryMatchesFilters: String get() = "No dictionary matches those filters."
    val dictionariesFailed: String get() = "Your dictionaries could not be loaded."
    val refreshFailed: String get() = "Could not refresh. Showing what was loaded earlier."
    val dictionaryDeleteFailed: String get() = "That dictionary could not be deleted. It is back in your list."

    // ---- dictionary detail ---------------------------------------------------------------------
    val dictionary: String get() = "Dictionary"
    val backToDictionaries: String get() = "Back to dictionaries"
    val test: String get() = "Test"
    val selfPractice: String get() = "Self Practice"
    val wordList: String get() = "Word list"
    val words: String get() = "Words"
    val addWord: String get() = "+ Add Word"
    val deleteDictionary: String get() = "Delete Dictionary"
    val deleteWord: String get() = "Delete word"
    val noWordsYet: String get() = "No words yet. Add the first one below."
    val dictionaryFailed: String get() = "This dictionary could not be loaded."
    val wordDeleteFailed: String get() = "That word could not be deleted. It is back in the list."
    fun deleteDictionaryWarning(name: String): String = "“$name” and all of its words will be deleted."
    fun wordCount(count: Int): String = if (count == 1) "1 word" else "$count words"

    // ---- dictionary form -----------------------------------------------------------------------
    val newDictionary: String get() = "New dictionary"
    val editDictionaryTitle: String get() = "Edit Dictionary"
    val createDictionarySubtitle: String get() = "Name it, choose a language pair, and tag it for easy filtering."
    val dictionaryName: String get() = "Dictionary name"
    val dictionaryNameHint: String get() = "e.g. German Basics"
    val fromLanguage: String get() = "From language"
    val toLanguage: String get() = "To language"
    val select: String get() = "Select…"
    val tags: String get() = "Tags"
    val tagSectionLevel: String get() = "Level"
    val tagSectionTopic: String get() = "Topic"
    val tagSectionExam: String get() = "Exam"
    val tagSelectorExpanded: String get() = "Expanded"
    val tagSelectorCollapsed: String get() = "Collapsed"
    val tagLevelBasic: String get() = "Basic"
    val tagLevelIntermediate: String get() = "Intermediate"
    val tagLevelAdvanced: String get() = "Advanced"
    val tagTopicFoodDrink: String get() = "Food & drink"
    val tagTopicHomeAppliances: String get() = "Home & appliances"
    val tagTopicClothing: String get() = "Clothing"
    val tagTopicFamily: String get() = "Family"
    val tagTopicDailyRoutine: String get() = "Daily routine"
    val tagTopicShopping: String get() = "Shopping"
    val tagTopicMoney: String get() = "Money"
    val tagTopicTravel: String get() = "Travel"
    val tagTopicTransport: String get() = "Transport"
    val tagTopicCarsParts: String get() = "Cars & parts"
    val tagTopicDirections: String get() = "Directions"
    val tagTopicCity: String get() = "City"
    val tagTopicNatureWeather: String get() = "Nature & weather"
    val tagTopicAnimals: String get() = "Animals"
    val tagTopicPlants: String get() = "Plants"
    val tagTopicBodyHealth: String get() = "Body & health"
    val tagTopicMedicine: String get() = "Medicine"
    val tagTopicEmotions: String get() = "Emotions"
    val tagTopicWorkOffice: String get() = "Work & office"
    val tagTopicBusiness: String get() = "Business"
    val tagTopicEducation: String get() = "Education"
    val tagTopicItTechnology: String get() = "IT & technology"
    val tagTopicLaw: String get() = "Law"
    val tagTopicScience: String get() = "Science"
    val tagTopicSports: String get() = "Sports"
    val tagTopicMusic: String get() = "Music"
    val tagTopicArtFilm: String get() = "Art & film"
    val tagTopicCultureHolidays: String get() = "Culture & holidays"
    val tagTopicNewsPolitics: String get() = "News & politics"
    val tagTopicFoodService: String get() = "Food service/hospitality"

    val saveChanges: String get() = "Save Changes"
    val saving: String get() = "Saving…"
    val dictionarySaveFailed: String get() = "That dictionary could not be saved."
    val dictionaryLoadFailed: String get() = "That dictionary could not be loaded."

    // ---- word form -----------------------------------------------------------------------------
    val newWord: String get() = "New word"
    val editWordTitle: String get() = "Edit Word"
    val addWordTitle: String get() = "Add Word"
    val saveWord: String get() = "Save Word"
    val wordType: String get() = "Word type"
    val typeOfWord: String get() = "Type of word"
    val word: String get() = "Word"
    val gender: String get() = "Gender"
    val addAnotherMeaning: String get() = "+ Add another meaning"
    val wordSaveFailed: String get() = "That word could not be saved."
    val wordLoadFailed: String get() = "This word could not be loaded."
    fun meaningNumber(index: Int): String = "Meaning $index"
    fun backTo(name: String): String = "Back to $name"

    // ---- practice ------------------------------------------------------------------------------
    val wordFirst: String get() = "Word first"
    val translationFirst: String get() = "Translation first"
    val switchSides: String get() = "Switch sides"
    val correct: String get() = "Correct"
    val wrong: String get() = "Wrong"
    val practiceFailed: String get() = "This practice session could not be loaded."

    // ---- test ----------------------------------------------------------------------------------
    val exitTest: String get() = "Exit test"
    val testComplete: String get() = "Test complete"
    val checkAnswer: String get() = "Check Answer"
    val nextQuestion: String get() = "Next Question"
    val testFailed: String get() = "This test could not be loaded."
    val answerSaveFailed: String get() = "That answer could not be saved."
    val wellDone: String get() = "Well done!"
    val keepPracticing: String get() = "Keep practicing"
    val passedMessage: String get() = "You have a strong grasp of these words. Great job!"
    val failedMessage: String get() = "Review the words and try again to improve your score."
    val incorrect: String get() = "Incorrect"
    val backToDictionary: String get() = "Back to Dictionary"
    val tryAgain: String get() = "Try Again"
    fun questionProgress(index: Int, total: Int): String = "Question $index of $total"
    fun askMeaning(word: String): String = "What does “$word” mean?"
    fun askForm(word: String): String = "What is “$word”?"
    fun correctAnswerWas(answer: String): String = "Not quite — the answer is “$answer”."
    fun notEnoughWords(required: Int): String =
        "A test needs at least $required different words to choose between."
    fun scoreOf(correct: Int, total: Int): String = "$correct of $total"

    // ---- onboarding ----------------------------------------------------------------------------
    val onboardingWelcomeTitle: String get() = "Welcome to Verborum"
    val onboardingLibraryTitle: String get() = "Build your library"
    val onboardingTestTitle: String get() = "Test yourself"
    val onboardingPracticeSwipeTitle: String get() = "Practice with a swipe"
    val onboardingPracticeFlipTitle: String get() = "Practice with a flip"
    val iAmDone: String get() = "I am done"

    // ---- keyboard ------------------------------------------------------------------------------
    val closeKeyboard: String get() = "Close keyboard"
    val space: String get() = "space"
    val shift: String get() = "Shift"
    val symbols: String get() = "Symbols"
    val backspace: String get() = "Backspace"
    val nextField: String get() = "Next field"
    val keyboardUnavailable: String get() = "Keyboard unavailable — choose a language first"
    val bopomofoNote: String get() = "Bopomofo — use your system input method to convert to characters."
    val pinyinNote: String get() = "Pinyin only — use your system input method for characters."
    fun showKeyboard(language: String): String = "Show the $language keyboard"
    fun hideKeyboard(language: String): String = "Hide the $language keyboard"

    // ---- parts of speech -----------------------------------------------------------------------
    val noun: String get() = "noun"
    val verb: String get() = "verb"
    val adjective: String get() = "adjective"
    val adverb: String get() = "adverb"
    val other: String get() = "other"
    val freeText: String get() = "free text"
    val preposition: String get() = "preposition"
    val pronoun: String get() = "pronoun"
    val numeral: String get() = "numeral"
    val conjunction: String get() = "conjunction"
    val interjection: String get() = "interjection"
    val article: String get() = "article"

    // ---- grammatical forms ---------------------------------------------------------------------
    val reading: String get() = "Reading"
    val plural: String get() = "Plural"
    val feminine: String get() = "Feminine"
    val comparative: String get() = "Comparative"
    val superlative: String get() = "Superlative"
    val present: String get() = "Present"
    val past: String get() = "Past"
    val pastForm: String get() = "Past form"
    val participle: String get() = "Participle"
    val auxiliary: String get() = "Auxiliary"
    val aspect: String get() = "Aspect"
    val root: String get() = "Root"
    val stem: String get() = "Stem"
    val measure: String get() = "Measure"
    val wordClass: String get() = "Class"
    val polite: String get() = "Polite"

    // ---- gender --------------------------------------------------------------------------------
    val masculine: String get() = "masculine"
    val neuter: String get() = "neuter"
    val common: String get() = "common"

    // ---- onboarding copy -----------------------------------------------------------------------
    val onboardingWelcomeBody: String
        get() = "Your personal vocabulary library. Build dictionaries, collect words and make them stick."
    val onboardingLibraryBody: String
        get() = "Create a dictionary for any language pair, then add words together with their " +
            "grammar — articles, plurals, verb forms and more."
    val onboardingTestBody: String
        get() = "Take multiple-choice tests on your words. Every form you entered gets its own " +
            "question, and correct answers raise a word's level."
    val onboardingPracticeSwipeBody: String
        get() = "In self practice, tap a card to reveal the translation, then swipe right when you " +
            "knew it and left when you did not — either way the word's level moves."
    val onboardingPracticeFlipBody: String
        get() = "In self practice, click a card to flip it and see the translation. Then mark it " +
            "correct when you knew it, or wrong when you did not — either way the word's level moves."

    // ---- language names ------------------------------------------------------------------------

    /**
     * A language's name **in the reader's language** — "Deutsch" reads as "German" to an English
     * speaker. One function rather than nineteen properties, so a translation is a single override.
     *
     * Distinct from `UiLanguage.endonym`, which names a language in itself. That is right for the
     * picker, where the reader is looking for their own; this is right everywhere else, where the
     * reader is being told what a dictionary is about.
     */
    fun languageName(code: String): String = when (code.lowercase()) {
        "en" -> "English"
        "de" -> "German"
        "fr" -> "French"
        "es" -> "Spanish"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        "nl" -> "Dutch"
        "lt" -> "Lithuanian"
        "tr" -> "Turkish"
        "az" -> "Azerbaijani"
        "pl" -> "Polish"
        "uk" -> "Ukrainian"
        "ru" -> "Russian"
        "el" -> "Greek"
        "ar" -> "Arabic"
        "fa" -> "Farsi"
        "ja" -> "Japanese"
        "zh" -> "Chinese"
        "ko" -> "Korean"
        else -> code
    }

    // ---- dictionary tags -----------------------------------------------------------------------

    /**
     * A tag's label, or null where the tag is a proper noun that never translates — the framework
     * codes (A1, N5, HSK 3) and the exam names (DELE, JLPT), which read the same in every language.
     */
    fun tagLabel(code: String): String? = when (code) {
        "basic" -> "Basic"
        "intermediate" -> "Intermediate"
        "advanced" -> "Advanced"
        "food_drink" -> "Food & drink"
        "home_appliances" -> "Home & appliances"
        "clothing" -> "Clothing"
        "family" -> "Family"
        "daily_routine" -> "Daily routine"
        "shopping" -> "Shopping"
        "money" -> "Money"
        "travel" -> "Travel"
        "transport" -> "Transport"
        "cars_parts" -> "Cars & parts"
        "directions" -> "Directions"
        "city" -> "City"
        "nature_weather" -> "Nature & weather"
        "animals" -> "Animals"
        "plants" -> "Plants"
        "body_health" -> "Body & health"
        "medicine" -> "Medicine"
        "emotions" -> "Emotions"
        "work_office" -> "Work & office"
        "business" -> "Business"
        "education" -> "Education"
        "it_technology" -> "IT & technology"
        "law" -> "Law"
        "science" -> "Science"
        "sports" -> "Sports"
        "music" -> "Music"
        "art_film" -> "Art & film"
        "culture_holidays" -> "Culture & holidays"
        "news_politics" -> "News & politics"
        "food_service" -> "Food service"
        else -> null
    }

    // ---- sort ----------------------------------------------------------------------------------
    val sortNameAsc: String get() = "Name A–Z"
    val sortNameDesc: String get() = "Name Z–A"
    val sortNewest: String get() = "Newest first"
    val sortOldest: String get() = "Oldest first"
}

/** The base language, and the fallback every other one degrades to. */
object EnglishStrings : Strings
