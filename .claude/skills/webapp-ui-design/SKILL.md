---
name: webapp-ui-design
description: Designing and implementing the Compose Multiplatform web UI (Kotlin/Wasm + Kotlin/JS canvas) — responsive layout, the index.html/styles.css host page, loading experience, pointer and keyboard input, browser history, fonts and bundle size. Load before changing composeApp/src/webMain, the web host page, or any UI that must adapt to a desktop browser window.
---

# Web UI design

The web app is **Compose Multiplatform rendering to a canvas**, not DOM. `main()` calls
`initKoin()` then `ComposeViewport { App() }`; `composeApp.js` is loaded by
`composeApp/src/webMain/resources/index.html`, styled by `styles.css`. The same `App()` composable
serves iOS, but the library's screens deliberately **fork**: the browser gets a desktop app — a
persistent sidebar, pages that title themselves, content across a wide window — while iOS keeps the
Android design. A fork is always an `expect`/`actual` on the screen's content composable with the
view model shared, never a copied screen. **The code in `webMain` is the design**; read the nearest
existing web screen before adding one, and match it. Anything without a web design of its own adapts
by layout instead.

## What canvas rendering means for you

- The page is one canvas. Browser text selection, `Ctrl+F`, extensions and DOM-based screen readers
  do **not** see your content. Do not rely on them for anything essential.
- Compose owns focus and input. Tab order comes from `Modifier.focusProperties`/composition order,
  not from HTML.
- Right-click, hover and scroll are Compose events; hover is real here (unlike iOS) — use
  `Modifier.pointerHoverIcon(PointerIcon.Hand)` on clickable rows and hover state on interactive
  surfaces so the app does not feel like a phone app in a browser.
- SEO and deep-link previews are not available from the canvas; anything that must be crawlable
  belongs in the host page.
- **There are no system fonts.** A glyph the bundled typeface does not carry renders as an empty
  box — there is no fallback to fall back to. This bit the app once already: Arabic, kana and even
  "↵" were invisible. See *Fonts* below.

## Responsive layout

A browser window ranges from a phone-sized viewport to an ultrawide desktop. Drive layout from the
available width, not from the platform:

```kotlin
BoxWithConstraints {
    val isExpanded = maxWidth >= 840.dp
    if (isExpanded) TwoPaneDictionary(...) else SinglePaneDictionary(...)
}
```

Breakpoints (Material window size classes): **compact** < 600dp, **medium** 600–840dp,
**expanded** ≥ 840dp.

- Constrain reading measures — `Modifier.widthIn(max = 720.dp)` centred, so definition text does not
  stretch across a 27" monitor.
- At expanded width prefer a `NavigationRail`/list-detail split over the bottom `NavigationBar`;
  bottom bars are a phone idiom. `TopLevelDestination` stays the source of truth for both.
- Never hardcode pixel positions; use `Row`/`Column`/`weight`/`Arrangement`.
- Test by resizing the browser window, not by trusting one size.

## The host page

`index.html` and `styles.css` are part of the UI:

- `html, body { width/height 100%; margin 0; overflow: hidden }` — keep it; the canvas sizes itself
  to the viewport and the page must not scroll behind it.
- The inline SVG spinner is the **pre-Compose** loading state: it is what the user sees while the
  Wasm/JS bundle downloads and initialises. Style it to match the app (`VerborumColors` values are
  fine here, hardcoded, since CSS cannot read the theme) and add a `prefers-color-scheme` rule so it
  is not a white flash in dark mode.
- Set a real `<title>`, `theme-color`, favicon and `lang`. `<title>` currently reads
  `verborum_kmp` — that is placeholder text.
- Keep `<meta name="viewport" content="width=device-width, initial-scale=1.0">` for mobile browsers.

## Loading and bundle size

Wasm/JS payloads are large; first paint is the weak point of this stack.

- Prefer the **wasmJs** target for modern browsers (`wasmJsBrowserDevelopmentRun` /
  `wasmJsBrowserDistribution`); the `js` target is the compatibility fallback.
- Check the shipped size after UI work: `./gradlew :composeApp:wasmJsBrowserDistribution` and inspect
  `composeApp/build/dist/wasmJs/productionExecutable/`.
- Bundle fonts as Compose resources in `core:designsystem` rather than fetching from a CDN — a
  third-party font request is both a latency and a privacy problem, and the CSP forbids it.

### Fonts — the canvas has none

`core:designsystem/composeResources/font` carries Noto Sans (Latin, Greek, Cyrillic) plus a face
each for Arabic, Japanese, Korean and Chinese. `verborumTypography()` is `expect`/`actual`: the web
actual builds the scale from Noto Sans, and the iOS actual returns the plain `Typography`, because
iOS draws with system fonts that already cover every script.

They are **separate families picked per language** by `fontFamilyForLanguage(code)`, never one family
with a fallback list. Compose resolves a `FontFamily` by weight and style, *not* by which face holds
a glyph, so a list is not a fallback chain — it would compile, look right, and still render boxes.
Choosing by language is deterministic, and it is what keeps the app light: Compose fetches a resource
only when something composes it, so the three CJK faces (17MB of 18MB) never load for a user studying
European languages.

Apply the family anywhere a string's language is known — word fields, word lists, practice cards,
keyboards. For a symbol the face may not carry (`⇧`, `⌫`, `↵`), use a vector icon instead: an icon
cannot go missing.
- Do not add DOM manipulation to make something appear faster; it will fight the canvas.

## Word entry is the app's own keyboard

`feature/bibliotheca/.../common/ui/keyboard` ships an on-screen keyboard per language, and
`docs/word-input-keyboard-webapp.md` is its spec. The rule that governs everything there: **the
keyboard is the restriction** — a character is enterable only if a key types it, and the field
filters anything else out, from the physical keyboard and from a paste alike.

Consequences worth knowing before touching it:

- The key set is a **contract mirrored by the Android client's field filter**, not shared code.
  Adding a key here that Android rejects produces words one client can write and the other cannot.
- Meaning separators (`/`, `،`, `・`, `、`) are never keys. They are drawn *between* meanings, and
  each meaning is its own entry in a JSON array; a key for one would let it be typed *into* a
  surface.
- Where a keyboard is **phonetic rather than complete** — Korean jamo composing into syllables,
  Chinese bopomofo standing in for hanzi, Japanese kana alongside kanji — the accepted set is the
  script's Unicode range, not the key caps. Restricting those to their keys makes the language
  impossible to write.
- Latin layouts carry the letters the language actually writes (Italian has no j, k, w, x, y).

## Browser integration

- **Back/forward**: Navigation Compose does not wire browser history automatically. If the user must
  be able to use the browser back button, that is an explicit piece of work bridging
  `window.history` to the `NavController` — put the bridge in `webMain`, keep it out of features.
- **Text input**: verify IME/composition and paste behaviour in a real browser; canvas text fields
  are the most fragile part of the stack.
- **Keyboard**: support `Esc` to dismiss, `Enter` to submit, and visible focus indicators — desktop
  users expect them and the bottom-bar-only design ignores them.
- **Scroll**: use `LazyColumn` with keys; add a scrollbar affordance on desktop widths, since the
  canvas has no native one.

## Verify

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # then resize, tab through, toggle OS dark mode
./gradlew :composeApp:jsBrowserDevelopmentRun       # fallback target
./gradlew :composeApp:wasmJsBrowserDistribution     # production bundle + size check
```

Check the browser console for Kotlin exceptions — on web they surface there, not in Gradle output.

## Checklist

- [ ] Layout adapts at 600dp/840dp; verified by resizing
- [ ] Reading widths capped; no phone-only bottom bar at expanded width
- [ ] Host page: real title/lang/theme-color, dark-mode-aware pre-load spinner
- [ ] Hover cursors on clickable elements; keyboard Esc/Enter/focus handled
- [ ] Fonts bundled, not fetched; any language-specific text uses `fontFamilyForLanguage`
- [ ] No nested scrollables — a `verticalScroll` inside a `verticalScroll` is measured against an
      unbounded height and throws at runtime, which no compile will catch
- [ ] Runs on both `wasmJs` and `js`; console free of exceptions
- [ ] A fork is `expect`/`actual` on the content composable with the view model shared — never a
      copied screen, and never a change to the iOS actual
