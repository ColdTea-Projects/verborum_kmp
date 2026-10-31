package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The app language reaches the login page: read at each attempt, so a changed language applies to the next one. */
class UiLocalesTest {

    private val echoesState: (String) -> AuthorizationResult =
        { state -> AuthorizationResult.Code(code = "code-1", state = state) }

    private val tokenResponse = """
        {
          "access_token": "${jwtWithClaims("""{"sub":"user-42"}""")}",
          "refresh_token": "refresh-1",
          "expires_in": 300
        }
    """.trimIndent()

    private fun service(launcher: FakeAuthorizationLauncher, language: () -> String?): AuthService {
        val session = AuthSession(
            storage = InMemoryTokenStorage(),
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
            nowEpochSeconds = { 0L },
        )
        return AuthService(
            config = testConfig,
            session = session,
            client = RecordingAuthClient(status = HttpStatusCode.OK, body = tokenResponse).client,
            launcher = launcher,
            pendingStore = FakePendingAuthorizationStore(retainSaves = true),
            uiLocale = language,
        )
    }

    private fun FakeAuthorizationLauncher.uiLocales() = Url(authorizedUrl!!).parameters["ui_locales"]

    @Test
    fun `sign in and sign up send the app language`() = runTest {
        val launcher = FakeAuthorizationLauncher(echoesState)
        val service = service(launcher) { "tr" }

        service.signIn(AuthEntry.SignIn)
        assertEquals("tr", launcher.uiLocales())

        service.signIn(AuthEntry.CreateAccount)
        assertEquals("tr", launcher.uiLocales())
    }

    @Test
    fun `a language changed between two logins applies to the next one`() = runTest {
        val launcher = FakeAuthorizationLauncher(echoesState)
        var language = "en"
        val service = service(launcher) { language }

        service.signIn(AuthEntry.SignIn)
        assertEquals("en", launcher.uiLocales())

        language = "ja"
        service.signIn(AuthEntry.SignIn)
        assertEquals("ja", launcher.uiLocales())
    }

    @Test
    fun `a service built without a language sends none`() = runTest {
        val launcher = FakeAuthorizationLauncher(echoesState)
        val service = service(launcher) { null }

        service.signIn(AuthEntry.SignIn)

        assertEquals(null, launcher.uiLocales())
    }
}
