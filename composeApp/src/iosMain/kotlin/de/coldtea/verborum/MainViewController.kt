package de.coldtea.verborum

import androidx.compose.ui.window.ComposeUIViewController
import de.coldtea.verborum.di.initKoin

private val koin by lazy { initKoin() }

/** The iOS entry point, called from `ContentView.swift`. */
fun MainViewController() = ComposeUIViewController {
    koin
    App()
}
