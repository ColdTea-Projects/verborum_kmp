---
name: material-design
description: Implements Material 3 correctly in this Compose Multiplatform app — the VerborumColors palette, the light/dark ColorScheme mapping, Typography, the Spacing scale, VerborumIcons, shared state composables and the token rules that keep screens consistent. Use when writing any Compose UI, choosing a colour, text style or dimension, adding an icon, editing core:designsystem, or reviewing UI code.
---

# Material 3 in Verborum

Material 3 comes from `org.jetbrains.compose.material3`, pulled in by `verborum.kmp.compose`.
`core:designsystem` is the single owner of theme and shared components, and everything renders
inside `VerborumTheme`.

## Quick start — the three token rules

```kotlin
color = MaterialTheme.colorScheme.secondary          // ✅ colour
style = MaterialTheme.typography.titleMedium         // ✅ type
padding(Spacing.medium)                              // ✅ dimension

color = VerborumColors.LightGold                     // ❌ breaks dark mode
color = Color(0xFFD4AF37)                            // ❌ literal
padding(16.dp)                                       // ❌ literal
```

**Colour** — always `MaterialTheme.colorScheme.*`. Never a `Color(0xFF…)` literal outside
`Color.kt`, and never `VerborumColors.*` directly in a feature: go through the scheme so dark mode
works for free. Pair every surface with its `on*` colour: `background`/`onBackground`,
`surfaceVariant`/`onSurfaceVariant`, `primary`/`onPrimary`. Mixing pairs is the usual cause of
unreadable text in one mode only.

**Typography** — always `MaterialTheme.typography.*` (`titleMedium`, `bodyMedium`, `labelSmall`).
Never a raw `fontSize`/`TextStyle` in a feature; if a style is missing, add it to `Type.kt`.

**Spacing** — always the `Spacing` scale (`extraSmall` 4 · `small` 8 · `medium` 16 · `large` 24 ·
`extraLarge` 32). Corner radius and elevation come from the component defaults unless there is a
stated reason.

## Shared composables

Use the existing state surfaces so every screen behaves identically:

```kotlin
when {
    state.isLoading       -> LoadingState()
    state.error != null   -> ErrorState(message = "…", onRetry = onRetry)
    state.words.isEmpty() -> EmptyState("No entry matches “${state.query}”.")
    else                  -> /* content */
}
```

A feature-local loading spinner or error column is a review finding — extend
`core:designsystem/component/` instead. Promote a composable there once a **second** feature needs
it, not before.

## Deeper reference

- Theme file layout, the palette → M3 slot mapping, and the Android-app sync rule:
  [references/theme_palette.md](references/theme_palette.md).
- Adding a glyph to `VerborumIcons`, and the three ways an icon renders blank:
  [references/icons.md](references/icons.md).
- Which component to reach for, and the accessibility floor every screen owes:
  [references/components_and_accessibility.md](references/components_and_accessibility.md).

## Checklist

- [ ] Content wrapped in `VerborumTheme`
- [ ] Colours from `colorScheme`, correct `on*` pairing, verified in light **and** dark
- [ ] Text styles from `MaterialTheme.typography`
- [ ] Dimensions from `Spacing`
- [ ] Loading/error/empty use the shared state composables
- [ ] `LazyColumn` items keyed
- [ ] `Scaffold` padding applied; touch targets ≥ 48dp; content descriptions present
- [ ] Palette edits mirrored in the Android project
