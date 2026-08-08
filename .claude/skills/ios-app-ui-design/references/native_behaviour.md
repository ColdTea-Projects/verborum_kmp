# Native iOS behaviour in a Compose UI

Answers one decision: **what does an iOS user expect this screen to do that Compose will not give
for free?**

## Gestures and motion

- **Edge back-swipe** is the strongest iOS convention and Compose Multiplatform does not give it for
  free on a `NavHost`. Either enable the platform back gesture for the nav host or provide an
  unmistakable back affordance in the top bar on every non-root screen. Never leave a pushed screen
  with no way back but the bottom bar.
- Scroll must **bounce/overscroll**; a hard stop reads as a web page. Use the platform overscroll
  effect rather than disabling it.
- Momentum and fling come from `LazyColumn`/`rememberScrollState` — do not reimplement scrolling.
- Animations: short (150–300ms) and interruptible. Respect Reduce Motion.

## Type, colour and the platform look

- **Dynamic Type**: iOS users change text size system-wide. Sizes come from
  `MaterialTheme.typography` (in `sp`, so they scale), and layouts must survive the largest
  accessibility sizes — no fixed-height rows around text, no `maxLines = 1` on body copy that
  matters. Test with the largest setting.
- **Dark mode**: `isSystemInDarkTheme()` follows the iOS appearance setting; verify both.
- **Touch targets ≥ 44pt** (Human Interface Guidelines) — stricter than Material's 48dp only in
  name; use the larger.
- Material 3 components are fine and intentional here, but respect iOS *behaviour*: destructive
  confirmations, sheets that can be dragged down, no Android-style back button drawn in the UI.
- Right-to-left: use `start`/`end` padding, never `left`/`right`.

## Keyboard

- The software keyboard must not cover the focused field. Ensure the content scrolls
  (`Modifier.imePadding()` plus a scrollable container) — the dictionary search field is the case to
  check first.
- Set `KeyboardOptions` (`imeAction = ImeAction.Search`, correct `keyboardType`) and handle the
  action; a search field whose Return key does nothing is a bug.
- Tap-outside and scroll should dismiss the keyboard.

## iPad

The device family includes iPad, so the app runs at tablet width. Reuse the width breakpoints from
the `webapp-ui-design` skill (600dp / 840dp) — a bottom `NavigationBar` stretched across an iPad is
wrong; prefer a rail or list-detail split. Also handle multitasking size changes (Split View) —
never cache a size across recomposition.

## The Swift seam

Keep it thin. `iosApp` should contain no app logic — everything goes through `MainViewController()`.

- `initKoin()` is invoked exactly once via the `private val koin by lazy { … }` in
  `MainViewController.kt`. Do not call it from Swift and do not call it per-composition.
- Kotlin exposed to Swift must be top-level functions or classes; Kotlin `object`s, sealed
  hierarchies and default arguments translate poorly across the ObjC bridge.
- Xcode gets the framework from the Gradle build phase (`embedAndSignAppleFrameworkForXcode`); the
  framework is static, `baseName = "ComposeApp"`.
- `Info.plist` sets `CADisableMinimumFrameDurationOnPhone` so ProMotion displays can run the canvas
  at 120Hz — keep it.
