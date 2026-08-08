---
name: ios-security
description: Security review and hardening for the iOS target — credential storage (Keychain vs NSUserDefaults), App Transport Security, OAuth via ASWebAuthenticationSession, on-device data at rest, secrets in the framework binary, logging, and privacy/Info.plist obligations. Load before touching core:auth iosMain, core:database iosMain, the iosApp Xcode project or xcconfig, anything handling tokens on iOS, or when reviewing the iOS build for security.
---

# iOS security

Threat model: an attacker with the device (lost/stolen, or jailbroken), an attacker on the network,
and anyone who can read the app bundle — the IPA is decompilable and every string in it is public.

## Token storage — done, keep it that way

`core/auth/src/iosMain/.../TokenStorage.ios.kt` is `KeychainTokenStorage`:
`SecItemAdd`/`SecItemCopyMatching`/`SecItemDelete` on a `kSecClassGenericPassword` item with a stable
`kSecAttrService`/`kSecAttrAccount` pair, protected by
**`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`** — `…ThisDeviceOnly` keeps it out of backups
and off the user's other devices, `AfterFirstUnlock` still permits a background refresh. It also
migrates and deletes any payload left by the previous `NSUserDefaults` implementation.

Do not regress this: never `kSecAttrAccessibleAlways`, never move a token back into
`NSUserDefaults`, and keep `SecItemDelete` on sign-out. `TokenStorage` is the seam, so any change
stays `iosMain`-only. The access token is held behind `AuthSession`'s `Mutex`; keeping it in memory
only (Keychain for the refresh token alone) is the remaining hardening step.

**Not yet verified on a device or simulator**: the Keychain path and the
`ASWebAuthenticationSession` flow compile, but this machine cannot link or run iOS binaries (no Xcode
command line tools). First run on a simulator should confirm read/write/delete and the
upgrade-from-`NSUserDefaults` path.

**`core:database`'s `BibliothecaDatabase` is a real SQLite file on iOS.** It sits in Application
Support under `de.coldtea.verborum`, created with `NSFileProtectionCompleteUntilFirstUserAuthentication`
(the Keychain's `AfterFirstUnlock` posture) and excluded from backup with `NSURLIsExcludedFromBackupKey`
— the rows mirror what the server holds, so a restore loses nothing. It stores dictionaries and words
only; never put a token or any authorization material in a table.

**`core:database`'s `LocalCache` is real on iOS** — audit what it caches. Anything user-identifying
or authorization-bearing needs the same protection as a token, or must not be cached at all.

## Transport

- HTTPS only. **Never** add `NSAllowsArbitraryLoads` (or a per-domain ATS exception) to
  `iosApp/iosApp/Info.plist`; the current plist has no ATS exceptions and should stay that way.
- The Darwin engine gets a fresh `defaultApiConfig()` per target — the iOS base URL is absolute, so
  confirm it is `https://` and points at production before release. A localhost/staging URL shipping
  in a release build is both a security and a privacy incident.
- Certificate pinning is worth considering for the auth endpoint (Darwin engine
  `handleChallenge`), but only with a documented rotation plan — a pin without one bricks the app.

## OAuth on iOS

- The flow is implemented: `core/auth/.../AuthorizationLauncher.ios.kt` wraps
  **`ASWebAuthenticationSession`**, `AuthService` verifies `state` and exchanges the code, and
  `AuthConfig.ios.kt` holds the endpoints. Never replace it with an embedded `WKWebView` or
  `UIWebView`: an in-app web view can read the user's credentials, breaks federated sign-in, and is
  an App Store review risk.
- The redirect scheme `de.coldtea.verborum` is registered in `iosApp/iosApp/Info.plist`
  (`CFBundleURLTypes`) and must stay in sync with `redirectUri` in `AuthConfig.ios.kt`.
- PKCE `S256` from `core:auth` is already correct and its iOS RNG is properly seeded —
  `secureRandomBytes` uses `SecRandomCopyBytes(kSecRandomDefault, …)`. Keep it; never substitute
  `arc4random`/`Random.Default` for cryptographic material.
- Register the redirect URI's custom scheme / universal link and validate the callback: verify
  `state`, and prefer a **universal link** over a custom scheme (custom schemes can be claimed by
  another app).
- Handle `signOut()` by clearing Keychain, `LocalCache`, the local database, and any in-memory state.
  `OptionsViewModel` calls `BibliothecaDatabase.clear()` after `AuthService.signOut()` — the library
  is one user's content, and the next person to sign in on the device must not find it waiting.

## Data at rest and on screen

- Nothing sensitive in `UserDefaults`, plist files, app-group containers or log files.
- Exclude any cache holding user data from backup (`NSURLIsExcludedFromBackupKey`).
- The app snapshot taken on backgrounding is stored on disk — blur or mask token/credential screens
  before `sceneWillResignActive` if such a screen ever exists.
- Do not put secrets in the pasteboard; if you must, set an expiry and `isSensitive`.
- `Config.xcconfig` (`TEAM_ID`, bundle id, version) is committed — keep credentials, provisioning
  secrets and API keys out of it and out of the repo.

## Secrets in the binary

The framework is static and linked into the app; every Kotlin string constant is recoverable with
`strings`. No API keys, client secrets, internal hostnames or admin endpoints in `commonMain` or
`iosMain`. Public OAuth client IDs are acceptable.

## Logging

`ApiConfig.enableLogging` must be `false` in release builds — Ktor `Logging` at `HEADERS`/`ALL` writes
the `Authorization` header into the device console, which any connected Mac can read. Never
`println` a token, a request body or PII. Prefer no logging of network traffic in release at all.

## Privacy obligations

- Declare purpose strings in `Info.plist` for any permission the app requests; the current plist
  requests none — adding one requires a real user-facing justification string.
- App Store submission needs a privacy manifest (`PrivacyInfo.xcprivacy`) declaring collected data
  types and any required-reason APIs. Adding an SDK or an analytics library changes this.
- Minimise: do not collect or persist what the feature does not need.

## Review checklist

- [ ] Tokens/credentials in Keychain with `…ThisDeviceOnly`, not `NSUserDefaults`
- [ ] Sign-out clears Keychain, `LocalCache` **and** the local database
- [ ] No ATS exception added; base URL is `https://` and production
- [ ] Authorization uses `ASWebAuthenticationSession`, not an embedded web view
- [ ] PKCE `S256`; crypto randomness from `SecRandomCopyBytes`; `state` verified
- [ ] Callback URI validated; universal link preferred over custom scheme
- [ ] Nothing sensitive in UserDefaults/plists/caches/pasteboard; backup exclusion set
- [ ] No secrets in the framework, in `Config.xcconfig`, or in the repo
- [ ] `enableLogging` false in release; no token/PII logging
- [ ] Privacy manifest and purpose strings still accurate
