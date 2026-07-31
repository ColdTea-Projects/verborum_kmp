package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

/**
 * Combines Korean jamo into syllables as they are typed, so the Korean keyboard produces "한" rather
 * than "ㅎㅏㄴ".
 *
 * Hangul syllables are laid out arithmetically in Unicode — `0xAC00 + (lead * 21 + vowel) * 28 +
 * tail` — which is what makes this possible without a dictionary. Only the character immediately
 * before the cursor is ever reconsidered: the composer takes the text so far and one new jamo, and
 * answers with what the tail of the text should become.
 */
internal object HangulComposer {

    private const val BASE = 0xAC00
    private const val VOWEL_COUNT = 21
    private const val TAIL_COUNT = 28
    private const val LAST = BASE + 19 * VOWEL_COUNT * TAIL_COUNT - 1

    private const val LEADS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private const val VOWELS = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"

    /** Index 0 is "no tail", so the string is deliberately one character short of the others. */
    private const val TAILS = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"

    /** Tails that are themselves two jamo, so a following vowel can steal the second one back. */
    private val compoundTails = mapOf(
        "ㄳ" to ("ㄱ" to "ㅅ"), "ㄵ" to ("ㄴ" to "ㅈ"), "ㄶ" to ("ㄴ" to "ㅎ"),
        "ㄺ" to ("ㄹ" to "ㄱ"), "ㄻ" to ("ㄹ" to "ㅁ"), "ㄼ" to ("ㄹ" to "ㅂ"),
        "ㄽ" to ("ㄹ" to "ㅅ"), "ㄾ" to ("ㄹ" to "ㅌ"), "ㄿ" to ("ㄹ" to "ㅍ"),
        "ㅀ" to ("ㄹ" to "ㅎ"), "ㅄ" to ("ㅂ" to "ㅅ"),
    )

    /** Vowels that merge with a following vowel — ㅗ + ㅏ becomes ㅘ. */
    private val compoundVowels = mapOf(
        ("ㅗ" to "ㅏ") to "ㅘ", ("ㅗ" to "ㅐ") to "ㅙ", ("ㅗ" to "ㅣ") to "ㅚ",
        ("ㅜ" to "ㅓ") to "ㅝ", ("ㅜ" to "ㅔ") to "ㅞ", ("ㅜ" to "ㅣ") to "ㅟ",
        ("ㅡ" to "ㅣ") to "ㅢ",
    )

    /** Consonants that merge into a compound tail — ㄹ + ㄱ becomes ㄺ. */
    private val compoundTailPairs = compoundTails.entries.associate { (tail, parts) -> parts to tail }

    /**
     * The result of typing [jamo] after [before]: how many characters to drop from the end of
     * [before], and what to append in their place.
     */
    data class Result(val backspaces: Int, val text: String)

    fun compose(before: String, jamo: String): Result {
        val previous = before.lastOrNull()?.toString() ?: return Result(0, jamo)

        return when {
            previous.isSyllable() -> composeOntoSyllable(previous, jamo)
            previous.isVowel() && jamo.isVowel() ->
                compoundVowels[previous to jamo]?.let { Result(1, it) } ?: Result(0, jamo)

            // A lead consonant followed by a vowel is the start of a syllable.
            previous.isLead() && jamo.isVowel() -> Result(1, syllable(previous, jamo, null))
            else -> Result(0, jamo)
        }
    }

    private fun composeOntoSyllable(syllable: String, jamo: String): Result {
        val code = syllable.single().code - BASE
        val lead = LEADS[code / (VOWEL_COUNT * TAIL_COUNT)].toString()
        val vowel = VOWELS[code / TAIL_COUNT % VOWEL_COUNT].toString()
        val tail = TAILS[code % TAIL_COUNT].toString().takeIf { it != " " }

        return when {
            // No tail yet: a consonant becomes one, a vowel may merge with the one already there.
            tail == null && jamo.isTail() -> Result(1, syllable(lead, vowel, jamo))
            tail == null && jamo.isVowel() ->
                compoundVowels[vowel to jamo]
                    ?.let { merged -> Result(1, syllable(lead, merged, null)) }
                    ?: Result(0, jamo)

            // A tail plus a consonant may compound; a tail plus a vowel hands the tail over to the
            // next syllable, which is how "한" + ㅏ becomes "하나".
            tail != null && jamo.isTail() ->
                compoundTailPairs[tail to jamo]
                    ?.let { compound -> Result(1, syllable(lead, vowel, compound)) }
                    ?: Result(0, jamo)

            tail != null && jamo.isVowel() -> {
                val (kept, moved) = compoundTails[tail] ?: (null to tail)
                Result(1, syllable(lead, vowel, kept) + syllable(moved, jamo, null))
            }

            else -> Result(0, jamo)
        }
    }

    private fun syllable(lead: String, vowel: String, tail: String?): String {
        val leadIndex = LEADS.indexOf(lead)
        val vowelIndex = VOWELS.indexOf(vowel)
        // A jamo that cannot head or fill a syllable is emitted as itself rather than mangled.
        if (leadIndex < 0 || vowelIndex < 0) return lead + vowel + tail.orEmpty()

        val tailIndex = tail?.let { TAILS.indexOf(it) }?.takeIf { it > 0 } ?: 0

        return ((BASE + (leadIndex * VOWEL_COUNT + vowelIndex) * TAIL_COUNT + tailIndex)).toChar()
            .toString()
    }

    private fun String.isSyllable(): Boolean = single().code in BASE..LAST

    private fun String.isLead(): Boolean = this in LEADS.map(Char::toString)

    private fun String.isVowel(): Boolean = this in VOWELS.map(Char::toString)

    private fun String.isTail(): Boolean = TAILS.indexOf(this) > 0
}
