package de.coldtea.verborum.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerborumTopBarControllerTest {

    @Test
    fun `starts with no chrome`() {
        val controller = VerborumTopBarController()

        assertEquals("", controller.state.title)
        assertNull(controller.state.subtitle)
        assertFalse(controller.state.showBackButton)
        assertNull(controller.state.action)
    }

    @Test
    fun `registering replaces the whole state rather than merging it`() {
        val controller = VerborumTopBarController()

        controller.register(
            VerborumTopBarState(title = "Bibliotheca", subtitle = "3 entries", showBackButton = false),
        )
        controller.register(VerborumTopBarState(title = "amor", showBackButton = true))

        assertEquals("amor", controller.state.title)
        // The previous screen's subtitle must not linger on the next screen's header.
        assertNull(controller.state.subtitle)
        assertTrue(controller.state.showBackButton)
    }

    @Test
    fun `a screen leaving clears the header it owns`() {
        val controller = VerborumTopBarController()
        val token = controller.register(VerborumTopBarState(title = "amor", showBackButton = true))

        controller.unregister(token)

        assertEquals(VerborumTopBarState(), controller.state)
    }

    @Test
    fun `a departing screen cannot blank the header that replaced it`() {
        // The regression this guards: screens overlap during a navigation transition, so the
        // outgoing screen's disposal runs *after* the incoming screen has registered. Clearing
        // unconditionally there left the new screen with no header until something re-registered it.
        val controller = VerborumTopBarController()
        val outgoing = controller.register(VerborumTopBarState(title = "Bibliotheca"))
        controller.register(VerborumTopBarState(title = "amor", showBackButton = true))

        controller.unregister(outgoing)

        assertEquals("amor", controller.state.title)
        assertTrue(controller.state.showBackButton)
    }

    @Test
    fun `unregistering twice is harmless`() {
        val controller = VerborumTopBarController()
        val token = controller.register(VerborumTopBarState(title = "amor"))

        controller.unregister(token)
        controller.register(VerborumTopBarState(title = "Forum"))
        controller.unregister(token)

        // The stale token must not take the current header with it.
        assertEquals("Forum", controller.state.title)
    }
}
