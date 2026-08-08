# The on-screen keyboard and browser integration

Answers one decision: **what may a user type, and what browser affordances must the canvas provide
itself?**

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
  users expect them and a bottom-bar-only design ignores them.
- **Scroll**: use `LazyColumn` with keys; add a scrollbar affordance on desktop widths, since the
  canvas has no native one.
