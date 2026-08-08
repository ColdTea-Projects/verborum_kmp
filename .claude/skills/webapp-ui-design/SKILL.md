---
name: webapp-ui-design
description: Designs and implements the Compose Multiplatform web UI (Kotlin/Wasm + Kotlin/JS canvas) — responsive layout and breakpoints, the index.html/styles.css host page, first-paint experience, pointer and keyboard input, browser history, per-language fonts and bundle size. Use when changing composeApp/src/webMain, the web host page, the on-screen keyboard, or any UI that must adapt to a desktop browser window.
---

# Web UI design

The web app is **Compose Multiplatform rendering to a canvas**, not DOM. `main()` calls `initKoin()`
then `ComposeViewport { App() }`; `composeApp.js` is loaded by
`composeApp/src/webMain/resources/index.html`, styled by `styles.css`. The same `App()` composable
serves iOS, but the library's screens deliberately **fork**: the browser gets a desktop app — a
persistent sidebar, pages that title themselves, content across a wide window — while iOS keeps the
Android design. A fork is always an `expect`/`actual` on the screen's content composable with the
view model shared, never a copied screen. **The code in `webMain` is the design**; read the nearest
existing web screen before adding one, and match it.

## What canvas rendering means

- The page is one canvas. Browser text selection, `Ctrl+F`, extensions and DOM-based screen readers
  do **not** see the content. Do not rely on them for anything essential.
- Compose owns focus and input. Tab order comes from `Modifier.focusProperties`/composition order,
  not from HTML.
- Right-click, hover and scroll are Compose events; hover is real here (unlike iOS) — use
  `Modifier.pointerHoverIcon(PointerIcon.Hand)` on clickable rows and hover state on interactive
  surfaces so the app does not feel like a phone app in a browser.
- SEO and deep-link previews are not available from the canvas; anything that must be crawlable
  belongs in the host page.
- **There are no system fonts** — an unbundled glyph renders as an empty box. See the fonts
  reference below before adding text in a new script.

## Quick start — responsive layout

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

## Deeper reference

- The host page, the pre-Compose spinner, bundle size, and the per-language font families that
  `fontFamilyForLanguage(code)` picks:
  [references/host_page_and_fonts.md](references/host_page_and_fonts.md).
- The on-screen word-entry keyboard and its character contract, plus browser history, IME, keyboard
  shortcuts and scrollbars: [references/keyboard_and_browser.md](references/keyboard_and_browser.md).

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
