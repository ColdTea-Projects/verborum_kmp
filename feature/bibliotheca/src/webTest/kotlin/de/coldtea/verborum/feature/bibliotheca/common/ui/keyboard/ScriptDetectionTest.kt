package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import de.coldtea.verborum.core.designsystem.theme.scriptCodeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScriptDetectionTest {

    @Test
    fun `text is drawn in the script it is written in, not the language it belongs to`() {
        // The case that matters: a Latin name typed while the Arabic keyboard is open. The Arabic
        // face carries no Latin at all, so getting this wrong is a field full of empty boxes.
        assertNull(scriptCodeOf("German Basics"))
        assertEquals("ar", scriptCodeOf("كتاب"))
    }

    @Test
    fun `kana settles Japanese before the shared Han range is consulted`() {
        // Both languages write 語; only Japanese writes の, and Chinese letterforms differ.
        assertEquals("ja", scriptCodeOf("日本語の本"))
        assertEquals("zh", scriptCodeOf("汉语"))
        assertEquals("ko", scriptCodeOf("한국어"))
    }

    @Test
    fun `an empty field has no script of its own`() {
        assertNull(scriptCodeOf(""))
    }
}
