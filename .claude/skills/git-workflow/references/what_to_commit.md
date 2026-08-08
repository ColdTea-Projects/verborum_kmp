# What must be committed, and what must never be

Answers one decision: **is this path allowed into a Verborum KMP commit?**

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

## Commit messages

Match the existing history — lowercase, imperative, concise, one line, prefixed with the platform
the change lands on:

```
[iOS] add database for ios app
[WebApp] cap the add-word fields at 40 characters
[iOS][WebApp] moving animation for flip cards
```

One logical change per commit: a feature, a fix, a build change, or a config change — not a mix. If
the diff needs "and" to describe it, it is two commits. Add a body only when the *why* is not
obvious from the diff. End the message with:

```
Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
```

Use the model actually running the session. The earliest commits in this repo predate the
convention and carry no trailer.

## Checking for accidents before a commit

A scratchpad file written inside the repo, a `build/` artifact force-added, a leftover `println`,
`enableLogging = true`, a populated `TEAM_ID`, a localhost/staging base URL.
