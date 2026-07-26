---
name: ios-app-ui-design
description: Making the Compose Multiplatform iOS app feel native — safe areas and insets, the SwiftUI/ComposeUIViewController seam, back-swipe and scroll behaviour, Dynamic Type, dark mode, keyboard avoidance, and Human Interface Guidelines expectations. Load before changing composeApp/src/iosMain, the iosApp Swift wrapper, or any UI that ships to iPhone/iPad.
---

# iOS UI design

`MainViewController()` wraps `App()` in `ComposeUIViewController`; `ContentView.swift` hosts it via
`UIViewControllerRepresentable`. Deployment target **iOS 18.2**, device family **1,2** (iPhone +
iPad). The same `App()` composable serves web — differences are adaptive layout, not forked screens.

## Safe areas — the first thing to get right

`ContentView.swift` applies `.ignoresSafeArea()`, so **Compose receives the full screen including the
notch/Dynamic Island and the home indicator**. Insets are entirely Compose's job:

- `Scaffold` in `App()` consumes window insets and hands you `padding` — apply it (it already does).
- Content that must reach the edge (a hero image, a coloured header) draws to the edge but keeps its
  *text and controls* inside `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`.
- Never hardcode a status-bar or home-indicator height.
- Verify on a notched device **and** a home-button-less iPad, in portrait and landscape.

## The Swift seam

Keep it thin. `iosApp` should contain no app logic — everything goes through
`MainViewController()`.

- `initKoin()` is invoked exactly once via the `private val koin by lazy { … }` in
  `MainViewController.kt`. Do not call it from Swift and do not call it per-composition.
- Kotlin exposed to Swift must be top-level functions or classes; Kotlin `object`s, sealed
  hierarchies and default arguments translate poorly across the ObjC bridge.
- Xcode gets the framework from the Gradle build phase (`embedAndSignAppleFrameworkForXcode`); the
  framework is static, `baseName = "ComposeApp"`.
- `Info.plist` sets `CADisableMinimumFrameDurationOnPhone` so ProMotion displays can run the canvas
  at 120Hz — keep it.

## Gestures and motion iOS users expect

- **Edge back-swipe** is the strongest iOS convention and Compose Multiplatform does not give it to
  you for free on a `NavHost`. Either enable the platform back gesture for the nav host or provide an
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
- **Touch targets ≥ 44pt** (HIG) — stricter than Material's 48dp only in name; use the larger.
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

Device family includes iPad, so the app runs at tablet width. Reuse the width breakpoints from
`webapp-ui-design` (600dp / 840dp) — a bottom `NavigationBar` stretched across an iPad is wrong;
prefer a rail or list-detail split. Also handle multitasking size changes (Split View) — never cache
a size across recomposition.

## Verify

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # fast compile+link check
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

Then run from Xcode (`open iosApp/iosApp.xcodeproj`) on a simulator and check: notch + home
indicator, landscape, dark mode, largest Dynamic Type, keyboard over the search field, back out of
word detail, iPad width.

## Checklist

- [ ] Safe-area insets handled in Compose (Swift side still `.ignoresSafeArea()`)
- [ ] Back affordance or back-swipe on every pushed screen
- [ ] Scroll bounces; no reimplemented scrolling
- [ ] Layout survives largest Dynamic Type; no clipped text
- [ ] Dark mode verified; touch targets ≥ 44pt; `start`/`end` padding
- [ ] Keyboard does not cover the focused field; IME action wired
- [ ] iPad/landscape layout adapts; Split View size changes handled
- [ ] `initKoin()` still called exactly once; no logic added to `iosApp/`
