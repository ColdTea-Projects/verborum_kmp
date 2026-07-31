package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

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
)

/**
 * The keyboard for a language, or null where the app has no layout for its script.
 *
 * Latin-script languages get their own national arrangement — QWERTZ for German, AZERTY for French,
 * the Turkish F-less QWERTY — because a learner typing that language expects the keys where that
 * language puts them, and the accented letters they need are on the top row rather than behind a
 * modifier.
 */
internal fun keyboardLayoutFor(languageCode: String): KeyboardLayout? =
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
                letters(";ςερτυθιοπ"),
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
        )

        // Jamo, composed into syllables as they are typed.
        "ko" -> KeyboardLayout(
            rows = listOf(
                jamo("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ"),
                jamo("ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ"),
                jamo("ㅋㅌㅊㅍㅠㅜㅡ", "ㅋㅌㅊㅍㅠㅜㅡ"),
            ),
            composesHangul = true,
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
        )

        else -> null
    }

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
