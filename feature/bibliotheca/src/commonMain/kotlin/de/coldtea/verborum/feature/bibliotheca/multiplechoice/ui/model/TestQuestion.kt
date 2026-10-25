package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model

import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.displayLine
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.formsOf

/**
 * One quiz item. A word produces a base question (word → translation) plus one per grammatical form
 * present on **both** sides, so `go/went/gone → gehen/ging/gegangen` asks three separate things.
 *
 * [formKey] names the form asked about, or null for the base question.
 */
internal data class TestQuestion(
    val wordId: String,
    val prompt: String,
    val answer: String,
    val formKey: FieldKey? = null,
) {
    /** "Past", shown above the prompt so the question is not ambiguous. */
    fun formLabel(strings: Strings): String? = formKey?.label(strings)
}

/** The question and the four answers it is asked with, one of them [TestQuestion.answer]. */
internal data class TestChoice(
    val question: TestQuestion,
    val choices: List<String>,
)

/**
 * A word's questions: the base pair, then every form both languages recorded. A form only one side
 * has cannot be asked — there would be no answer to mark correct.
 *
 * [direction] decides which side asks and which answers; everything below reads from the chosen
 * sides rather than from the dictionary's own, so the two directions share this one shape.
 */
internal fun Word.toQuestions(direction: TestDirection): List<TestQuestion> {
    val isForward = direction == TestDirection.WORD_TO_TRANSLATION
    val (promptSurfaces, promptMeta) = if (isForward) word to wordMeta else translation to translationMeta
    val (answerSurfaces, answerMeta) = if (isForward) translation to translationMeta else word to wordMeta

    val promptForms = formsOf(promptMeta)
    val answerForms = formsOf(answerMeta)

    return buildList {
        add(
            TestQuestion(
                wordId = wordId,
                prompt = displayLine(promptSurfaces, promptMeta),
                answer = displayLine(answerSurfaces, answerMeta),
            ),
        )

        FieldKey.entries.forEach { key ->
            val prompt = promptForms[key]
            val answer = answerForms[key]

            if (prompt != null && answer != null) {
                add(TestQuestion(wordId = wordId, prompt = prompt, answer = answer, formKey = key))
            }
        }
    }
}
