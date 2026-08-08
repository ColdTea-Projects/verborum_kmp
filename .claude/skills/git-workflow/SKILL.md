---
name: git-workflow
description: Manages git for the Verborum KMP project — staging and commit conventions, what must be committed and what must never be, and the cross-platform verification required before any commit. Use when staging, committing, preparing a PR, or reasoning about git state in this repo.
---

# Git workflow (Verborum KMP)

Work lands on **`main`** (solo/small-team project). Branch first only when preparing a PR or when
asked.

## Track every new file as you create it

**A file you create is `git add`-ed in the same turn you create it.** An untracked file is invisible
to `git diff`, to review, and to anyone else who clones — so a new source file, test, build file or
skill left as `??` is a half-finished change.

```bash
git status --short              # ?? = untracked, ' M' = modified, 'A '/'M ' = staged
git add -- <paths>              # stage deliberately, by path
```

Rules:

- **New files** (created with Write): `git add -- <path>` right after creating them, by path.
  Do the same for a whole new source set directory you introduced.
- **Edits to already-tracked files**: leave unstaged. They are already in the VCS and visible in
  `git diff`; staging them is only needed when the user asks for a commit.
- **Never** blanket-add: no `git add -A`, no `git add .`, no `git add -f` past `.gitignore`. Check
  the "Never commit" list below before adding — a scratchpad file or a build artifact must not be
  tracked, and staging is where that mistake gets caught.
- Staging is not committing. Tracking new files as you go changes nothing about the rule below.

There is no auto-staging hook here — `.claude/` holds `skills/` and `agents/` only, so this is on
you, every time. (The Android project automates the same intent with a `PostToolUse` hook in
`.claude/settings.json`.)

When the user asks for a commit, stage everything you created **or** edited at that point.

## Never commit unless asked

**Do not commit or push unless the user explicitly asks.** Finishing an implementation is not a
request to commit it. The same goes for `git push`, tags, and opening a PR. Staging is harmless and
reversible; committing is not, from the user's point of view.

## Before you commit

1. **Verify the change on both platforms.** This repo ships to iOS and web from the same source, so a
   commit verified on one target is half-verified:

   ```bash
   ./gradlew compileKotlinJs compileKotlinWasmJs compileKotlinIosSimulatorArm64
   ./gradlew jsNodeTest wasmJsNodeTest
   ```

   Never commit a change you could not build. If a target could not be verified in this environment
   (no Xcode, no browser), say so — do not commit silently past it.

2. **Review what is actually going in:**

   ```bash
   git status
   git diff              # unstaged — anything here you meant to include?
   git diff --staged     # this is the commit
   ```

3. **Check for accidents** and confirm every path belongs in the commit — the allow/deny lists,
   the commit-message format and the platform tag live in
   [references/what_to_commit.md](references/what_to_commit.md). Read it before staging anything
   unfamiliar, and before writing the message.

## Quick reference

```bash
git status --short                    # what is staged vs not
git add -- <paths>                    # stage by path, never a blind `git add -A`
git restore --staged -- <file>        # unstage
git diff --staged                     # review the commit
git log --oneline -5                  # match message style
git diff main...HEAD                  # full branch diff, for a PR
```

Interactive flags (`git add -i`, `git rebase -i`) are unavailable in this environment. Use `gh` for
GitHub operations, and only when asked.
