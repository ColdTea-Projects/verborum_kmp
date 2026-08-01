---
name: material-design
description: Material 3 usage in this Compose Multiplatform app — the VerborumColors palette, the light/dark ColorScheme mapping, Typography, the Spacing scale, shared state composables, and the token rules that keep screens consistent. Load before writing any Compose UI, choosing a colour or dimension, editing core:designsystem, or reviewing UI code.
---

# Material 3 in Verborum

Material 3 comes from `org.jetbrains.compose.material3` (**1.11.0-alpha07**), pulled in by
`verborum.kmp.compose`. `core:designsystem` is the single owner of theme and shared components.

## Theme structure

```
core/designsystem/src/commonMain/kotlin/de/coldtea/verborum/core/designsystem/theme/
├── Color.kt     VerborumColors — the raw palette (Light*/Dark*)
├── Theme.kt     LightColorScheme / DarkColorScheme + VerborumTheme
├── Type.kt      Typography
└── Spacing.kt   Spacing
```

The palette and the scheme mapping are shared with the Android app
(`verborum_android/core/.../theme/`) — **keep the two in sync**. Two Android-only pieces are
deliberately absent here: dynamic colour (Material You) and the status-bar `SideEffect`, neither of
which exists on iOS/web.

`VerborumTheme(darkTheme = isSystemInDarkTheme())` wraps every entry point — `App()` for both
platforms — and every preview. Nothing renders outside it.

### Palette → scheme mapping

| Palette role | M3 slot(s) |
|---|---|
| `*Accent` (crimson) | `primary`, `surfaceTint`, `inversePrimary` (opposite mode) |
| `*Gold` | `secondary` |
| `*Background` | `background` |
| `*Surface` | `surface`, `primaryContainer`, `secondaryContainer`, `tertiaryContainer` |
| `*SurfaceAlt` | `surfaceVariant` |
| `*Text` | `onBackground`, `onSurface` |
| `*TextSecondary` | `onSurfaceVariant` |
| `*TextTertiary` | `tertiary` |
| `*Border` | `outline`, `outlineVariant` |

Both schemes are fully populated (errors, containers, inverses, scrim) so no M3 component ever falls
back to a default that clashes.

## Token rules

**Colour** — always `MaterialTheme.colorScheme.*`. Never a `Color(0xFF…)` literal outside
`Color.kt`, and never `VerborumColors.*` directly in a feature: go through the scheme so dark mode
works for free.

```kotlin
color = MaterialTheme.colorScheme.secondary          // ✅
color = VerborumColors.LightGold                     // ❌ breaks dark mode
color = Color(0xFFD4AF37)                            // ❌
```

Pair every surface with its `on*` colour: `background`/`onBackground`,
`surfaceVariant`/`onSurfaceVariant`, `primary`/`onPrimary`. Mixing pairs is the usual cause of
unreadable text in one mode only.

**Typography** — always `MaterialTheme.typography.*` (`titleMedium`, `bodyMedium`, `labelSmall`).
Never a raw `fontSize`/`TextStyle` in a feature; if a style is missing, add it to `Type.kt`.

**Spacing** — always the `Spacing` scale (`extraSmall` 4 · `small` 8 · `medium` 16 · `large` 24 ·
`extraLarge` 32). No literal `.dp` padding in a feature. Corner radius and elevation come from the
component defaults unless there is a stated reason.

**Icons** — `VerborumIcons` in `core:designsystem`. Add there rather than pulling
`material-icons-extended` into a feature: the set is small, and that dependency ships every Material
glyph into a Wasm bundle.

Icons are `ImageVector` declared in Kotlin, not files. That is the Compose Multiplatform norm for
tintable glyphs — nothing to load, nothing to package per target, and **zero HTTP fetches on web**,
where every `composeResources` file is a separate request. `composeResources` is for the other
category: illustrations, logos, fonts, photos.

To add one: run the SVG through a converter (`com.android.tools:sdk-common`'s `Svg2Vector`, which is
the code behind Android Studio's Vector Asset importer, or an SVG-to-Compose site), then paste the
result into `materialIcon { … }`. The converter is what flattens `<circle>`/`<rect>` into path data
and bakes in `transform`; hand-pasting a `d` attribute only works for SVGs that are already a single
`<path>`.

Three rules, each learned the hard way:

- **Every `path(…)` must state a `fill` or a `stroke`.** Compose's `path` defaults `fill = null`,
  which paints nothing — an invisible icon, with no warning and no error.
- **Never add a local helper named `path` to that file.** An explicit import outranks a same-file
  top-level declaration in Kotlin, so `import androidx.compose.ui.graphics.vector.path` silently
  captures *every* `path { … }` call in the file. That once blanked eighteen icons at once.
- Set `materialIcon(viewport = …)` to the source SVG's `viewBox` when it is not 24.

The fill colour itself is arbitrary: `Icon` tints with a colour filter and replaces it. Black is used
throughout only for consistency. An icon that must keep its own colours is not an icon — use
`Image(painterResource(…))` with the asset in `composeResources`.

## Shared composables

Use the existing state surfaces so every screen behaves identically:

```kotlin
when {
    state.isLoading    -> LoadingState()
    state.error != null -> ErrorState(message = "…", onRetry = onRetry)
    state.words.isEmpty() -> EmptyState("No entry matches “${state.query}”.")
    else               -> /* content */
}
```

A feature-local loading spinner or error column is a review finding — extend
`core:designsystem/component/` instead. Promote a composable there once a **second** feature needs
it, not before.

## Component choices

- `Scaffold` owns the frame; it hands you `padding` — apply it, do not discard it.
- Bottom navigation is `NavigationBar` + `NavigationBarItem`, driven by `TopLevelDestination`.
  Reach for `NavigationRail` / `NavigationSuiteScaffold` when the window is wide (see
  `webapp-ui-design`).
- Lists: `LazyColumn` with a stable `key` (`items(state.words, key = Word::id)`) — without it,
  scroll position and animations break on updates.
- Text input: `OutlinedTextField` with a `label`, `singleLine` where appropriate.
- Dividers: `HorizontalDivider` (not the removed `Divider`).
- Buttons by emphasis: `Button` (primary action) → `FilledTonalButton` → `OutlinedButton` →
  `TextButton`. One primary action per screen region.

## Accessibility

- Every actionable element gets a real `contentDescription`, or an explicit `null` when a sibling
  `Text` already labels it (as in `NavigationBarItem`).
- Minimum touch target 48dp (iOS wants 44pt) — `Modifier.clickable` on a bare `Text` is usually too
  small; pad it out or use `Modifier.minimumInteractiveComponentSize()`.
- Body text ≥ 4.5:1 contrast against its surface, in **both** schemes. Verify when changing the
  palette — the gold on white pairing is the one to watch.
- Never encode meaning in colour alone; add an icon or label.

## Checklist

- [ ] Content wrapped in `VerborumTheme`
- [ ] Colours from `colorScheme`, correct `on*` pairing, verified in light **and** dark
- [ ] Text styles from `MaterialTheme.typography`
- [ ] Dimensions from `Spacing`
- [ ] Loading/error/empty use the shared state composables
- [ ] `LazyColumn` items keyed
- [ ] `Scaffold` padding applied; touch targets ≥ 48dp; content descriptions present
- [ ] Palette edits mirrored in the Android project
