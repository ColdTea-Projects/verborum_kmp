package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model

import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.formsOf
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.translationLine
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.wordLine

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
    val formLabel: String? get() = formKey?.label
}

/** The question and the four answers it is asked with, one of them [TestQuestion.answer]. */
internal data class TestChoice(
    val question: TestQuestion,
    val choices: List<String>,
)

/**
 * A word's questions: the base pair, then every form both languages recorded. A form only one side
 * has cannot be asked — there would be no answer to mark correct.
 */
internal fun Word.toQuestions(): List<TestQuestion> {
    val promptForms = formsOf(wordMeta)
    val answerForms = formsOf(translationMeta)

    return buildList {
        add(TestQuestion(wordId = wordId, prompt = wordLine(), answer = translationLine()))

        FieldKey.entries.forEach { key ->
            val prompt = promptForms[key]
            val answer = answerForms[key]

            if (prompt != null && answer != null) {
                add(TestQuestion(wordId = wordId, prompt = prompt, answer = answer, formKey = key))
            }
        }
    }
}
