# Component choices and accessibility

Answers one decision: **which M3 component belongs here, and what does it owe an assistive user?**

## Component choices

- `Scaffold` owns the frame; it hands down `padding` — apply it, do not discard it.
- Bottom navigation is `NavigationBar` + `NavigationBarItem`, driven by `TopLevelDestination`.
  Reach for `NavigationRail` / `NavigationSuiteScaffold` when the window is wide (see the
  `webapp-ui-design` skill).
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
