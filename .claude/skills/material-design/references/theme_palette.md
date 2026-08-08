# Theme structure and the palette → scheme mapping

Answers one decision: **which M3 slot does a Verborum palette colour feed, and where do I change
it?**

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
which exists on iOS or web.

`VerborumTheme(darkTheme = isSystemInDarkTheme())` wraps every entry point — `App()` for both
platforms — and every preview. Nothing renders outside it.

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

Changing a colour means editing `Color.kt` (the raw value) or `Theme.kt` (the slot it feeds), never
a call site — and mirroring the edit in the Android project.
