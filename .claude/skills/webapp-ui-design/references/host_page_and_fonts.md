# The host page, first paint, bundle size and fonts

Answers one decision: **what does the user see before and while Compose starts, and which typeface
draws their language?**

## The host page

`index.html` and `styles.css` in `composeApp/src/webMain/resources/` are part of the UI:

- `html, body { width/height 100%; margin 0; overflow: hidden }` — keep it; the canvas sizes itself
  to the viewport and the page must not scroll behind it.
- The inline SVG spinner is the **pre-Compose** loading state: it is what the user sees while the
  Wasm/JS bundle downloads and initialises. Style it to match the app (`VerborumColors` values are
  fine here, hardcoded, since CSS cannot read the theme) and add a `prefers-color-scheme` rule so it
  is not a white flash in dark mode.
- Set a real `<title>`, `theme-color`, favicon and `lang`. A `<title>` still reading `verborum_kmp`
  is placeholder text.
- Keep `<meta name="viewport" content="width=device-width, initial-scale=1.0">` for mobile browsers.
- Do not add DOM manipulation to make something appear faster; it will fight the canvas.

## Loading and bundle size

Wasm/JS payloads are large; first paint is the weak point of this stack.

- Prefer the **wasmJs** target for modern browsers (`wasmJsBrowserDevelopmentRun` /
  `wasmJsBrowserDistribution`); the `js` target is the compatibility fallback.
- Check the shipped size after UI work: `./gradlew :composeApp:wasmJsBrowserDistribution` and
  inspect `composeApp/build/dist/wasmJs/productionExecutable/`.
- Bundle fonts as Compose resources in `core:designsystem` rather than fetching from a CDN — a
  third-party font request is both a latency and a privacy problem, and the CSP forbids it.

## Fonts — the canvas has none

**There are no system fonts on the canvas.** A glyph the bundled typeface does not carry renders as
an empty box — there is no fallback to fall back to. This bit the app once already: Arabic, kana and
even "↵" were invisible.

`core:designsystem/composeResources/font` carries Noto Sans (Latin, Greek, Cyrillic) plus a face
each for Arabic, Japanese, Korean and Chinese. `verborumTypography()` is `expect`/`actual`: the web
actual builds the scale from Noto Sans, and the iOS actual returns the plain `Typography`, because
iOS draws with system fonts that already cover every script.

They are **separate families picked per language** by `fontFamilyForLanguage(code)`, never one
family with a fallback list. Compose resolves a `FontFamily` by weight and style, *not* by which
face holds a glyph, so a list is not a fallback chain — it would compile, look right, and still
render boxes. Choosing by language is deterministic, and it is what keeps the app light: Compose
fetches a resource only when something composes it, so the three CJK faces (17MB of 18MB) never load
for a user studying European languages.

Apply the family anywhere a string's language is known — word fields, word lists, practice cards,
keyboards. For a symbol the face may not carry (`⇧`, `⌫`, `↵`), use a vector icon instead: an icon
cannot go missing.
