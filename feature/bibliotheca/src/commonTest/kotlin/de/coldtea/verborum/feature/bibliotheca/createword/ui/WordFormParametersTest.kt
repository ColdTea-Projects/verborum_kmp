package de.coldtea.verborum.feature.bibliotheca.createword.ui

import org.koin.core.parameter.parametersOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * How the word form's arguments survive the trip through Koin.
 *
 * `koinViewModel` does not pass the holder the call site built: `AndroidParametersHolder` rebuilds
 * one from its *values* alone, so anything the call site configured about how to read them — an
 * index, for instance — is gone by the time the module looks. What is left is a plain holder, which
 * is what these tests use.
 */
class WordFormParametersTest {

    @Test
    fun `the arguments arrive as one value of their own type`() {
        val creating = parametersOf(WordFormArgs("dict-1", wordId = null))
        val editing = parametersOf(WordFormArgs("dict-1", wordId = "word-1"))

        assertEquals(WordFormArgs("dict-1", null), creating.get<WordFormArgs>())
        assertEquals(WordFormArgs("dict-1", "word-1"), editing.get<WordFormArgs>())
    }

    @Test
    fun `two strings would hand the dictionary id back as the word id`() {
        // Why they are not passed as a pair. An absent optional parameter is looked for by position
        // and, finding null, falls back to the first value of the same type — so "Add word" received
        // the dictionary id as a word id and reported that the word could not be loaded.
        val creating = parametersOf("dict-1", null)

        assertEquals("dict-1", creating.get<String>())
        assertEquals("dict-1", creating.getOrNull<String>())
    }
}
