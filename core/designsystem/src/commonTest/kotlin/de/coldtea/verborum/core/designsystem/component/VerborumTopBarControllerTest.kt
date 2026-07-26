package de.coldtea.verborum.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerborumTopBarControllerTest {

    @Test
    fun `starts with no chrome`() {
        val controller = VerborumTopBarController()

        assertEquals("", controller.state.title)
        assertNull(controller.state.subtitle)
        assertTrue(!controller.state.showBackButton)
        assertNull(controller.state.action)
    }

    @Test
    fun `registering replaces the whole state rather than merging it`() {
        val controller = VerborumTopBarController()

        controller.update(
            VerborumTopBarState(title = "Bibliotheca", subtitle = "3 entries", showBackButton = false),
        )
        controller.update(VerborumTopBarState(title = "amor", showBackButton = true))

        assertEquals("amor", controller.state.title)
        // The previous screen's subtitle must not linger on the next screen's header.
        assertNull(controller.state.subtitle)
        assertTrue(controller.state.showBackButton)
    }

    @Test
    fun `clearing drops the chrome so the next screen cannot inherit it`() {
        val controller = VerborumTopBarController()
        controller.update(VerborumTopBarState(title = "amor", showBackButton = true))

        controller.clear()

        assertEquals(VerborumTopBarState(), controller.state)
    }
}
