package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model

/**
 * Which way round a test asks. A dictionary records one direction — English to German — but recognising
 * *der Apfel* and producing it are different skills, so both ways round are worth being tested on.
 *
 * The prompt comes from one side of the word pair and every answer from the other, grammatical forms
 * included: reversing the direction swaps which side supplies which.
 */
internal enum class TestDirection {

    /** The dictionary's own direction: the word is asked, its translation answered. */
    WORD_TO_TRANSLATION,

    TRANSLATION_TO_WORD,
}

/**
 * One way round, named as the reader's own language names the pair — "English › German".
 *
 * The label is built where the dictionary's language codes are known, so the chooser only renders it.
 */
internal data class DirectionOption(
    val direction: TestDirection,
    val label: String,
)
