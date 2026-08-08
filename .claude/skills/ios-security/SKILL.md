---
name: ios-security
description: Reviews and hardens the iOS target's security — Keychain credential storage, App Transport Security, OAuth via ASWebAuthenticationSession, on-device data at rest, secrets in the framework binary, logging, and privacy manifest obligations. Use when touching core:auth iosMain, core:database iosMain, the iosApp Xcode project or xcconfig, anything handling tokens on iOS, or when reviewing the iOS build for security.
---

# iOS security

Threat model: an attacker with the device (lost/stolen, or jailbroken), an attacker on the network,
and anyone who can read the app bundle — the IPA is decompilable and every string in it is public.

## Quick start

Before changing anything under `core/auth` on iOS, confirm the current posture is intact:

```bash
grep -rn "kSecAttrAccessible\|NSUserDefaults" core/auth/src/iosMain
grep -n "NSAllowsArbitraryLoads\|CFBundleURLTypes" iosApp/iosApp/Info.plist
```

The first must show `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` and no token in
`NSUserDefaults`; the second must show the redirect scheme and **no** ATS exception.

## Token storage — done, keep it that way

`core/auth/src/iosMain/.../TokenStorage.ios.kt` is `KeychainTokenStorage`:
`SecItemAdd`/`SecItemCopyMatching`/`SecItemDelete` on a `kSecClassGenericPassword` item with a
stable `kSecAttrService`/`kSecAttrAccount` pair, protected by
**`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`** — `…ThisDeviceOnly` keeps it out of backups
and off the user's other devices, `AfterFirstUnlock` still permits a background refresh. It also
migrates and deletes any payload left by the previous `NSUserDefaults` implementation.

Do not regress this: never `kSecAttrAccessibleAlways`, never move a token back into
`NSUserDefaults`, and keep `SecItemDelete` on sign-out. `TokenStorage` is the seam, so any change
stays `iosMain`-only. The access token is held behind `AuthSession`'s `Mutex`; keeping it in memory
only (Keychain for the refresh token alone) is the remaining hardening step.

Exercise the Keychain path on a simulator when it changes — read, write, delete, and the
upgrade-from-`NSUserDefaults` path.

## The rest of the surface

- OAuth (`ASWebAuthenticationSession`, PKCE, redirect scheme, sign-out), HTTPS/ATS rules and
  logging: [references/oauth_and_transport.md](references/oauth_and_transport.md).
- Local database and `LocalCache` protection, pasteboard and snapshot exposure, secrets in the
  static framework, privacy manifest and purpose strings:
  [references/data_at_rest_and_privacy.md](references/data_at_rest_and_privacy.md).

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
