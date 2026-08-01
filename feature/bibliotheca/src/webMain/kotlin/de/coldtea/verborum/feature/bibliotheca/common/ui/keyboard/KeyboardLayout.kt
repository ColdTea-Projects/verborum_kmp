package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey

/** True for the scripts that are written and laid out right to left. */
internal fun isRightToLeft(languageCode: String): Boolean =
    languageCode.lowercase() in setOf("ar", "fa")

/** One key: what it types unshifted, and what it types shifted. */
internal data class KeyCap(val lower: String, val upper: String) {
    fun text(isShifted: Boolean): String = if (isShifted) upper else lower
}

/**
 * A language's on-screen keyboard.
 *
 * The keyboard **is** the restriction: a character can be entered only if there is a key for it, so
 * these rows are the app's per-language typeable-character contract. That contract is mirrored by
 * the Android client's field filter rather than shared with it — nothing is shared between the two
 * but the contracts — so a key added here that Android rejects produces a word one client can write
 * and the other cannot. See `docs/word-input-keyboard-webapp.md`.
 *
 * [isRtl] lays the rows out right to left, so an Arabic keyboard reads the way its script does.
 * [composesHangul] turns the Korean jamo keys into syllables as they are typed — without it the
 * field would fill with "ㅎㅏㄴ" instead of "한".
 * [note] is shown under the keys where a script cannot honestly be typed without the operating
 * system's own input method.
 */
internal data class KeyboardLayout(
    val rows: List<List<KeyCap>>,
    val isRtl: Boolean = false,
    val composesHangul: Boolean = false,
    val hasShift: Boolean = true,
    val note: String? = null,
    /**
     * The non-letter keys every language gets, beyond space.
     *
     * The apostrophe is typed inside words — French *aujourd'hui* — and the hyphen joins them.
     * Meaning separators (`/`, `،`, `・`, `、`) are deliberately **absent**: those are drawn between
     * meanings by the display layer, and each meaning is stored as its own entry in an array. A key
     * for one would let a separator be typed *into* a single surface, which is the very thing the
     * array shape exists to prevent.
     */
    val punctuation: List<KeyCap> = DEFAULT_PUNCTUATION,
    /**
     * Characters accepted in a field though no key types them.
     *
     * Needed wherever the keyboard is *phonetic* rather than complete. Korean keys are jamo and the
     * composer turns them into syllables; the Chinese keyboard is bopomofo but a Chinese word is
     * hanzi; the Japanese keyboard is kana but Japanese is written with kanji too. Restricting those
     * to their key caps would make the language impossible to write. The range still restricts —
     * Latin cannot be typed into a Chinese word — it just restricts to the *script* rather than to
     * the keys.
     */
    val scriptRanges: List<CharRange> = emptyList(),
) {
    /**
     * Whether [char] may appear in a field using this keyboard.
     *
     * The keyboard is the restriction: what it cannot type is not accepted from the physical
     * keyboard or from a paste either.
     */
    fun accepts(char: Char): Boolean =
        char == ' ' ||
            char in typeable ||
            scriptRanges.any { range -> char in range }

    private val typeable: Set<Char> by lazy {
        buildSet {
            (rows.flatten() + punctuation).forEach { key ->
                addAll(key.lower.toSet())
                addAll(key.upper.toSet())
            }
        }
    }
}

/** The typographic apostrophe, normalised to the plain one so a word has a single spelling. */
internal const val CURLY_APOSTROPHE = '\u2019'
internal const val PLAIN_APOSTROPHE = '\''

private val ARABIC = '\u0600'..'\u06FF'
private val HIRAGANA = '\u3040'..'\u309F'
private val KATAKANA = '\u30A0'..'\u30FF'
private val BOPOMOFO = '\u3105'..'\u312F'
private val KANJI = '\u4E00'..'\u9FFF'
private val KANJI_EXTENDED = '\u3400'..'\u4DBF'
private val HANGUL_SYLLABLES = '\uAC00'..'\uD7A3'
private val HANGUL_JAMO = '\u3130'..'\u318F'

private val DEFAULT_PUNCTUATION = listOf(
    KeyCap(PLAIN_APOSTROPHE.toString(), PLAIN_APOSTROPHE.toString()),
    KeyCap("-", "-"),
)

/**
 * The keyboard for a language, or null where the app has no layout for its script.
 *
 * Latin-script languages get their own national arrangement — QWERTZ for German, AZERTY for French,
 * the Turkish F-less QWERTY — because a learner typing that language expects the keys where that
 * language puts them, and the accented letters they need are on the top row rather than behind a
 * modifier.
 */
internal fun keyboardLayoutFor(
    languageCode: String,
    fieldKey: FieldKey? = null,
): KeyboardLayout? {
    // A Chinese word is hanzi, but its reading is pinyin — two different keyboards for one language,
    // which is why the field and not just the language decides.
    if (languageCode.lowercase() == "zh" && fieldKey == FieldKey.READING) return pinyin()

    return baseLayoutFor(languageCode)
}

private fun baseLayoutFor(languageCode: String): KeyboardLayout? =
    when (languageCode.lowercase()) {
        "en" -> latin()
        "de" -> latin(extras = "äöüß", home = "asdfghjklöä", top = "qwertzuiopü", bottom = "yxcvbnm")
        "fr" -> latin(
            extras = "éèêëàâçùûôîï",
            top = "azertyuiop",
            home = "qsdfghjklm",
            bottom = "wxcvbn",
        )
        "es" -> latin(extras = "áéíóúüñ¿¡", home = "asdfghjklñ")
        // Italian has 21 letters; j, k, w, x and y belong to words borrowed from elsewhere.
        "it" -> latin(extras = "àèéìòù", unused = "jkwxy")
        "pt" -> latin(extras = "ãõáéíóúâêôçà")
        "nl" -> latin(extras = "éëïöü")
        "lt" -> latin(extras = "ąčęėįšųūž", unused = "qwx")
        "pl" -> latin(extras = "ąćęłńóśźż", unused = "qvx")
        "tr" -> latin(
            extras = "ğüşıöç",
            top = "qwertyuıopğü",
            home = "asdfghjklşi",
            bottom = "zxcvbnmöç",
            unused = "qwx",
        )
        "az" -> latin(
            extras = "əğıöüçş",
            top = "qwertyuiopöğ",
            home = "asdfghjklıə",
            bottom = "zxcvbnmçş",
            unused = "w",
        )

        "ru" -> cyrillic("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю")
        "uk" -> cyrillic("йцукенгшщзхї", "фівапролджє", "ячсмитьбю", extras = "ґ'")

        "el" -> KeyboardLayout(
            rows = listOf(
                letters("ςερτυθιοπ"),
                letters("ασδφγηξκλ"),
                letters("ζχψωβνμ"),
                letters("άέήίόύώϊϋ"),
            ),
        )

        // Arabic and Persian share a script but not a keyboard: Persian adds پ چ ژ گ and puts ی
        // and ک where Arabic has ي and ك.
        "ar" -> KeyboardLayout(
            rows = listOf(
                unshifted("ضصثقفغعهخحجد"),
                unshifted("شسيبلاتنمكط"),
                unshifted("ئءؤرذىةوزظ"),
                unshifted("أإآّ"),
            ),
            isRtl = true,
            hasShift = false,
            // The whole Arabic block, so harakat and the letterforms a word is written with are
            // accepted even where no key types them.
            scriptRanges = listOf(ARABIC),
        )
        "fa" -> KeyboardLayout(
            rows = listOf(
                unshifted("ضصثقفغعهخحجچ"),
                unshifted("شسیبلاتنمکگ"),
                unshifted("ظطزرذدپوژ"),
                unshifted("آأؤئء"),
            ),
            isRtl = true,
            hasShift = false,
            scriptRanges = listOf(ARABIC),
        )

        // Hiragana unshifted, katakana shifted — the two kana syllabaries map one to one.
        "ja" -> KeyboardLayout(
            rows = listOf(
                kana("あいうえお", "アイウエオ"),
                kana("かきくけこさしすせそ", "カキクケコサシスセソ"),
                kana("たちつてとなにぬねの", "タチツテトナニヌネノ"),
                kana("はひふへほまみむめも", "ハヒフヘホマミムメモ"),
                kana("やゆよらりるれろわをん", "ヤユヨラリルレロワヲン"),
                kana("がぎぐげござじずぜぞ", "ガギグゲゴザジズゼゾ"),
                kana("だぢづでどばびぶべぼぱぴぷぺぽ", "ダヂヅデドバビブベボパピプペポ"),
                kana("っゃゅょーぁぃぅぇぉ", "ッャュョーァィゥェォ"),
            ),
            // Japanese is written with kanji as well as kana, and no on-screen keyboard can offer
            // those — they come from the system input method.
            scriptRanges = listOf(HIRAGANA, KATAKANA, KANJI, KANJI_EXTENDED),
        )

        // Jamo, composed into syllables as they are typed.
        "ko" -> KeyboardLayout(
            rows = listOf(
                jamo("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ"),
                jamo("ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ"),
                jamo("ㅋㅌㅊㅍㅠㅜㅡ", "ㅋㅌㅊㅍㅠㅜㅡ"),
            ),
            composesHangul = true,
            // The keys are jamo; what ends up in the field is the syllables they compose into.
            scriptRanges = listOf(HANGUL_SYLLABLES, HANGUL_JAMO),
        )

        // Bopomofo, in the standard Dachen arrangement — the phonetic alphabet a Chinese keyboard
        // actually types, rather than the Latin one a learner would find unhelpful here. Hanzi still
        // need a conversion dictionary, which is what the note says.
        "zh" -> KeyboardLayout(
            rows = listOf(
                unshifted("ㄅㄉㄓˊˇˋ˙ㄚㄞㄢㄦ"),
                unshifted("ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ"),
                unshifted("ㄇㄋㄎㄑㄕㄘㄨㄜㄠㄤ"),
                unshifted("ㄈㄌㄏㄒㄖㄙㄩㄝㄡㄥ"),
            ),
            hasShift = false,
            note = "Bopomofo — use your system input method to convert to characters.",
            // A Chinese word is hanzi; bopomofo is only how it is spelled out.
            scriptRanges = listOf(BOPOMOFO, KANJI, KANJI_EXTENDED),
        )

        else -> null
    }

/** Pinyin with the tone-marked vowels, for the Chinese `reading` field. */
private fun pinyin(): KeyboardLayout = KeyboardLayout(
    rows = listOf(
        letters("qwertyuiop"),
        letters("asdfghjkl"),
        letters("zxcvbnm"),
        letters("āáǎàēéěèīíǐì"),
        letters("ōóǒòūúǔùǖǘǚǜü"),
    ),
)

/**
 * A Latin keyboard: the national arrangement, with the language's own letters on a fourth row.
 *
 * [unused] drops the letters a language does not write with — Italian has no j, k, w, x or y, and a
 * key for a letter that never appears in the language is a key in the way. A loanword that needs one
 * can still be typed on the physical keyboard; this one is for the letters the *language* uses.
 */
private fun latin(
    extras: String = "",
    top: String = "qwertyuiop",
    home: String = "asdfghjkl",
    bottom: String = "zxcvbnm",
    unused: String = "",
): KeyboardLayout = KeyboardLayout(
    rows = listOfNotNull(
        letters(top.filterNot { it in unused }),
        letters(home.filterNot { it in unused }),
        letters(bottom.filterNot { it in unused }),
        letters(extras).takeIf { it.isNotEmpty() },
    ),
)

private fun cyrillic(top: String, home: String, bottom: String, extras: String = ""): KeyboardLayout =
    KeyboardLayout(
        rows = listOfNotNull(
            letters(top),
            letters(home),
            letters(bottom),
            letters(extras).takeIf { it.isNotEmpty() },
        ),
    )

/** Cased letters: shift gives the capital. */
private fun letters(source: String): List<KeyCap> =
    source.map { char -> KeyCap(char.toString(), char.uppercase()) }

/** Scripts without letter case, where shift would have nothing to give. */
private fun unshifted(source: String): List<KeyCap> =
    source.map { char -> KeyCap(char.toString(), char.toString()) }

private fun kana(hiragana: String, katakana: String): List<KeyCap> =
    hiragana.mapIndexed { index, char ->
        KeyCap(char.toString(), katakana.getOrNull(index)?.toString() ?: char.toString())
    }

private fun jamo(plain: String, shifted: String): List<KeyCap> =
    plain.mapIndexed { index, char ->
        KeyCap(char.toString(), shifted.getOrNull(index)?.toString() ?: char.toString())
    }
