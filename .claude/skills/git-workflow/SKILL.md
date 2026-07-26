---
name: git-workflow
description: Git workflow for the Verborum KMP project — staging and commit conventions, what must be committed and what must never be, and the cross-platform verification required before any commit. Use when staging, committing, preparing a PR, or reasoning about git state in this repo.
---

# Git workflow (Verborum KMP)

Work lands on **`main`** (solo/small-team project). Branch first only when preparing a PR or when
asked.

## Staging is manual here

There is **no auto-staging hook** in this repo — `.claude/` contains `skills/` and `agents/` only.
(The Android project auto-stages Write-created files via a `PostToolUse` hook in
`.claude/settings.json`; do not assume that behaviour here.)

So **nothing is staged until you stage it.** New files created with Write show as `??`, edits made
with Edit show as ` M`, and both need an explicit `git add`.

```bash
git status --short              # ?? = untracked, ' M' = modified, 'A '/'M ' = staged
git add -- <paths>              # stage deliberately, by path
```

Stage everything you created or edited when the user asks for a commit. When they have **not** asked
for one, leaving the working tree dirty and unstaged is the correct end state — see below.

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

3. **Check for accidents**: a scratchpad file written inside the repo, a `build/` artifact
   force-added, a leftover `println`, `enableLogging = true`, a populated `TEAM_ID`, a
   localhost/staging base URL.

## Commit messages

Match the existing history — lowercase, imperative, concise, one line:

```
initial commit
update theme
```

One logical change per commit: a feature, a fix, a build change, or a config change — not a mix. If
the diff needs "and" to describe it, it is two commits. Add a body only when the *why* is not
obvious from the diff. End the message with:

```
Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
```

Use the model actually running the session. This repo's two existing commits predate the convention
and carry no trailer.

## Must be committed

- `.claude/skills/`, `.claude/agents/`, `CLAUDE.md` — shared team config.
- `gradle/libs.versions.toml`, `settings.gradle.kts`, every `build.gradle.kts`, and all of
  `build-logic/`.
- `gradle/wrapper/gradle-wrapper.properties` **including `distributionSha256Sum`**, plus
  `gradlew`/`gradlew.bat`.
- **`kotlin-js-store/yarn.lock`** and `kotlin-js-store/wasm/` — these are lockfiles, not build
  output. Regenerate with `./gradlew kotlinUpgradeYarnLock` and commit the result; never delete them
  to work around a mismatch.
- `iosApp/iosApp.xcodeproj/project.pbxproj` and shared schemes — `.gitignore` deliberately
  un-ignores these while excluding `xcuserdata` and per-user settings.

## Never commit

- Build output: `**/build/`, `.gradle/`, `.kotlin/`, `node_modules/`, and the web
  `dist/…/productionExecutable/` bundle. All gitignored — do not `git add -f` past it.
- `local.properties`, `.idea/`, `.DS_Store`, `xcuserdata/`.
- **Secrets of any kind**: keystores, API keys, client secrets, tokens. A committed secret is
  compromised even after a later removal — tell the user immediately rather than quietly amending.
- A populated `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`. It is intentionally empty in the
  repo and set locally.
- `.claude/settings.local.json` (personal overrides) if one ever appears. **It is not currently in
  `.gitignore`** — add the entry before creating such a file.
- Scratchpad/temp files. If one landed inside the repo by accident, delete it (and
  `git restore --staged --` it if already staged).

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
