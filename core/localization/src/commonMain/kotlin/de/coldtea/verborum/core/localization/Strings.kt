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
    /** Shown instead of [offline] on the Forum and Profile, which never work without a connection. */
    val offlineOnlineOnly: String get() = "You are offline. Forum and Profile need an internet connection."

    // ---- sign in -------------------------------------------------------------------------------
    val signIn: String get() = "Sign in"
    val createAccount: String get() = "Create account"
    val signInTagline: String get() = "Your dictionaries, on every device."

    // Why an attempt did not sign anyone in. Deliberately free of error codes and configuration
    // advice: what a Keycloak reason or a CORS misconfiguration means belongs in the log, to the
    // person building the app, not on screen in front of the person trying to get in.
    val signInRefused: String get() = "Sign-in was refused. Please try again."
    val signInUnverified: String get() = "That sign-in could not be verified. Please try again."
    val signInUnreachable: String get() = "The sign-in server could not be reached. Check your connection."
    val signInRejected: String get() = "The sign-in server rejected this attempt. Please try again."
    val signInFailed: String get() = "Signing in did not finish. Please try again."

    // ---- tabs ----------------------------------------------------------------------------------
    val bibliotheca: String get() = "Bibliotheca"
    val forum: String get() = "Forum"
    val options: String get() = "Options"
    val comingSoon: String get() = "Coming soon"
    val forumFailed: String get() = "The forum could not be loaded."

    // ---- options -------------------------------------------------------------------------------
    val yourAccount: String get() = "Your account"
    val signOut: String get() = "Log out"
    val signingOut: String get() = "Logging out…"
    val howToUseTheApp: String get() = "How to use the app"
    /** The Options row. The picker it opens is titled [appLanguage], which says more. */
    val optionsLanguage: String get() = "Language"
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
    val tagsHint: String get() = "Add tags to organize your dictionary (optional)"
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
    /** The pair, not the dictionary: the wrong answers may come from any dictionary of it. */
    fun notEnoughWords(required: Int): String =
        "A test needs at least ${required} words in this language pair"
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

    // ---- the forum -------------------------------------------------------------------------------
    val forumListScreenTitle: String get() = "Forum"
    val forumListScreenSubtitle: String get() = "Dictionaries shared by the community"
    val forumTabPopular: String get() = "Popular"
    val forumTabFresh: String get() = "Fresh"
    val forumListEmpty: String get() = "No shared dictionaries yet"
    val forumListNoMatches: String get() = "No shared dictionaries match these filters"
    val forumListLoadError: String get() = "Couldn\u2019t load the shared dictionaries"
    val forumListLoadMoreError: String get() = "Couldn\u2019t load more dictionaries"
    val forumUnknownPublisher: String get() = "Unknown publisher"

    /**
     * "English to French" — a listing's direction read as a sentence.
     *
     * Spelled out rather than the library's "English \u203A French": a Forum card is about someone
     * else's dictionary, which the reader has to take in at a glance rather than recognise.
     */
    fun forumLanguageDirection(from: String, to: String): String = "$from to $to"
    val forumMoreOptions: String get() = "More options"
    val forumDownload: String get() = "Download"
    val forumBadgeYours: String get() = "Yours"
    val forumDownloadSucceeded: String get() = "Downloaded to your Bibliotheca"
    val forumDownloadUnavailable: String get() = "This dictionary is no longer available"
    val forumDownloadFailed: String get() = "Couldn\u2019t download the dictionary. Please try again."

    /** Shown instead of the list to someone who has not joined — an explanation, not an error. */
    val forumMembershipTitle: String get() = "Join the Forum"
    val forumMembershipMessage: String
        get() = "To browse and import shared dictionaries, go to Options \u2192 Profile, add a " +
            "display name and accept the Forum terms."

    /** Rounded to one decimal, which is what the backend sends. */
    fun forumRatingContentDescription(average: String): String = "Rated $average out of 5"

    // ---- the forum's search screen -----------------------------------------------------------
    val forumSearchScreenTitle: String get() = "Search"
    val forumSearchScreenSubtitle: String get() = "Find shared dictionaries"
    val forumSearchLanguagePairs: String get() = "Language pairs"
    val forumSearchLanguagePairsHint: String
        get() = "Each pair matches dictionaries in either direction"

    val forumSearchUser: String get() = "User"
    fun forumSearchUserTooShort(minimum: Int): String = "Enter at least $minimum characters"
    val forumAddLanguagePair: String get() = "Add language pair"
    val forumLanguagePairSheetTitle: String get() = "Add a language pair"
    val forumLanguagePairPickFirst: String get() = "Pick the first language"
    fun forumLanguagePairPickSecond(first: String): String = "Pick a language to pair with $first"
    fun forumLanguagePairChip(first: String, second: String): String = "$first \u21C4 $second"
    fun forumRemoveLanguagePair(pair: String): String = "Remove $pair"
    fun forumFilterTagsChip(tags: String): String = "Tags: $tags"
    val forumFilterTagsTitle: String get() = "Tags"
    fun forumFilterUserChip(user: String): String = "User: $user"
    val forumFilterUserHint: String get() = "User name"

    // ---- the forum's details screen ----------------------------------------------------------
    val forumDetailsCreator: String get() = "Creator"
    val forumDetailsRating: String get() = "Rating"
    val forumDetailsDownloads: String get() = "Downloads"
    val forumDetailsNoRating: String get() = "No ratings yet"
    val forumDetailsWordListHeader: String get() = "Word List"

    // ---- rating a downloaded dictionary ------------------------------------------------------
    val ratingYourRating: String get() = "Your rating"
    val ratingTapToRate: String get() = "Tap to rate"
    val ratingHint: String get() = "Tap a star to rate. Tap it again to remove your rating."
    fun ratingStarDescription(stars: Int): String = "Rate $stars out of 5"
    fun ratingCurrent(stars: Int): String = "Your rating: $stars out of 5"
    val ratingShow: String get() = "Show your rating"
    val ratingHide: String get() = "Hide your rating"
    val errorRatingFailed: String
        get() = "Couldn\u2019t save your rating. Check your connection and try again."

    val errorRatingUnavailable: String
        get() = "This dictionary is no longer available, so it can\u2019t be rated."

    // ---- downloaded dictionaries -----------------------------------------------------------------
    val dictionaryBadgeDownloaded: String get() = "Downloaded"
    val dictionaryOptionsUnsubscribe: String get() = "Unsubscribe"
    fun unsubscribeDialogTitle(name: String): String = "Unsubscribe from \u201C$name\u201D?"
    val unsubscribeDialogMessage: String
        get() = "It will be removed from your Bibliotheca, along with your practice progress on " +
            "it. You can download it again from the Forum."

    val errorUnsubscribeFailed: String
        get() = "Couldn\u2019t unsubscribe. Check your connection and try again."

    /** "by Anna Bauer" — the publisher line, shown only when this device knows the name. */
    fun forumPublishedBy(publisher: String): String = "by $publisher"

    // ---- sharing a dictionary in the Forum -------------------------------------------------------
    val dictionarySharingTitle: String get() = "Shared in the Forum"
    val dictionarySharingOn: String get() = "Forum members can see and download it"
    val dictionarySharingOff: String get() = "Only you can see it"
    val dictionarySharingLastShared: String
        get() = "Your last shared dictionary \u2014 while you\u2019re in the Forum, one must stay shared"

    fun unshareDialogTitle(name: String): String = "Stop sharing \u201C$name\u201D?"
    val unshareDialogMessage: String
        get() = "It leaves the Forum, and people who downloaded it lose access."

    val unshareDialogConfirm: String get() = "Stop sharing"
    val errorSharingFailed: String
        get() = "Couldn\u2019t change sharing. Check your connection and try again."

    val errorDeleteLastShared: String
        get() = "This is your last shared dictionary. Share another one first, or leave the Forum " +
            "to make all of them private."

    // ---- profile -------------------------------------------------------------------------------
    val optionsProfile: String get() = "Profile"
    val profileEmailLabel: String get() = "Email"
    val profileDisplayNameLabel: String get() = "Display name"
    val profileDisplayNameSupporting: String get() = "Shown to others on the dictionaries you share in the Forum"
    val profileDisplayNameRequired: String get() = "A display name is required to accept the Forum terms"
    val profileTermsTitle: String get() = "Forum terms"
    val profileTermsShow: String get() = "Show the terms"
    val profileTermsHide: String get() = "Hide the terms"
    val profileTermsAgree: String get() = "I agree to the Forum terms"

    /**
     * Deliberately **not** translated, and the one string here that is not: it stands in for legal
     * terms that do not exist yet, and a translated placeholder would read as a translated contract.
     * It goes to a lawyer before it goes to a translator.
     */
    val profileTermsText: String
        get() = "To use the Forum you agree that the dictionaries you choose to share are visible to " +
            "other Verborum users, together with your display name, and that they can import them. " +
            "You can withdraw at any time from your profile: your shared dictionaries then stop " +
            "appearing in the Forum. This is placeholder text; the full terms will follow."

    val profileUpdate: String get() = "Update"
    val profileUpdated: String get() = "Profile updated"
    val profileUpdateError: String get() = "Couldn\u2019t update your profile. Please try again."
    val leaveForumDialogTitle: String get() = "Leave the Forum?"
    val leaveForumDialogMessage: String
        get() = "All your dictionaries will become private, and people who downloaded them will " +
            "lose access. You can join again anytime."
    val leaveForumDialogConfirm: String get() = "Leave the Forum"
    val leaveForumDialogCancel: String get() = "Stay"

    // ---- sort ----------------------------------------------------------------------------------
    val sortNameAsc: String get() = "Name A–Z"
    val sortNameDesc: String get() = "Name Z–A"
    val sortNewest: String get() = "Newest first"
    val sortOldest: String get() = "Oldest first"
}

/** The base language, and the fallback every other one degrades to. */
object EnglishStrings : Strings
