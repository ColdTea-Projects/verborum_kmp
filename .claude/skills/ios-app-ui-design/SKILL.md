---
name: ios-app-ui-design
description: Implements a native-feeling Compose Multiplatform iOS app — safe areas and insets, the SwiftUI/ComposeUIViewController seam, back-swipe and scroll behaviour, Dynamic Type, dark mode, keyboard avoidance, and Human Interface Guidelines expectations. Use when changing composeApp/src/iosMain, the iosApp Swift wrapper, an iOS platform actual, or any UI that ships to iPhone or iPad.
---

# iOS UI design

`MainViewController()` wraps `App()` in `ComposeUIViewController`; `ContentView.swift` hosts it via
`UIViewControllerRepresentable`. The device family covers iPhone and iPad. The same `App()`
composable serves web, but the library's screens fork per platform: iOS keeps the Android design
while web is laid out as a desktop app. A fork is an `expect`/`actual` on the screen's content
composable with the view model shared. **Web work must not change the iOS actual** — that is the
whole point of the split, and the iOS actuals are the Android design's only record here.

## Quick start

Compile and link the framework, then run it in a simulator:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # fast compile+link check
open iosApp/iosApp.xcodeproj
```

On the simulator, check: notch + home indicator, landscape, dark mode, largest Dynamic Type,
keyboard over the search field, back out of word detail, iPad width.

## Safe areas — the first thing to get right

`ContentView.swift` applies `.ignoresSafeArea()`, so **Compose receives the full screen including
the notch/Dynamic Island and the home indicator**. Insets are entirely Compose's job:

- `Scaffold` in `App()` consumes window insets and hands down `padding` — apply it (it already does).
- Content that must reach the edge (a hero image, a coloured header) draws to the edge but keeps its
  *text and controls* inside `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`.
- Never hardcode a status-bar or home-indicator height.
- Verify on a notched device **and** a home-button-less iPad, in portrait and landscape.

## Everything iOS users expect that Compose does not give for free

Back-swipe, overscroll, Dynamic Type, dark mode, touch-target size, keyboard avoidance, iPad
layout, and the rules for the thin Swift seam (`initKoin()` exactly once, no logic in `iosApp/`)
are in [references/native_behaviour.md](references/native_behaviour.md). Read it before changing
navigation, a text field, or anything in `iosApp/`.

## Checklist

- [ ] Safe-area insets handled in Compose (Swift side still `.ignoresSafeArea()`)
- [ ] Back affordance or back-swipe on every pushed screen
- [ ] Scroll bounces; no reimplemented scrolling
- [ ] Layout survives largest Dynamic Type; no clipped text
- [ ] Dark mode verified; touch targets ≥ 44pt; `start`/`end` padding
- [ ] Keyboard does not cover the focused field; IME action wired
- [ ] iPad/landscape layout adapts; Split View size changes handled
- [ ] `initKoin()` still called exactly once; no logic added to `iosApp/`
