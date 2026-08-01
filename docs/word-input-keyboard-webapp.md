# Verborum — Word Input Keyboard (KMP Webapp)

**Target location:** KMP webapp repo, `docs/word-input-keyboard-webapp.md`
**Related:** Frontend–Backend Integration (backend repo,
`docs/integration/frontend-backend-integration.md` §1, §4, §10), Android Development
(`verborum_android/docs/android-development.md` §4), Android Word Input Filtering
(`verborum_android/docs/word-input-filtering.md`)

---

## 1. Goal

The webapp does not rely on the browser/OS keyboard for word entry. It ships its **own on-screen
Compose keyboard**, so the set of typeable characters is decided directly by which keys the
keyboard renders. This document defines what that keyboard must contain per language, which
auxiliary keys to add, and how `FREE_TEXT` is handled — so it stays consistent with the Android
field filter.

## 2. Model — the keyboard *is* the restriction

Unlike Android (which keeps the system keyboard and filters the field — see the Android doc),
the webapp controls input at the source: a character can be entered only if the keyboard offers
a key for it. There is no separate filter needed for the restricted word types; the key set is
the allowed set.

Uppercase is already handled by the existing **Shift tab**, so each per-language layout is
authored in lowercase and the Shift tab exposes the case variants (needed for German nouns,
proper forms, etc.).

## 3. Per-language letter keys — source of truth

The per-language letter sets are defined in:

```
de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardLayout
```

**That file is the source of truth for which letter keys each language shows.** This document
does not reproduce or restate those sets — it defines what surrounds them (auxiliary keys,
casing, free-text handling) and the contract they must satisfy.

The languages and scripts the layouts must cover (per Android doc §8.1):

| Script | Languages | Notes |
|---|---|---|
| Latin (+ precomposed diacritics) | en, de, fr, es, it, pt, nl, tr, az, lt, pl | ł ß ş ğ ı ė ç ñ ã ä ö ü … as their own keys |
| Cyrillic | uk, ru | |
| Greek | el | |
| Arabic | ar | RTL (§7) |
| Farsi | fa | RTL (§7) |
| Japanese | ja | kanji + kana |
| Chinese (Simplified) | zh | hanzi + a pinyin path for `reading` (§6) |
| Korean | ko | hangul |

> **Reality check for the custom-keyboard scope.** A full on-screen keyboard for CJK and RTL
> scripts is a large surface. Decide per language whether `KeyboardLayout` genuinely renders that
> script, or whether the webapp falls back to the OS keyboard for it and simply does not
> restrict. Either is defensible; it just needs to be a deliberate call, and it must match what
> the Android filter allows for the same language (§8). This is not settled by the knowledge base.

## 4. Auxiliary keys to add

Beyond the per-language letters and the existing Shift tab, add the following auxiliary keys.
These are my recommendations (the character-set decision was delegated); each is justified below,
and the hyphen is explicitly a judgment call.

| Key | Char | Why |
|---|---|---|
| Space | ` ` (U+0020) | Multi-word surfaces; also required by the Arabic `root` meta field, which stores spaces between letters (`ك ت ب`, Android doc §8.1) |
| Apostrophe | `'` (U+0027) | Within-word apostrophes that are **typed**, e.g. French `aujourd'hui`. (Article elision like `l'eau` is normally composed from the gender chip, not typed — but a typed apostrophe key costs little and covers real vocabulary) |
| Curly apostrophe | `’` (U+2019) | Optional. Some sources/keyboards use the typographic form; offer it only if you want to accept it — otherwise normalise `’ → '` on input to avoid two encodings of the same word |
| Hyphen | `-` (U+002D) | **Judgment call — not shown in any knowledge-base example.** Hyphenated forms are common across these languages; the key is cheap to add and easy to remove. Include unless you have a reason not to |

### Do NOT add meaning-separators as keys

The separators `/` `،` `・` `、` `·` `；` etc. are **display-layer** separators inserted *between*
meanings or forms (Android doc §8.3). Storage keeps meanings as a JSON array, so **a single
surface value never contains them** — they are produced by the "add another meaning" UI, not
typed. Putting them on the keyboard would let a user inject a separator into one surface string,
which is exactly what the array model avoids.

## 5. FREE_TEXT handling

`FREE_TEXT` is the "type absent" case (Android doc §4.2) and is arbitrary content. On Android it
gets **no filter**. The webapp needs an explicit equivalent, and there are two workable options —
pick one:

1. **Fall back to the OS keyboard for `FREE_TEXT`.** Simplest and most permissive: free text is
   not what the custom keyboard exists for, so hand it to the browser keyboard and impose no
   restriction. Recommended.
2. **Custom keyboard with the full auxiliary set enabled.** If you want a consistent look, keep
   the custom keyboard but expose all auxiliary keys (space, apostrophe, hyphen, and common
   punctuation) so free text is not artificially constrained.

Whichever you choose, `FREE_TEXT` must not be held to the letters-only, per-language rule that
governs the typed word types.

## 6. Per-field extras

The keyboard should be field-aware, because two typed meta fields need characters beyond the
plain letter set:

- **`reading` for Chinese (pinyin)** needs **tone-marked vowels**:
  `ā á ǎ à · ē é ě è · ī í ǐ ì · ō ó ǒ ò · ū ú ǔ ù · ǖ ǘ ǚ ǜ · ü`.
  Provide a pinyin layer (or a tone modifier) for the `reading` field on zh, rather than
  expecting the user to find toned vowels elsewhere.
- **`root` for Arabic** needs the **space** key (already added in §4) because the root is stored
  with spaces between letters (`ك ت ب`).

Chip fields — `aux` (`haben`/`sein`) and `class` (`group1`/`na`) — are selected, never typed
(Android doc §4.3), so the keyboard is not involved for them.

## 7. RTL (Arabic, Farsi)

For `ar` and `fa`, the keyboard and the field compose **right-to-left** end to end (Android doc
§8.2/§8.3 make RTL a first-class requirement). Run a manual RTL pass over the keyboard, the input
field, and any preview of the composed surface before calling ar/fa done.

## 8. This is a cross-client contract

`KeyboardLayout` is the **webapp's implementation of the per-language typeable-character
contract**. The Android field filter (Android doc) is the **other** implementation of the same
rule. Because Android shares no code with the KMP clients — nothing is shared except the
contracts (Integration §1) — the two are a **mirrored contract, not shared code**, and will drift
if authored independently: a character on this keyboard that the Android filter rejects (or vice
versa) yields surfaces one client accepts and the other cannot.

Keep them in step:

- The set of **letter keys** here (per language) must equal the Android allowed-letter set for
  that language.
- The **auxiliary decisions** (§4) — space, apostrophe, hyphen, and per-language elision handling
  — must match the Android filter's choices for the same languages.

> **Docs update needed.** The Integration doc (§4, §10) specifies grammar fields and the meta JSON
> shape, but **not** the set of typeable characters per language. Add a "typeable characters per
> language" section to `docs/integration/frontend-backend-integration.md` so Android, this webapp,
> and future iOS implement one list — otherwise the rule gets invented three times, differently
> (the exact failure Integration §1 exists to prevent). `KeyboardLayout` can be the reference
> encoding that section points to.

## 9. Implementation checklist

- [ ] Per-language layouts in `KeyboardLayout` cover every script in §3 (or an explicit
      OS-keyboard fallback is chosen and documented for the ones they don't).
- [ ] Shift tab exposes case variants for each layout.
- [ ] Auxiliary keys added: space, apostrophe (and optional curly-apostrophe normalisation),
      hyphen (§4).
- [ ] Meaning-separators are **not** keys (§4).
- [ ] `FREE_TEXT` uses the chosen no-restriction path (§5).
- [ ] `reading` (zh) has a pinyin/tone path; `root` (ar) has space (§6).
- [ ] RTL pass done for ar/fa (§7).
- [ ] Letter keys and auxiliary decisions verified equal to the Android filter per language (§8),
      ideally via a shared contract section in the Integration doc.
