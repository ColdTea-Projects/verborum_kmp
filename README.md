This is a Kotlin Multiplatform project targeting iOS and Web.

## Module layout

```
verborum_kmp/
├── build-logic/                 # Gradle convention plugins (KMP / Compose / feature)
├── gradle/libs.versions.toml    # the single version catalog
│
├── composeApp/                  # the shell: theme, app chrome, nav graph, Koin bootstrap
│   ├── commonMain/              #   App(), NavigationCentral, VerborumNavHost, TopLevelDestination, di/AppModule
│   ├── iosMain/                 #   MainViewController()
│   └── webMain/                 #   main() + index.html (compiles for both js and wasmJs)
├── iosApp/                      # Xcode wrapper (Swift @main → composeApp)
│
├── core/
│   ├── designsystem/            #   VerborumTheme, M3 colours, shared composables, chrome, icons
│   ├── common/                  #   BaseViewModel, Outcome, VerborumError, Envelope DTOs, connectivity
│   ├── network/                 #   Ktor client, base URL per target, error mapping
│   ├── auth/                    #   token storage (expect/actual), PKCE, refresh
│   └── database/                #   optional local cache — real on iOS, no-op on web
│
└── feature/
    ├── auth/                    # the login wall (Keycloak, Authorization Code + PKCE)
    ├── bibliotheca/             # the library — one folder per screen slice
    │   ├── common/              #   shared inside the feature: dictionary + word layers, SyncService
    │   ├── dictionarylist/      #   di / ui for the dictionary list
    │   ├── dictionarydetails/   #   di / ui for one dictionary's words
    │   ├── selfpractice/        #   shared session logic; the UI forks per platform
    │   ├── multiplechoice/      #   the test — one design for both platforms
    │   ├── createdictionary/    #   the dictionary form — create and edit
    │   └── createword/          #   the word form — create and edit
    ├── forum/                   # marketplace
    ├── onboarding/              # the welcome tour; when it shows differs per platform
    └── options/                 # the Options tab — signing out lives here
```

Dependencies point one way only: `composeApp → feature/* → core/*`. The shell knows each
feature's nav graph entry point and nothing else; features never depend on each other.

### The navigation centre

`NavigationCentral` (in `composeApp/navigation/`) is the single place that knows the app's shape —
modelled on `NavigationCentral` in the Android app, with type-safe `@Serializable` routes instead of
string routes:

- **One nav host.** Each feature contributes a nested graph (`bibliothecaGraph()`); the shell never
  names a screen.
- **One header.** A screen declares its own with `RegisterTopBar(title, subtitle, showBackButton,
  action)` from `core:designsystem`; the shell renders it and owns the back button. Registering
  nothing means no chrome at all, which is what a full-screen destination such as onboarding wants.
- **One snackbar.** `LocalSnackbarHostState` is provided here; `ShowSnackbarMessages(flow)` pipes a
  view model's messages onto it.
- **One offline banner**, pinned under the header, driven by `observeConnectivity()` in `core:common`
  (`NWPathMonitor` on iOS, `online`/`offline` window events on web).
- **Tabs from `TopLevelDestination`**: a `NavigationBar` on phone widths, a `NavigationRail` at
  ≥ 840dp (desktop browser, iPad), and only on a tab root — deeper screens are left via the header's
  back button, so the tabs cannot swallow the back stack.

### Login

Authorization Code + PKCE against the Keycloak `verborum` realm, in `core:auth`, with the screen in
`feature:auth`. Signing in and creating an account are the same flow pointed at different endpoints —
there is no password field in the app.

| | iOS | Web |
|---|---|---|
| Browser leg | `ASWebAuthenticationSession` | top-level redirect; code stripped from the URL |
| Redirect | `de.coldtea.verborum://oauth2redirect/cb` (`Info.plist`) | the app's own origin |
| PKCE verifier | in memory | `sessionStorage` (must survive the reload) |
| Tokens | Keychain, `…AfterFirstUnlockThisDeviceOnly` | `localStorage` — the session lasts until sign-out |
| Endpoints | `https://auth.verborum.coldtea.de/realms/verborum` | same-origin `/auth/realms/verborum` |

**Web is same-origin everywhere.** Deployed, a reverse proxy serves `/auth` and `/api` next to the
app; in development the dev server does the same (`composeApp/webpack.config.d/devServerProxy.js`
→ Keycloak on `:8180`, `ms_dictionary` on `:8085`). So the token exchange and every API call are
same-origin: no CORS at any point, no **Web origins** entry to keep in step with the dev port, and
`connect-src 'self'` stays valid.

Local Keycloak (realm `verborum`, client `verborum-app`) still needs its **Valid redirect URIs** to
include `http://localhost:8280/*` — that is a browser navigation, not an XHR, so no proxy can cover
it. The dev port (**8280**) is set once in `composeApp/build.gradle.kts`.

iOS talks to the services directly and keeps its `https` issuer: pointing it at a local Keycloak over
plain http would need an ATS exception, which this repo does not ship.

Signing out is the Options tab's only action today; it ends the session, and the shell swaps the app
for the login wall on its own. `AuthService` is the only entry point the UI touches, `AuthSession.sessionState` is the gate the shell
watches (`Unknown` → neither wall nor app, so no login flash for a signed-in user), and a failed
refresh clears the session — so a signed-in user stays signed in across restarts for as long as the
`offline_access` refresh token lives. Both `*-security` skills describe the storage trade-offs: on web
the tokens are readable by any script on the origin, which is the price of not logging out when the tab
closes. The end state is a `HttpOnly` refresh-token cookie, and it needs backend work.

### The dictionary list

The bibliotheca tab's root screen, and where a user lands after signing in. Ported from the Android
app's `bibliotheca/dictionary` package, keeping its structure: search, From/To language filters, sort
order, pull-to-refresh, an overflow per row with edit/delete, and skeleton rows on first load.

Tapping a row opens **dictionary details**: the two practice modes, the words it holds (each with its
practice progress, edit and delete), and deleting the dictionary itself. A mode that cannot start says
why rather than sitting there inert — self practice needs one word, a test needs four distinct ones.

`SyncService` (`bibliotheca/common/domain`) is the download half of the sync. It reads the signed-in
user from the auth session and pulls `GET dictionaries/{userId}` plus, in one further request,
`GET words/user/{userId}` — which is what puts a word count on every list row. The details screen
pulls just its own `GET words/dictionary/{id}`. Both land in `DictionaryStore` / `WordStore`, the
in-memory single sources of truth the screens observe. Deletes tombstone locally first, then ask the
server, and restore the row if the server refuses.

The deliberate gap: there is **no local database**, so both stores are empty again after a restart
until the first sync completes. Creating and editing dictionaries and words, and the two practice
screens, are still to come; until then those taps report themselves on the snackbar.

Local dev needs `ms_dictionary` running on `http://localhost:8085`; the dev server proxies `/api` to
it, so no CORS configuration is required on the service.

### The test

Multiple choice over a dictionary's words, ported from Android and **not** forked — a question card
over two actions reads the same with a finger or a mouse — inside the app's usual capped column.

A word contributes one question per grammatical form both languages recorded, so `go/went/gone →
gehen/ging/gegangen` asks three separate things. Wrong answers are drawn from **every dictionary
sharing the language pair**, preferring the same form, so a past-tense question is offered other past
tenses; that is also why the test needs four distinct entries in the pair before it will start.

Scoring and levelling are deliberately different things: the score counts every correct *question*,
while a word's *level* moves at most one rung each way per run — the first correct answer raises it,
the first wrong answer lowers it, and both together leave it where it started. The delta is measured
against the level as it stood when the run began, not the current one, since each save flows straight
back through the observed words.

### The two forms — dictionary and word

`createdictionary` is a name, two language dropdowns and the tag catalogue (level / topic / exam).
A new dictionary gets its id client-side, so the row can appear in the list before the server has
answered; saving a new one opens it, saving an edit returns where it came from.

`createword` follows the dictionary's two languages. The part of speech comes in two layers: the
four open classes — noun, verb, adjective, adverb — carry grammatical forms of their own, while every
closed class (free text, preposition, pronoun, numeral, conjunction, interjection, article) sits
under `WordCategory.OTHER` and asks for nothing beyond the word itself. **iOS** shows the five
categories as chips and lets "Other" name itself in a dropdown, as Android does; **web** has the room
to give all eleven types a chip of their own and drops the "Other" bucket entirely. Free text stores
no `type` at all, which is what makes a word saved before sub-types existed edit as free text —
though it still *displays* as no part of speech, since it never claimed one.

What the form asks for beyond that is decided by `WordGrammar` per **language and word type** — a
German noun offers *der/die/das* and a plural, a
German verb offers past / participle / *haben*-or-*sein*, a Japanese noun offers a counter and no
gender at all, an adverb offers nothing beyond the word itself. A word can hold several
alternatives per language, and **the two sides are independent**: *kaufen* and *erwerben* can both
mean *buy*, so each language has its own add button and its own list. Surfaces and meta are
index-aligned *within* a side, never across the pair, which is what lets the counts differ. Each side
always keeps at least one entry.

What is stored is not what is typed. `composeWordText` folds the article back in — `Apfel` +
masculine becomes `["der Apfel"]`, and French elides to `["l'eau"]` — while `composeWordMeta`
writes the grammar as `{"lang":"de","type":"noun","genders":["m"],"fields":{"plural":["Äpfel"]}}`.
`parseWordFormInputs` is the exact inverse, so opening a word for editing shows *Apfel* with its
gender chip lit rather than the stored surface. Editing preserves the word's id, its level and its
creation time: correcting a typo must not cost the user the progress they built on that word.

Two things from the Android form are deliberately not ported yet: the per-language hint texts under
each field, and the Japanese verb/adjective class pickers.

### Two designs, one app

The browser gets a desktop app — a persistent sidebar, pages that title themselves, content laid out
across a wide window — while iOS keeps the Android design it was built from. The two are different by
intent, and the code in `webMain` and `iosMain` is the design: there is no separate spec to consult.
The split is always the same shape:
the view model, the state and the callbacks are shared in `commonMain`, and only a `@Composable
expect fun …Content(…)` forks. Both actuals render the same state and call the same lambdas, so no
behaviour can drift between the two.

| | iOS (`iosMain`) | Web (`webMain`) |
|---|---|---|
| Chrome | shared top bar over tabs, a rail on iPad | slim top bar, plus a sidebar or a bottom bar |
| Headers | the shell draws what `RegisterTopBar` registered | the bar holds the way back; the page holds the serif title |
| Measure | one phone-shaped `ContentColumn`, centred | a per-screen `ContentPane`, centred and top-anchored |
| List | one column of cards, search behind a magnifier | card grid, filters always in view |
| Detail | tiles over a lazy word list | tiles over a bordered "WORD LIST" panel |
| Word form | the two language cards stacked | the two side by side, crimson and gold edges |
| Test | question card over two buttons | the same, plus answers that mark themselves after checking |

The web pages are built from one set of parts in `core:designsystem/webMain` — `WebPageTitle`,
`WebBackLink`, `WebChip`, `WebPrimaryButton`, `WebPanel`, `WebSelect`, `WebTextField`. A page that
needs furniture the others do not have belongs there too, not in the feature.

The web navigation follows the **window**, not the platform: at 700dp and wider it is the 240dp
sidebar, and below that — a phone, or a browser dragged narrow — the same destinations become a
bottom bar of glyphs over small labels, since a sidebar there would leave nothing to read. Pages take
the smaller gutter below 600dp for the same reason. Which one shows is decided from the destination,
not from what a screen registered: only the tour goes without.

Both platforms declare their chrome the same way, with `RegisterTopBar`. On iOS that draws the top
bar; on web it fills `WebTopBar` — a slim strip above the page carrying the way back, outside the
page's scroll so a screen can always be left without scrolling up to find a link. Screens name what
back leads to through `backLabel` ("Back to dictionaries", "Exit test"), which iOS ignores in favour
of a bare chevron. The arrow is hidden only where there is genuinely nothing behind — the first
screen after signing in.

Onboarding is the one destination that goes without navigation entirely — it owns the window and
carries its own way out.

`Forum` and `Options` have no page design of their own yet, so the web shell draws their heading from
what they registered — see the branch in `VerborumAppScaffold.web.kt`. Give either a real page and
that branch goes.

### Language

`core:localization` holds every word the app says, as one `Strings` interface whose **property
defaults are the English text**. A language overrides only what it has translated, so a partial
translation falls back to English string by string — nothing is ever blank, and adding a key needs no
change to the other eighteen files. `UiLanguage` lists the languages the *interface* speaks, kept
separate from `SupportedLanguage`, which is what a dictionary can be *about*: same codes today,
different questions, and opposite sides of the module graph.

All nineteen are translated; a test asserts every language in the picker has a catalogue and that
none of them is quietly returning English. Composables read `strings.back` through a composition local, shaped like `MaterialTheme.colorScheme`.
View models cannot read a local, so they take `LanguageSettings` and resolve through a property
getter — the language can change while a screen is open.

The default is **the device's own language**, from `navigator.language` on web and
`NSLocale.preferredLanguages` on iOS, with an explicit choice in Options overriding it and persisting.
"System language" heads the picker and hands the choice back — it keeps *following* the device rather
than freezing today's answer, so changing the phone's language changes the app's.
Geolocating the IP was considered and rejected: an address says *where* someone is, not what they
read — wrong for travellers, wrong behind a VPN — and it would mean sending the user's address to a
third party on first launch, which the CSP forbids and the platform makes unnecessary.

Each language is listed in the picker under its **own** name — "Deutsch", not "German" — which is
what the person looking for it will recognise, and which saves translating nineteen language names
nineteen times.

### Fonts: the canvas has none

Compose renders the web app to a **canvas**, and a canvas has no system fonts behind it. Anything the
bundled typeface does not cover is drawn as an empty box — which is why Arabic, the kana and even the
"↵" on the keyboard were missing. Every script the app supports has to be shipped.

`core:designsystem/composeResources/font` carries Noto Sans (Latin, Greek, Cyrillic) plus one face
each for Arabic, Japanese, Korean and Chinese. They are **separate families picked per language** by
`fontFamilyForLanguage`, not one family with a fallback list: Compose resolves a family by weight and
style, not by which face happens to contain a glyph, so a list is not a reliable fallback chain.
Choosing by language is deterministic — and it is what keeps the app light, because Compose fetches a
resource only when something composes it. The three CJK faces are 17MB of the 18MB and never load for
someone studying only European languages; the initial download is the 1.2MB Noto Sans.

Anywhere a string's language is known — the keyboards, the detail screen's word list, the practice
cards — that language's family is applied. **Text fields go by content instead**, via
`fontFamilyForText`: a field's language does not always describe what is in it, and a face for one
script carries nothing of another — the Arabic one has no Latin whatsoever, so a dictionary named
"German Basics" typed with the Arabic keyboard open would be a row of empty boxes. Keys that are actions rather than
characters (shift, backspace, enter) are vector icons for the same reason: a symbol the face lacks
would be another empty box.

iOS is untouched: it draws with the system fonts, which already cover every script here.

### The on-screen keyboards (web only)

Every word card carries a key in its corner. Pressing it opens a keyboard for **that card's
language**, anchored to the card: beside it when the window has room, underneath it when not, always
clamped inside the window. Opening it on a card whose fields are all idle focuses the card's first
field first — a keyboard with nowhere to type would be a dead panel.

The panel itself is deliberately **dark in both themes**, from `KeyboardColors` in
`core:designsystem`. It floats over the page as chrome rather than content, the way an operating
system's keyboard does, and one that changed colour with the page would read as part of the form
underneath it. Rows are centred on each other — seven letters against ten — so the keyboard reads as
one block; enter carries the accent, and shift its own gold, outlined while off and filled while on.

`KeyboardController` owns the whole thing: the registered fields in an explicit `order`, which one
has focus, and whether the keyboard is showing. The keyboard follows the focus, never the reverse,
which is what makes the two rules fall out of one place — Enter moves to the next field and an open
keyboard travels with it, into the other language if that is where the next field is, while a closed
keyboard stays closed because nothing here opens it on its own. Enter means the same from either
keyboard: the on-screen key and `onPreviewKeyEvent` call the same method.

Fields work in `TextFieldValue` rather than a plain string so keys insert at the cursor and replace a
selection, instead of appending wherever the caret happens not to be. The popup is deliberately
**not** focusable — taking focus would pull it out of the very field it types into.

`docs/word-input-keyboard-webapp.md` is the spec: **the keyboard is the restriction**, so its keys
are the app's per-language typeable-character contract. That contract is *mirrored* by the Android
client's field filter rather than shared with it, so the two drift if either is changed alone. Every
layout carries the same two auxiliary keys — apostrophe (`aujourd'hui`) and hyphen — and deliberately
carries no meaning separator: those are drawn *between* meanings by the display layer, and each
meaning is its own entry in an array, so a key for one would let it be typed *into* a surface. The
typographic `’` is normalised to `'` on input, so a word has one spelling however it was typed. Free
text is the exception the spec calls for — arbitrary content, handed to the browser's own keyboard
and restricted by nothing.

The keyboard is field-aware where a language needs it to be: a Chinese word is bopomofo, its
`reading` is pinyin with the tone-marked vowels.

An **extended** keyboard adds one row: digits unshifted, punctuation shifted, paired the way a
physical keyboard pairs them — `1!`, `2@`, `3#`. Symbols past the tenth have no digit left to sit
under and pair off with each other (`-_`, `+=`). Shift is a mode rather than a one-shot, switching
every face at once — letters to capitals, digits to punctuation — since dropping it after one key
would put the punctuation out of reach. It appears wherever it changes something, which includes the
scripts that have no capitals at all: Arabic, Persian and bopomofo leave their letters alone under
shift, but it is still how they reach their punctuation. The forms follow the script: Arabic and Persian get their own
numerals, Japanese and Chinese their own full-width marks. No separator appears
even there — `،` and `؛` are ordinary Arabic punctuation, but they are also what the display layer
puts *between* meanings, so a key for one would let it be typed into a value and read back as two.

A field that could reasonably be written in either half of a pair gets **both** keyboards, switched
from the header — the dictionary name, which might be "Deutsch für Anfänger" or "German Basics".
Given one language the header is only a title. The keys, the writing direction, the script the field
draws in and the Korean composer all follow whichever language is picked. Both extras belong on a field that is *not* restricted to its
keyboard's characters, and for the same reason: the filter follows the field's own base layout, so a
second language — or a digit — would type something the field then discards. The dictionary name has
both; the word cards have neither, one language and letters only.

All 19 languages have a layout, in their own national arrangement (QWERTZ for German, AZERTY for
French, ЙЦУКЕН for Russian, right-to-left rows for Arabic and Persian). Two need saying: Korean types
jamo and `HangulComposer` folds them into syllables as they go — `ㅎ ㅏ ㄴ` becomes `한`, and a
following vowel carries the tail into the next syllable — while Chinese types bopomofo and says on
the keyboard that hanzi need the system input method, because they cannot be produced without a
conversion dictionary. Latin keyboards carry the letters their language actually writes: Italian has
no j, k, w,
x or y, Turkish and Lithuanian no q, w or x.

This is web-only; the iOS cards are untouched.

### Self practice — the screen that forked first

This one was forked before the web redesign and is unaffected by it: both platforms keep the designs
they already had. `selfpractice` shares everything about a *session* — deck order, direction, which
cards are open, how
an answer moves the level — in `SelfPracticeViewModel`. Only the presentation forks, through an
`expect`/`actual` `SelfPracticeContent`:

| | iOS (`iosMain`) | Web (`webMain`) |
|---|---|---|
| Layout | one column, full width | grid, `Adaptive(220.dp)`, **no width cap** |
| Card | expandable — tap reveals the answer | flip — click turns the card over |
| Grading | swipe right/left | Wrong / Correct buttons on the back |
| Forms | side by side: `go · went · gone` | stacked and centred: `go` / `went` / `gone` |
| Shape | full-width row | square, or spanning two grid cells when a form is too long for one |

iOS deliberately reproduces the Android screen, and the fork stops at `SelfPracticeContent`: both actuals
render the same state and call the same callbacks.

### Onboarding

The four-panel welcome tour, ported from Android. The panels and their copy are shared; **when** it
appears and **how it is laid out** both differ per platform, through `expect`/`actual`
(`OnboardingGate`, `isOnboardingOpenedFromOptions`, `OnboardingContent`):

| | iOS | Web |
|---|---|---|
| When | a wall between signing in and the app, first launch only | only from Options → "How to use the app" |
| Layout | one panel at a time, swiped, dots indicator | all four at once, 2×2 grid filling the screen |
| Done button | on the last panel | pinned at the bottom, always visible |
| Practice panel | "Practice with a swipe" | "Practice with a flip" — the web app has flip cards, not swipes |
| On finish | the app | back to Options |

Finishing records the fact in `LocalCache`, so iOS shows it exactly once. The web grid drops to a
single scrolling column below 700dp, where two panels side by side would leave neither readable.

`feature:options` knows nothing about it: the shell passes `optionsGraph` a nullable lambda, and a
null leaves that row out entirely.

### Convention plugins

Module build files stay near-empty because `build-logic/convention` owns the shared setup:

| Plugin id                  | What it does                                                     |
|----------------------------|------------------------------------------------------------------|
| `verborum.kmp.library`     | targets (iOS arm64/sim, js, wasmJs), coroutines, `kotlin-test`     |
| `verborum.kmp.compose`     | the above + Compose Multiplatform, Material 3, lifecycle           |
| `verborum.kmp.feature`     | the above + `core:common`, `core:designsystem`, Koin, Navigation   |
| `verborum.kmp.serialization` | kotlinx.serialization plugin + JSON runtime                      |

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and
options:

- Web app:
    - Wasm target (faster, modern browsers): `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
    - JS target (slower, supports older browsers): `./gradlew :composeApp:jsBrowserDevelopmentRun`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Web tests (Node): `./gradlew jsNodeTest wasmJsNodeTest`
- Web tests (browser): `./gradlew jsBrowserTest wasmJsBrowserTest`
- iOS tests: `./gradlew iosSimulatorArm64Test` (requires Xcode command line tools)

### License

MIT — see [LICENSE](./LICENSE). Copyright (c) 2026 Yasar Naci Gündüz and Seymur Mammadrza.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
