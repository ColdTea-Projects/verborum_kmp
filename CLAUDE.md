# Verborum KMP — working agreement

Kotlin Multiplatform app targeting **iOS** (`iosArm64`, `iosSimulatorArm64`) and **web**
(`js`, `wasmJs`). **There is no Android target** — never add `androidMain` code, an Android
dependency, or a JVM-only API to a shared source set.

Stack: Kotlin 2.4.10 · Compose Multiplatform 1.11.1 · Material 3 · MVVM/MVI on
`BaseViewModel` · Koin 4.2 · Navigation Compose · Ktor 3.5 client · kotlinx.serialization ·
kotlinx.coroutines · Gradle 9.1 with convention plugins in `build-logic/`.

Architecture in one line: `composeApp (shell) → feature/* (screens) → core/* (plumbing)`,
dependencies pointing one way only, features never depending on features.
`README.md` has the full module map; `.claude/skills/kmp-app-architecture/SKILL.md` has the rules.

---

## ⚠️ Required on every response

**End every response with a footer naming the skills you loaded this turn:**

```
---
**Skills used:** kmp-development, kotlin
```

Write `**Skills used:** none` when you loaded none. Count only skills you actually invoked with the
Skill tool, and include skills invoked by subagents you spawned (their reports end with the same
footer — merge them in). This applies to every response, including short answers and questions back
to the user.

---

## Skills — load before acting, not from memory

These live in `.claude/skills/<name>/SKILL.md`. Invoke with the Skill tool. They encode decisions
already made about this codebase, so **read the skill before writing code in its area** rather than
reasoning from general knowledge.

| Skill | Load when |
|---|---|
| `kmp-app-architecture` | Adding/moving a module, adding a feature, deciding where a class belongs, wiring Koin, changing navigation, judging layering |
| `kmp-development` | Writing or editing any Kotlin in `commonMain`/`iosMain`/`webMain`, adding a dependency, adding an `expect`/`actual` |
| `kotlin` | Writing or reviewing any Kotlin — idiom, immutability, error handling, naming, comments |
| `gradle-toolchain` | Any `*.gradle.kts`, `libs.versions.toml`, `settings.gradle.kts`, convention plugin, or build failure |
| `material-design` | Any Compose UI, any colour/type/spacing decision, editing `core:designsystem` |
| `webapp-ui-design` | `composeApp/src/webMain`, the host page, responsive/desktop layout |
| `ios-app-ui-design` | `composeApp/src/iosMain`, the `iosApp` Swift wrapper, insets, Dynamic Type, iPad |
| `webapp-security` | Web tokens, `js(...)` bridges, CSP, CORS, the host page, OAuth on web |
| `ios-security` | iOS credential storage, Keychain, ATS, `Info.plist`, `Config.xcconfig`, OAuth on iOS |
| `kmp-unit-testing` | Writing or changing any test; assessing whether a change is covered |
| `kmp-integration-testing` | Testing across layers — `MockEngine`, auth session, Koin graph, Compose UI tests |
| `git-workflow` | Staging, committing, preparing a PR, or reasoning about git state |
| `write-a-skill` | Creating, expanding, splitting or auditing any skill under `.claude/skills` |

**Skill file shape.** Every skill here follows the `write-a-skill` standard: a description that
opens with an action verb and names its triggers with "Use when …", a `SKILL.md` under 100 lines,
and deeper material in a flat `references/` folder one level down. Adding to or creating a skill
means loading `write-a-skill` first, then proving the result with its validators:

```bash
python3 .claude/skills/write-a-skill/scripts/skill_review_checklist_runner.py .claude/skills/<name>
```

**Default bundles.** Any code change loads `kotlin` + `kmp-development`. Add
`kmp-app-architecture` for anything structural, `material-design` for anything visual, the matching
`*-ui-design` for the target, the matching `*-security` for anything touching auth/tokens/config, a
testing skill whenever behaviour changes, `gradle-toolchain` for anything in the build, and
`git-workflow` before any `git add`/`git commit` or when asked about repo state.

---

## Agents — delegate when asked

These live in `.claude/agents/`. **Spawn them only when the user asks for an agent or names one**;
otherwise do the work inline with your own tools and the skills above. Each agent loads its own
skills and reports which ones it used.

| Agent | Owns | Uses skills |
|---|---|---|
| `webapp-development` | Feature work landing on the web target, end to end | architecture, development, kotlin, material-design, webapp-ui-design, webapp-security, testing, gradle, git-workflow |
| `ios-development` | Feature work landing on iOS, end to end | architecture, development, kotlin, material-design, ios-app-ui-design, ios-security, testing, gradle, git-workflow |
| `kmp-code-review` | Read-only review of a diff/branch before commit or PR | all of them, scoped to the diff; `git-workflow` always |
| `webapp-build` | Compiling, bundling, testing, diagnosing the web build | gradle-toolchain, development, webapp-ui-design, webapp-security, git-workflow |
| `ios-build` | Compiling, linking, testing, diagnosing iOS + Xcode | gradle-toolchain, development, ios-app-ui-design, ios-security, git-workflow |

Typical flow for a substantial change: `*-development` to implement → `*-build` to verify →
`kmp-code-review` before committing.

---

## Standing rules

- **`commonMain` first.** Drop to `webMain`/`iosMain` only with a one-line reason in a comment, and
  to `jsMain`/`wasmJsMain` only for a minimal bridge (`js(...)` bodies are illegal in `webMain`).
- **Verify on both platforms.** A change is not done until it compiles for web *and* iOS:
  ```bash
  ./gradlew compileKotlinJs compileKotlinWasmJs compileKotlinIosSimulatorArm64
  ./gradlew jsNodeTest wasmJsNodeTest
  ```
- **Errors are values.** Repositories return `Outcome<T>`; `VerborumError` is the only error model
  features see. No Ktor type escapes `core:network`. Rethrow `CancellationException`. No `!!`.
- **Tokens are not hardcoded, not logged, not widened.** Storage is the Keychain on iOS and
  `sessionStorage` on web; `TokenStorage` is the only seam that may change. Never move a token to
  `NSUserDefaults` or `localStorage`, and read the matching `*-security` skill before touching auth.
- **Design tokens only.** `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `Spacing`. No
  colour, text style or `.dp` literal in a feature module.
- **One version catalog.** `gradle/libs.versions.toml`. No version literals in build files. Shared
  build setup goes in a convention plugin, not a second module build file.
- **Match the surrounding code** — structure, naming, and its sparse "explain why" comment style.
  `feature/bibliotheca` is the reference feature.
- **Report honestly.** Paste real command output. If a check was skipped or a simulator/browser was
  unavailable, say so; never claim verification you did not perform.
- **`git add` every file you create, in the turn you create it.** There is no auto-staging hook here,
  so a new file stays untracked — and invisible to `git diff` and to review — until you stage it by
  path. Edits to already-tracked files can stay unstaged. Never `git add -A`/`.`/`-f`.
- **No commits, pushes or PRs unless asked.** Tracking new files is not committing them. Verify both
  platforms before any commit, keep `kotlin-js-store/` lockfiles committed, and keep secrets and a
  populated `TEAM_ID` out. See `git-workflow`.
