package de.coldtea.verborum

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import de.coldtea.verborum.di.initKoin

/** The web entry point; Compose attaches its canvas to the document body. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()

    ComposeViewport {
        App()
    }
}
