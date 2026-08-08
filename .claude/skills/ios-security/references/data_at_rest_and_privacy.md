# Data at rest, binary secrets and privacy obligations on iOS

Answers one decision: **where may this piece of user data live on the device, and what does shipping
it oblige the app to declare?**

## Local storage

**`core:database`'s `BibliothecaDatabase` is a real SQLite file on iOS.** It sits in Application
Support under `de.coldtea.verborum`, created with
`NSFileProtectionCompleteUntilFirstUserAuthentication` (the Keychain's `AfterFirstUnlock` posture)
and excluded from backup with `NSURLIsExcludedFromBackupKey` — the rows mirror what the server
holds, so a restore loses nothing. It stores dictionaries and words only; never put a token or any
authorization material in a table.

**`core:database`'s `LocalCache` is real on iOS** — audit what it caches. Anything user-identifying
or authorization-bearing needs the same protection as a token, or must not be cached at all.

## Data at rest and on screen

- Nothing sensitive in `UserDefaults`, plist files, app-group containers or log files.
- Exclude any cache holding user data from backup (`NSURLIsExcludedFromBackupKey`).
- The app snapshot taken on backgrounding is stored on disk — blur or mask token/credential screens
  before `sceneWillResignActive` if such a screen ever exists.
- Do not put secrets in the pasteboard; if it is unavoidable, set an expiry and `isSensitive`.
- `Config.xcconfig` (`TEAM_ID`, bundle id, version) is committed — keep credentials, provisioning
  secrets and API keys out of it and out of the repo.

## Secrets in the binary

The framework is static and linked into the app; every Kotlin string constant is recoverable with
`strings`. No API keys, client secrets, internal hostnames or admin endpoints in `commonMain` or
`iosMain`. Public OAuth client IDs are acceptable.

## Privacy obligations

- Declare purpose strings in `Info.plist` for any permission the app requests; the current plist
  requests none — adding one requires a real user-facing justification string.
- App Store submission needs a privacy manifest (`PrivacyInfo.xcprivacy`) declaring collected data
  types and any required-reason APIs. Adding an SDK or an analytics library changes this.
- Minimise: do not collect or persist what the feature does not need.
