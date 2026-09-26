package de.coldtea.verborum.core.designsystem.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the split behind [scriptedText]. On the web each run is drawn in its own face, and a face
 * for one script carries nothing of another — so a run given the wrong script is a stretch of empty
 * boxes on screen.
 */
class ScriptRunsTest {

    @Test
    fun `a sentence keeps its own script and the word inside it keeps another`() {
        val runs = scriptRuns("What does “犬” mean?")

        assertTrue(runs.any { it.script == "zh" && it.text.contains("犬") })
        assertTrue(runs.any { it.script == DEFAULT_SCRIPT && it.text.contains("What does") })
    }

    @Test
    fun `kana settles Japanese for the whole string rather than character by character`() {
        // 語 and 本 are written by both languages; only の is Japanese. Deciding per character would
        // hand most of this sentence to the Chinese face, whose letterforms differ.
        assertEquals(listOf(ScriptRun("ja", "日本語の本")), scriptRuns("日本語の本"))
        assertEquals(listOf(ScriptRun("zh", "汉语")), scriptRuns("汉语"))
    }

    @Test
    fun `a bracket belongs to the script it opens`() {
        // The Japanese catalogue asks 「X」の意味は？ — a Latin face has no 「 to draw. (The kana
        // is what makes this Japanese rather than Chinese; a bracketed kanji on its own is neither.)
        assertEquals(listOf(ScriptRun("ja", "「本」の")), scriptRuns("「本」の"))
    }

    @Test
    fun `punctuation at the end of a string keeps the script it follows`() {
        val runs = scriptRuns("كتاب.")

        assertEquals(listOf(ScriptRun("ar", "كتاب.")), runs)
    }

    @Test
    fun `text the default face draws is one run and no script`() {
        assertEquals(listOf(ScriptRun(DEFAULT_SCRIPT, "German Basics")), scriptRuns("German Basics"))
        assertEquals(emptyList(), scriptRuns(""))
    }

    @Test
    fun `the runs always add back up to the text they came from`() {
        // Every character is drawn exactly once, whatever the splitting decides about it.
        listOf(
            "What does “犬” mean?",
            "「日本語」の意味は？",
            "«كتاب» yani ne?",
            "“한국어 사전” and all of its words will be deleted.",
            "",
        ).forEach { text ->
            assertEquals(text, scriptRuns(text).joinToString("") { it.text })
        }
    }
}
