package de.coldtea.verborum.core.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiLanguageTest {

    @Test
    fun `a regional tag resolves to the language, not to English`() {
        // The app has one German, and an Austrian asking for de-AT wants it.
        assertEquals(UiLanguage.GERMAN, UiLanguage.fromTag("de-AT"))
        assertEquals(UiLanguage.PORTUGUESE, UiLanguage.fromTag("pt_BR"))
        assertEquals(UiLanguage.CHINESE, UiLanguage.fromTag("zh-Hans-CN"))
        assertEquals(UiLanguage.ENGLISH, UiLanguage.fromTag("EN"))
    }

    @Test
    fun `a language the app does not speak resolves to nothing, so the caller can fall back`() {
        assertNull(UiLanguage.fromTag("sv"))
        assertNull(UiLanguage.fromTag(""))
        assertNull(UiLanguage.fromTag(null))
    }

    @Test
    fun `every language the picker offers is actually translated`() {
        // A language listed but absent from the catalogue would silently show English, which reads
        // as a bug rather than as a fallback.
        UiLanguage.entries.forEach { language ->
            assertTrue(language in translations, "${language.code} has no catalogue")
        }
    }

    @Test
    fun `each language speaks for itself, and none is quietly English`() {
        val english = EnglishStrings.back

        UiLanguage.entries.filter { it != UiLanguage.ENGLISH }.forEach { language ->
            val translated = stringsFor(language)

            assertTrue(translated.back != english, "${language.code} never translated `back`")
            assertTrue(translated.signIn != EnglishStrings.signIn, "${language.code}: signIn")
            assertTrue(translated.saving != EnglishStrings.saving, "${language.code}: saving")
        }

        assertEquals("Zurück", stringsFor(UiLanguage.GERMAN).back)
        assertEquals("戻る", stringsFor(UiLanguage.JAPANESE).back)
    }

    @Test
    fun `every language names itself in its own script`() {
        UiLanguage.entries.forEach { language ->
            assertTrue(language.endonym.isNotBlank(), "${language.code} has no name")
        }
        // Not the English name: someone looking for their language must recognise it.
        assertEquals("Deutsch", UiLanguage.GERMAN.endonym)
        assertEquals("日本語", UiLanguage.JAPANESE.endonym)
    }

    @Test
    fun `the tour says something on every page, in every translated language`() {
        translations.values.forEach { strings ->
            listOf(
                strings.onboardingWelcomeTitle,
                strings.onboardingLibraryTitle,
                strings.onboardingTestTitle,
                strings.onboardingPracticeFlipTitle,
                strings.onboardingPracticeSwipeTitle,
            ).forEach { title -> assertTrue(title.isNotBlank()) }
        }
    }

    @Test
    fun `a chosen language outlives the device setting, and giving it up restores it`() {
        val stored = mutableMapOf<String, String>()
        val storage = object : LanguageStorage {
            override fun read() = stored["k"]
            override fun write(code: String) { stored["k"] = code }
            override fun clear() { stored.remove("k") }
        }
        val settings = LanguageSettings(storage = storage, platformLanguage = { "de-DE" })

        // The device's language is the default, without anyone having to ask.
        assertEquals(UiLanguage.GERMAN, settings.language.value)
        assertNull(settings.chosen.value)

        settings.choose(UiLanguage.JAPANESE)
        assertEquals(UiLanguage.JAPANESE, settings.language.value)
        assertEquals(UiLanguage.JAPANESE, settings.chosen.value)

        // "System language" in the picker: the choice goes back to the device, and the stored one
        // is forgotten rather than merely overridden.
        settings.followDevice()
        assertEquals(UiLanguage.GERMAN, settings.language.value)
        assertNull(settings.chosen.value)
        assertNull(storage.read())
    }

    @Test
    fun `following the device tracks it, rather than freezing today's answer`() {
        var deviceTag = "fr-FR"
        val settings = LanguageSettings(
            storage = object : LanguageStorage {
                private var stored: String? = "ja"
                override fun read() = stored
                override fun write(code: String) { stored = code }
                override fun clear() { stored = null }
            },
            platformLanguage = { deviceTag },
        )

        // Starts on the stored choice, not the device.
        assertEquals(UiLanguage.JAPANESE, settings.language.value)

        deviceTag = "el-GR"
        settings.followDevice()

        assertEquals(UiLanguage.GREEK, settings.language.value)
    }
}
