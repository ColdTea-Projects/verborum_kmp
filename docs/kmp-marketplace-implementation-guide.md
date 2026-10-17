# Verborum KMP — Marketplace (Forum) & Profile implementation guide

Audience: the AI agent working on the Verborum **KMP project** (iOS + Web). Goal: port, behaviour for
behaviour, everything the Android app gained after the pre-marketplace version — the **Profile** screen
in Options, the **Forum** tab, and the **Bibliotheca** changes that come with them.

Status of this document: written 2026-10-11 from the Android code at commit `5457196`. Where this guide
and the Android code disagree, the code wins; where the code and `docs/marketplace-client-guide.md` (the
backend contract) disagree, tell the user.

---

## 0. How to use this guide

1. **Read first, in this order:** `docs/marketplace-client-guide.md` (the backend contract — every
   endpoint, status code and rule), then this guide (the *client decisions* taken on top of it).
2. This guide describes behaviour in platform-neutral terms. Android file names are given as
   `Class (module)` references so you can read the original; **do not port Android APIs** (Retrofit,
   Hilt, Room, WorkManager, Paging 3, Compose). Map each concept onto whatever the KMP project already
   uses for networking, persistence, DI and UI. Inspect the KMP project's existing slices first and
   mirror their layering and naming — your job is the same *behaviour*, in the KMP project's idiom.
3. Android is the reference implementation. If something is unclear, read the Android class named in
   the step; it is small and heavily commented.
4. Work in the order of the steps in §3. Each step is independently buildable and testable.
5. Never invent backend behaviour. If an endpoint answers something this guide does not list, treat it
   as a generic failure and report it.

### Android → KMP concept map

| Android | KMP (commonMain unless noted) |
|---|---|
| Retrofit interface | Ktor/your HTTP client API class, one per backend service |
| `Response<T>` status checks | Inspect the HTTP status explicitly; do **not** let non-2xx throw before you read it for the endpoints marked "status-mapped" |
| Room entity / DAO / migration | Whatever the KMP project persists with (SQLDelight / Room KMP / IndexedDB-backed on Web). New tables + a schema migration |
| Hilt `@IntoSet` hooks | A plain list/registry of hook objects injected into the services that notify them |
| WorkManager delayed sync | A coroutine `delay` in an app-scoped job (iOS and Web both have no WorkManager; see §6) |
| Paging 3 | Page-number paging with `hasNext` (infinite scroll); see §3.8 |
| `StateFlow` + `combine` | `StateFlow`/`combine` — same |

---

## 1. Scope

**In scope**

- Options → **Profile** screen (email, display name, Forum terms, Update, leave-Forum confirmation).
- **Forum** tab wired to the live marketplace backend: Popular / Fresh tabs, filters/search, download,
  rating, member gate.
- **Bibliotheca** changes: downloaded ("imported") dictionaries, read-only handling, unsubscribe,
  sharing switch, keep-one-shared rules, server-owned `isPublic`, safe delete, post-membership resync,
  hiding downloads from non-members.

**Out of scope (not built on Android either — do not invent)**

- Forum offline mode / caching of listings (the Forum is **online-only, no database**).
- A "Top rated" tab (`/top-rated` exists on the backend; the product decided **not** to use it).
- "No longer available" state for vault entries (unreadable ones are silently dropped locally).
- Publisher names for dictionaries downloaded on *another* device; "more from this publisher".
- Free-text search (guide §10). Real legal terms text (the terms are an English placeholder).

---

## 2. Invariants (the rules everything else follows)

Memorise these; most bugs come from breaking one.

1. **Membership = display name + accepted terms.** Only members may browse/import/rate. The server
   returns 403 otherwise. Client mirrors it with a blocker screen; it does not rely on 403 alone.
2. **`GET /users/me` is the source of truth** for the profile. Create a profile only after `/me`
   answers **404**, with a **fresh random UUID** as `userId` (never the token's `sub`, never reused).
3. **Two different ids.** `sub` (JWT subject) is the owner key in ms_dictionary / ms_marketplace
   (`publisherId`, dictionary `userId`). ms_user's own `id` from `/users/me` is what `/users/{userId}/vault`
   paths take. Never mix them.
4. **The server owns `isPublic` once a dictionary exists.** Joining the Forum makes *all* the user's
   dictionaries public; leaving makes *all* private (server flips them, by event, within ~1 s). The
   client never pushes a stale local `isPublic`, and always takes `isPublic` from the server on sync.
5. **A member must always keep ≥ 1 shared dictionary** (server rule: 400 `SharingRequiredException`).
   Client enforces it in the UI too: cannot unshare, cannot delete the last shared one.
6. **Downloaded dictionaries are read-only copies of someone else's data**, stored in their own
   tables, **never** uploaded, never touched by the sync upload/dirty set. Only the user's practice
   `level` on a downloaded word is local-writable, and it stays on the device.
7. **Forum and vault changes are online-only** (profile update, sharing change, unsubscribe, rating,
   download). They are never queued. Failure → error message, nothing changes locally.
8. **Everything in a listing is untrusted text** written by another user: render as plain text only
   (no markdown/HTML interpretation, no auto-linking).
9. **Every user-visible string is localized** in all locales the KMP project supports.

---

## 3. Step by step

### Step 1 — Configuration and networking

Since the API gateway there is **one** backend base URL for every service (Android debug `http://10.0.2.2:8080/`; iOS
simulator and local web `http://localhost:8080`; the KMP project's per-environment config; see
`docs/production-cutover.md`). Nothing to add for the marketplace — the same client reaches it by path:

| Service | Purpose | Path prefix on the gateway |
|---|---|---|
| ms_user | profile + vault | `/users/**` |
| ms_marketplace | browse/import/rating | `/marketplace/**` |

Rules:

- Every call carries the bearer token (same token layer as existing calls; same 401-refresh-retry
  behaviour).
- The access token must contain `aud` ∋ `verborum-api` (backend SEC-05). A **401 "aud claim is not
  valid"** is not a client bug: the Keycloak audience mapper is missing — `docker compose up
  keycloak-bootstrap` in the backend repo.
- JSON: lenient decoding (ignore unknown keys), **omit null fields when encoding** (a null in a PUT body
  must be *absent*, not `null`, because absent = "unchanged" on several endpoints).
- **Web only:** the browser needs CORS for the web origin; the gateway answers it (`CORS_ALLOWED_ORIGINS`, local
  default `http://localhost:8280`). A new custom request header must be added to the gateway's allowed list first.

Create a `core`-level typed model for API errors: `{ error: String?, errorDetail: String? }`. `error` is
the exception class name (e.g. `SharingRequiredException`, `InvalidRequestException`), `errorDetail` is
a sentence written for people. Helper: "read this body off a non-2xx response".

### Step 2 — Profile slice (data + domain), in the shared/core layer

Reference: `core/profile/**` (Android).

**API (ms_user)**

| Call | Endpoint | Notes |
|---|---|---|
| getMe | `GET users/me` | 200 profile, **404 = no profile yet** |
| createProfile | `POST users/` | body `{userId, keycloakId, email, displayName?}` |
| updateProfileInfo | `PUT users/me/profile-info` | body `{displayName?, marketplaceAgreementAccepted?, marketplaceAgreementVersion?}`; absent = unchanged; `displayName: ""` removes the name |
| getVault | `GET users/{userId}/vault` | `[{vaultEntryId?, dictionaryId, importedAt?}]` |
| removeFromVault | `DELETE users/{userId}/vault/{dictionaryId}` | 200, also when already gone |

**DTO `UserProfileResponse`**: `id`, `email?`, `displayName?`, `marketplaceAgreementAccepted` (default
false), `marketplaceAgreementVersion?`. Convert to a domain `UserProfile(userId=id, email=email.orEmpty,
displayName=blank→null, isMarketplaceMember=marketplaceAgreementAccepted, marketplaceAgreementVersion)`.
UI model `ProfileUi(email, displayName (empty when none), isMarketplaceMember)`.

**Constants**

- `MarketplaceTerms.VERSION = "2026-10-04"` — **use the same value as Android** so one account looks
  consistent across clients. Bump it only when the terms text changes (backend stores whatever you send).
- `MAX_DISPLAY_NAME_LENGTH = 255`.

**`EnsureUserProfile(subject, email, displayName)`** — exact algorithm:

1. `GET /users/me`. 2xx → return it. Not 404 → throw (generic failure).
2. 404 → need an email. `email` comes from the **access token claims** (`email`; display name from
   `name`/`preferred_username`, see `JwtDecoder`). No email → fail (`"No email claim"`). Trim the
   token's name, drop if empty, cap at 255.
3. `POST /users/` with `userId = random UUID`, `keycloakId = sub`, `email`, `displayName?`.
4. If the answer is **400 and a name was sent** → the name probably contains a reserved word
   ("admin", "staff", "support", "verborum"…). Retry **once without a name**, with a **new** random
   `userId`. The user then picks a name on the Profile screen.
5. Any non-2xx other than **409** → throw. **409** = a profile appeared meanwhile (another device,
   retried login) → do **not** post again.
6. `GET /users/me` again and return it (404 now = failure).

**`UpdateProfileInfo(request)`**: 2xx ok; **400 → `InvalidProfileException(errorDetail)`** (message is
user-readable: reserved word, too long, email taken…); anything else → generic failure.

**`ProfileService`** (singleton / app-scoped; the session's profile cache):

- `observeProfile(): Flow<ProfileUi?>` — null until the first successful load; distinct-until-changed.
- `refreshProfile()` — `getSessionIdentity()` (sub/email/name from the stored token; throw if signed
  out) → `EnsureUserProfile` → store → return.
- `getUserId()` — cached ms_user id, loading the profile first if unknown. Used for vault paths.
- `updateProfile(displayName?, accepted?)` — send only what was passed; when `accepted == true`
  also send `marketplaceAgreementVersion = MarketplaceTerms.VERSION`. Then `refreshProfile()` (the
  profile in the cache is **always** what `/me` says after an update). If `accepted != null`,
  call every `MarketplaceMembershipHook.onMembershipChanged(isMember)` — each isolated in try/catch so
  a hook failure never fails the update.
- `clear()` — wipe the cache. **Register it as a post-logout hook**; the next account on the device
  must never see the previous profile.
- Online-only; never queue profile changes.

**When the profile loads** (lazy, no login-time creation): Profile screen on entry (always refresh —
it may have changed on another device); the Forum gate on every visit; Bibliotheca's details screen
refreshes membership best-effort (offline → stays unknown).

**`MarketplaceMembershipHook`**: `suspend onMembershipChanged(isMember: Boolean)`. Defined in the
shared/core layer; implemented by Bibliotheca (Step 5.9). It must be quick: schedule, don't do heavy
work inline.

### Step 3 — Options → Profile screen

Reference: `core/profile/ui/profile/**`.

Add a "Profile" entry to Options (title string `optionsProfile`). Screen contents, top to bottom:

1. **Email** — read-only text field (readOnly, *not* disabled, so it stays legible).
2. **Display name** — editable, single line, capped at 255 characters (`take(255)` on input), label
   "Display name", supporting text "Shown to others on the dictionaries you share in the Forum". When
   the field is in the "missing" state it is shown as an error with the text "A display name is
   required to accept the Forum terms".
3. **Forum terms section** — an expandable block: header "Forum terms" with Show/Hide toggle; the
   (placeholder, English-only, untranslated) terms text when expanded; an "I agree to the Forum terms"
   checkbox that is always visible (not hidden inside the expansion).
4. **Update button** — sticky at the bottom (the content above it scrolls; the button never does).
   Shows a progress indicator while saving. After success a "Profile updated" message.

**Form model** `ProfileForm(email, savedDisplayName, savedAgreementAccepted, displayName,
agreementAccepted, isSaving, isLeaveConfirmationShown)`:

```
trimmedDisplayName   = displayName.trim()            // server trims; whitespace-only edits aren't changes
hasChanges           = trimmedDisplayName != savedDisplayName || agreementAccepted != savedAgreementAccepted
isDisplayNameMissing = agreementAccepted && trimmedDisplayName.isEmpty()
isLeavingForum       = savedAgreementAccepted && !agreementAccepted
canUpdate            = hasChanges && !isDisplayNameMissing && !isSaving
```

So: Update is disabled when nothing changed, when the terms are ticked with an empty name, and while
saving. Terms can be unticked with an empty name (that is how a user leaves; an empty name is legal
when not a member).

**ViewModel behaviour**

- `init`: observe `profileService.observeProfile()`; every emission (first load, and the reload after
  an update) **resets the form to the saved values** — this is also how "update succeeded" becomes
  visible. Then `refresh()`.
- States: `Loading` / `Success(form)` / `Failed`. A failed *first* load → `Failed` + retry; if a
  cached profile exists, a failed refresh keeps the editable form (no error screen).
- `update()`:
  - if `!canUpdate` → nothing;
  - if `isLeavingForum` → set `isLeaveConfirmationShown = true` and **stop** (confirmation first);
  - else `save()`.
- `confirmLeaveForum()`: hide the dialog, then `save()` if still `canUpdate`.
  `dismissLeaveConfirmation()`: hide the dialog, keep the drafts, send nothing.
- `save()`: `isSaving = true`; call `updateProfile(displayName = trimmed if != saved else null,
  accepted = agreement if != savedAgreement else null)`. Success → "Profile updated". `InvalidProfile
  Exception` → show its `detail` (server text) if present, else the generic "Couldn't update your
  profile. Please try again."; any other error → generic text. **Always** `isSaving = false` in
  `finally`. The user's drafts stay on failure.
- Leaving the screen with unsaved edits needs no prompt (Android has none).

**Leave-the-Forum dialog** (guide §6.2): title "Leave the Forum?"; message "All your dictionaries will
become private, and people who downloaded them will lose access. You can join again anytime."; confirm
"Leave the Forum" (destructive colour), cancel "Stay". Joining and renaming never show a dialog.

### Step 4 — Persistence for downloaded dictionaries (migration)

Two new tables in the Bibliotheca database — **their own tables, never shared with `dictionary`/`word`**
— plus a schema migration (bump the DB version by one; the Android DB went 3 → 4):

`imported_dictionary` — primary key `dictionary_id`
| column | type | notes |
|---|---|---|
| dictionary_id | text | PK |
| publisher_id | text | owner's `sub` |
| publisher_name | text, nullable | only known from the Forum listing it was downloaded from |
| name | text | |
| from_lang | text | |
| to_lang | text | |
| tags | text | JSON array of lowercase tag codes, same encoding as `dictionary.tags` |
| imported_at | integer | epoch millis |

`imported_word` — primary key `word_id`
| column | type | notes |
|---|---|---|
| word_id | text | PK |
| fk_dictionary_id | text | |
| word | text | |
| word_meta | text | same surfaces/meta JSON contract as own words |
| translation | text | |
| translation_meta | text | |
| level | integer | **local practice progress, device-only**; starts at 0 |
| created_at | integer | |
| updated_at | integer | |

Queries needed (all observable): all dictionaries; one dictionary; all dictionary ids; words of one
dictionary (ordered `created_at, word_id`); words of every imported dictionary whose
`(from_lang,to_lang)` equals a given pair, **case-insensitive**; word counts grouped by dictionary.
Writes: save dictionary + words (see replaceWords), delete one dictionary (cascade its words), update
one word's `level`, delete all (logout).

**`replaceWords(dictionaryId, words)`** (transactional): read existing `word_id → level`; delete the
dictionary's words; insert the new ones with `level = old level if the word id is still there, else
the incoming level (0)`. This preserves practice progress across refreshes.

Also: the **post-logout wipe** (`ClearLocalData`) must delete both tables.

### Step 5 — Bibliotheca changes

#### 5.1 Models

`DictionaryUi` gains two fields: `isImported: Boolean = false` and `publisherName: String? = null`.
An imported dictionary converts to UI as: `userId = publisherId`, `isPublic = true`, `createdAt =
updatedAt = importedAt` (so a download sorts by when it was downloaded), `isImported = true`,
`publisherName`. Own dictionaries keep `isImported = false`.

#### 5.2 The imported slice (service)

`ImportedDictionaryService`:

- `observeDictionaries`, `observeDictionary(id)`, `observeDictionaryIds`, `observeWords(id)`,
  `observeWordsInLanguagePair(from,to)`, `observeWordCounts`, `updateWordLevel(wordId, level)`.
- **`addImportedDictionary(dictionaryId, publisherName)`**: fetch the content (below) and save it. If
  the dictionary is unreadable (404 → null), store nothing and do not throw.
- **`refreshFromVault()`** — mirrors the vault; called from the sync run (Step 5.8):
  1. `vault = GET users/{userId}/vault` (`userId` = ms_user id via `getUserId()`); `local` = stored
     imported dictionaries.
  2. Delete local ones not in the vault **unless** `now - importedAt <= 10 min` (the backend adds the
     vault entry asynchronously after import; without the grace window a fresh download would vanish).
  3. For every vault entry: re-fetch the owner's current version, keeping the known `publisherName`
     and `importedAt` (`entry.importedAt ?: now` for a dictionary downloaded on another device). Fetch
     returned null → delete locally (made private/deleted, guide §6.9). Fetch **threw** → keep the
     local copy, log, continue with the next entry (a flaky network must never look like a removal).
- **`unsubscribe(id)`**: `DELETE users/{userId}/vault/{id}` must return 2xx **first**; only then
  delete locally. Online-only; throws when unreachable, local state untouched.
- Rating pass-throughs (`getMyRating`, `setRating`, `removeRating`) delegating to the Forum rating
  service (see Step 6.7). Because Bibliotheca cannot depend on the Forum module, **declare an interface
  in core** (`MarketplaceRatings`: `getMyRating(id): Int?`, `setRating(id, stars): RatingOutcome`,
  `removeRating(id): RatingOutcome`) and let the Forum module implement/bind it. Same pattern for
  `ImportedDictionariesStore` (Forum → Bibliotheca: `observeDictionaryIds()`,
  `addImportedDictionary(id, publisherName)`) and `OwnedDictionariesSource` (Forum reads the user's own
  dictionaries' language pairs). Keep module dependencies one-directional exactly as Android does.

**Content fetch** (`FetchImportedDictionaryContent(dictionaryId, publisherName, importedAt)`) — reads
from ms_dictionary (not ms_marketplace):

1. `GET dictionaries/dictionary/{id}` — **404 → return null**; other errors throw.
2. `GET dictionaries/{id}/tags` → lowercase, trimmed, non-empty, distinct tag codes.
3. `GET words/dictionary/{id}` → drop entries without `wordId`; map `word`, `wordMeta`,
   `translation`, `translationMeta` (empty string when null); `createdAt = parse(createdAt) ?:
   importedAt`, `updatedAt = parse(updatedAt) ?: createdAt`; **`level = 0`** (the server's level is the
   owner's and is always null for you).

#### 5.3 Combine own + imported in every read path

- **Dictionary list**: `own + imported`, with tags dropped from the list projection (tags are shown
  only on the details screen) and the combined flow distinct-until-changed.
- **Single dictionary**: `own ?: imported`.
- **Words of a dictionary**: own words if it is an own dictionary, else imported words.
- **Practice / exercises** (words by language pair): `own pair words + imported pair words`.
- **Word counts**: merge both maps.
- **Word level updates**: try the own table, else the imported table; no-op if neither has the word.

#### 5.4 Hide downloads from non-members

When the profile is **known** and `isMarketplaceMember == false`, the imported slice's *observe* flows
return empty (`[]`, `null`, `{}`): list, details (incl. deep links), words, practice pairs, counts.
Local rows are kept, so re-joining brings them back immediately. **If the profile is not loaded yet,
do not hide** (an offline cold start must not strip a member of their downloads). Implementation:
`profile.observe().map { it?.isMember != false }.distinctUntilChanged().flatMapLatest { allowed ->
if (allowed) source else flowOf(blockedValue) }`. Do not hide the id set the Forum uses for its
"Downloaded" badge.

#### 5.5 Read-only UI for imported dictionaries

List (long-press / overflow sheet):
- Own: **Edit** + **Delete** (unchanged).
- Imported: **no Edit**; **Unsubscribe** replaces Delete → confirmation dialog ("Unsubscribe from
  “%1$s”?" / "It will be removed from your Bibliotheca, along with your practice progress on it. You
  can download it again from the Forum." / confirm "Unsubscribe"; cancel = existing cancel string).
  Failure → "Couldn't unsubscribe. Check your connection and try again."
- Card: a **"Downloaded"** badge (`dictionaryBadgeDownloaded`).

Details screen of an imported dictionary:
- Subtitle: `"<languages> · <N words> · by <publisherName>"` — the "by …" part only when the name is
  known (`forumPublishedBy`: "by %1$s").
- **No** add-word button, **no** edit/delete on words (hide swipe actions, overflow menus, long-press
  edit; the word row takes a `readOnly` flag), **no** sharing switch.
- An **Unsubscribe** button (same confirmation dialog); on success navigate back. A dictionary that
  disappears (`observeDictionary` → null) also navigates back.
- A **rating card** (Step 6.7), shown only for downloaded, not-own dictionaries.
- Practice still works on it; level changes persist device-locally.

#### 5.6 Deleting a dictionary (safe order — this was a data-loss bug on Android)

**Send only `DELETE /dictionaries/{id}`.** The server deletes the words in the same transaction *after*
checking the sharing rule. Never delete words remotely first (if the server then refuses, the words
are already gone). `DeleteDictionaryRemotely(id)` returns:

| Server answer | Local action | Result |
|---|---|---|
| 2xx | delete words + dictionary locally | `DELETED` |
| 400 `error == "SharingRequiredException"` | lift tombstone (restore) | `SHARING_REQUIRED` |
| any other 400 | lift tombstone | `REFUSED` |
| unreachable / 5xx / other | touch nothing here | `PENDING` |

`deleteDictionary(id)`: call the above; on `PENDING` set the **tombstone** (hide locally, mark for
deletion). The existing sync upload phase retries tombstones using the *same* use case — so a refused
delete is never retried forever (lifting the tombstone on 400 is what stops that).

UI mapping: `SHARING_REQUIRED` → snackbar "This is your last shared dictionary. Share another one
first, or leave the Forum to make all of them private." (`errorDeleteLastShared`); `REFUSED` and
exceptions → the existing generic delete-failed snackbar; `DELETED`/`PENDING` → silent.
**Client guard:** before calling the server, if `sharingState.isLastShared(id)` show the same snackbar
and do nothing.

#### 5.7 Sharing: server-owned `isPublic`, the switch, keep-one-shared

**`SharingState(isMember, sharedDictionaryIds)`** from `profile.isMember` (unknown = false) and the
user's own dictionaries with `isPublic == true`. `isLastShared(id) = isMember && sharedIds == {id}`.

**Upload (`SaveDictionary`, i.e. `POST /dictionaries/` upsert) — never sends a stale `isPublic`:**

1. POST with `isPublic` **omitted** (null = unchanged). Success → done.
2. If 400 and `error == "InvalidRequestException"` → the dictionary is new to the server (it requires
   `isPublic` on create): POST again with the **local** `isPublic`.
3. If that is 400 `SharingRequiredException` **and local `isPublic` was false** → a member must
   keep one shared; POST once more with `isPublic = true`.

**Download/sync (`SyncUserDictionaries`)**: `isPublic` always comes from the server. For rows where
the local copy has unsynced edits (local normally wins), still apply the server's `isPublic` to the
local row, touching nothing else (`updateIsPublic`). Without this, a rejected row retries forever.

**`createDictionary`**: `isPublic = isMember && sharedDictionaryIds.isEmpty()` — new dictionaries are
private, except a member's first shared one.

**`setShared(id, isPublic)`** (details-screen switch; online-only):
- send `PUT /dictionaries/` with the full dictionary request and the **explicit** `isPublic` (this is
  the one place an explicit value is sent for an existing dictionary);
- 2xx → write `isPublic` onto the local row (`UPDATED`);
- 400 `SharingRequiredException` → `LAST_SHARED` (snackbar `errorDeleteLastShared`-style message);
- anything else / exception → `FAILED` (snackbar "Couldn't change sharing. Check your connection and
  try again.").

**The switch** (own dictionaries, **members only**; hidden for non-members and for imported ones):
- Row: title "Shared in the Forum"; subtitle "Forum members can see and download it" when on, "Only
  you can see it" when off; when it is the last shared one: "Your last shared dictionary — while
  you're in the Forum, one must stay shared".
- `canToggle = !isUpdating && !(isShared && isLastShared)`: the last shared dictionary's switch is on
  and **disabled**; the switch is also disabled while a request is in flight.
- **Unsharing is confirmed** first: "Stop sharing “%1$s”?" / "It leaves the Forum, and people who
  downloaded it lose access." / confirm "Stop sharing". Sharing needs no confirmation.
- **Design:** the *off* state must still read as a switch. Android's default colours rendered the off
  track and the card background almost identical. Use explicit colours: on = primary track + onPrimary
  thumb; off = a visible outline/border + onSurfaceVariant thumb on a background-coloured track;
  disabled = same colours at ~0.5 alpha. Check light **and** dark theme.

#### 5.8 Sync run additions

At the end of the existing full sync, call `importedDictionaryService.refreshFromVault()` **inside its
own try/catch** (a vault failure must not fail the dictionary sync). Imported tables are never part of
the upload set. Order stays: upload pending → download remote → vault refresh.

#### 5.9 Membership change → resync

`BibliothecaMembershipHook.onMembershipChanged` → schedule a **full** sync **~3 s later**, replacing
any pending one (the server flips every `isPublic` by event within about a second; syncing instantly
would read the old values). Only the latest scheduled run survives. See §6 for how to schedule on
iOS/Web.

#### 5.10 Logout

`ClearLocalData` also deletes `imported_dictionary` and `imported_word`; `ProfileService.clear()` runs
as a post-logout hook (Step 2).

### Step 6 — The Forum module

The Forum has **no database**. Everything is read live; the only things remembered are in-memory.

#### 6.1 API (ms_marketplace)

| Call | Endpoint | Notes |
|---|---|---|
| getListings | `GET marketplace/dictionaries?page&size&pair&tag&publisher` | newest first |
| getPopularListings | `GET marketplace/dictionaries/popular?…same…` | most imported first |
| importDictionary | `POST marketplace/dictionaries/{id}/import` | no body; 201 (also on repeat) |
| getRating | `GET marketplace/dictionaries/{id}/rating` | 200 `{dictionaryId?, stars, ratedAt?}` / 404 not rated |
| putRating | `PUT marketplace/dictionaries/{id}/rating` | body `{stars: 1..5}` |
| deleteRating | `DELETE marketplace/dictionaries/{id}/rating` | 200 also when none |

Query rules: `page` is **zero-based**; `pair` = `"EN-TR"` (two codes joined by `-`, uppercase, either
direction matches), **repeated** parameter (`pair=EN-TR&pair=FR-DE`), at most 10; `tag` repeated, never
comma-joined, at most 10; `publisher` = part of the display name, 3–255 chars, case-insensitive. Omit
empty parameters.

ms_dictionary reads for someone else's *public* dictionary (read-only; writes are 403):
`GET dictionaries/dictionary/{id}` (404 = gone), `GET dictionaries/batch?ids=a,b,c` (unreadable ids
silently omitted), `GET dictionaries/{id}/tags`, `GET words/dictionary/{id}` (unreadable → `[]`, not
404; `level` is always null).

**DTOs**
- `MarketplacePage { items, page, size, hasNext }` (only these four; ignore unknown).
- `MarketplaceListing { dictionaryId, publisherId (= owner sub, never shown), publisherName?, name,
  fromLang, toLang (uppercase, real direction), tags (lowercase, sorted, [] if none), importCount,
  ratingAverage? (null while unrated), ratingCount, publishedAt (ISO-8601 UTC) }`.
- Conversion rules: `publisherName` blank → null; **`ratingAverage` clamped to 1..5, `ratingCount`
  ≥ 0**; tags trimmed/lowercased/non-empty/distinct; `publishedAt` parsed, 0 if unparsable;
  **`distinctBy dictionaryId`** within a page.

#### 6.2 Member gate

`ForumAccessState = Loading | Member | NotMember | Failed`, derived from the *session profile*
(`combine(profile, accessLoadFailed)`):

```
profile != null  → Member if isMarketplaceMember else NotMember
loadFailed       → Failed        // only when there is no cached profile at all
else             → Loading
```

- Call `refreshAccess()` (= `profileService.refreshProfile()`) **every time the Forum tab is visited**
  — membership can change on another device. A cached profile keeps deciding when a refresh fails.
- It reacts live: joining/leaving on the Profile screen flips the Forum without a manual reload.
- `NotMember` → **blocker screen**: title "Join the Forum"; message "To browse and import shared
  dictionaries, go to Options → Profile, add a display name and accept the Forum terms." (A button that
  navigates to Options → Profile is a nice touch, not required.)
- `Failed` → generic error + retry. `Loading` → progress.

#### 6.3 Tabs and paging

- Two tabs, **Popular** (default selected) and **Fresh**: both share one filter; "Fresh" = newest
  endpoint, "Popular" = `/popular`.
- **Infinite scroll**, page size **15** (a fixed size — page numbers only line up with a constant
  size). Next page while `hasNext`. Never use a different size for the first load.
- A new filter or a pull-to-refresh builds a **new paging generation from page 0**; keep a `seenIds`
  set per generation and drop listings already delivered (a listing published mid-scroll can repeat,
  and a duplicate key crashes lazy lists).
- Keep each tab's loaded pages across tab switches, rotation/state changes and back-navigation (cache
  in the ViewModel's scope). A pager only runs while its tab is visible.
- States per list: loading (skeleton cards), empty ("No shared dictionaries yet" with no filter;
  "No shared dictionaries match these filters" with a filter), load error ("Couldn't load the shared
  dictionaries" + retry), append error ("Couldn't load more dictionaries" + retry), end.
- **403 on a listing request:** wait ~1 s and retry **once** (membership reaches the marketplace by
  event; a request right after joining can still be refused). A second 403 → treat as access denied
  (re-check membership). Other HTTP errors surface as the load error.

#### 6.4 Default filter (the user's top 3 language pairs)

On first entry (before any list loads — hold the lists until it is known, so nothing loads unfiltered
first), compute the default filter:

1. Collect `DictionaryLanguages(fromLang, toLang, lastActivityAt)` from **own dictionaries**
   (excluding tombstoned ones; `lastActivityAt = max(updatedAt, createdAt)`, since `updatedAt` is 0
   until the server first stamps it) **and** from **vault dictionaries**: `getVault(userId)` → one
   `GET dictionaries/batch?ids=…` → `importedAt` as activity. Each source is best-effort: a failure
   contributes an empty list, never an error.
2. `LanguagePairRanking.topPairs(all, limit = 3)`:
   - uppercase both langs; drop blanks and `from == to`;
   - **group by the unordered pair** (`{DE, EN}` — DE→EN and EN→DE are one pair, like the server's
     filter);
   - sort by group size descending (the number of dictionaries), then by most recent activity
     descending;
   - take 3; each pair is displayed in the direction of its most recently used dictionary.
3. The filter is `languagePairs = those pairs`; no dictionaries at all → empty filter (shows
   everything).

A filter the user applied before the default arrives **wins** over the late default
(`compareAndSet(null, default)`). "Clear filters" clears everything, **including** the default pairs.
Active filters are shown as removable chips above the list.

#### 6.5 Search / filter screen

A separate screen holding an **editable draft** (initialised once from the list's current filter —
once, so rotation keeps edits). Back changes nothing; only **Search** hands the draft to the list.

- **Language pairs**: add via a two-step bottom sheet ("Pick the first language" → "Pick a language to
  pair with %1$s"). Reject `first == second`, the same two languages in either order, and an 11th pair
  (max 10). Chip label `"%1$s ⇄ %2$s"`; each chip removable.
- **Tags**: toggle from the tag taxonomy (same taxonomy and translated labels Bibliotheca uses for
  dictionary tags); max 10 selected.
- **User** (publisher name): text field, capped at 255 chars; trimmed on Search; blank = no filter; if
  1–2 characters: error "Enter at least 3 characters" and Search disabled (the server would answer
  400).
- The search screen needs no data layer.

#### 6.6 Listing card and details

**Card** (same look as Bibliotheca's dictionary card): name; language pair; a meta line
`"<relative publish time> · by <publisherName>"` (publisher falls back to "Unknown publisher"; a word
count is shown only if known — the listing does not carry one, so normally it is omitted; the line
wraps to a second row for a long publisher name); tag chips along the bottom; **rating** inline: five
stars (rounded to the nearest half, gold/secondary accent) followed by `"4.3 (12)"` (one decimal +
count in parentheses, locale number formatting) — show **nothing** when unrated on the card (never 0
stars); badges: **"Yours"** when `publisherId == currentUser.sub`, **"Downloaded"** when downloaded.
The trailing control is a **⋮ overflow button** that opens a small options sheet containing
**Download**; it is **absent** for own and already-downloaded listings (`canDownload = !isOwn &&
!isDownloaded`), and replaced by a progress indicator while downloading. (Own listings essentially
never appear in browse lists since 2026-10-04; keep the badge, it is cheap and harmless.)

**Details screen:** loads in parallel — the dictionary (`GET dictionaries/dictionary/{id}`) and its
words (`GET words/dictionary/{id}`); 404 or any failure → failed state with retry. The marketplace has
**no single-listing endpoint**, so publisher name, import count, rating and tags come from an
**in-memory map of the listings loaded this session** (`loadedListings[dictionaryId]`), written when a
page is fetched. If the dictionary was not seen this session: tags are read fresh
(`/dictionaries/{id}/tags`), the rest degrade (no publisher → "Unknown publisher", rating shows "No
ratings yet", `publishedAt` falls back to the dictionary's `createdAt`).
Info panel: Creator, Rating (stars + `"4.3 (12)"`, or "No ratings yet"), Downloads (importCount), then
a "Word List" header and plain read-only word rows (word → translation). Download button as on the card.

#### 6.7 Download (import)

Shared by card and details (`ForumDownloads`): tracks `downloading` ids, `downloadedThisSession`
ids and `unavailable` ids, combined with the persisted downloaded ids from Bibliotheca
(`ImportedDictionariesStore.observeDictionaryIds()`) into `ForumUserState(userId, downloadedIds,
downloadingIds, unavailableIds)`, applied to listings **after** paging (so a download changes badges
without reloading a page; `unavailable` ones are filtered out of the list).

`download(listing)`:
1. Ignore if own, already downloaded, or already in flight (**double-tap guard**).
2. Mark downloading. `POST …/import`: 2xx → imported; **500 → retry once** (idempotent double-import
   race); 403 → `NotMember`; 404 → `Unavailable`; anything else / no connection → `Failed`.
3. `Imported`: mark "downloaded this session" (badge appears at once), then — **best effort** — call
   `ImportedDictionariesStore.addImportedDictionary(id, publisherName)` so it shows in Bibliotheca
   immediately; if that copy fails the next vault refresh repairs it, so only the import decides the
   result. Message "Downloaded to your Bibliotheca".
4. `Unavailable`: remove from the list, message "This dictionary is no longer available".
5. `NotMember`: re-check membership (`refreshAccess`) and show "Couldn't download the dictionary.
   Please try again."; `Failed`: same message.
6. Always clear the downloading flag in `finally`.

The `publisherName` passed to the store is the **listing's** name — it exists nowhere else and is the
only source of the "by …" subtitle in Bibliotheca.

#### 6.8 Rating a downloaded dictionary (guide §5.6, §6.10)

Rules: only for dictionaries the user **downloaded** and does **not own**; online-only.

- The rating is read once the dictionary is known to be downloaded (also when the download happens on
  this very screen): `GET …/rating` → 200 → stars (accept only 1..5); **404 → "not rated" (null)**;
  any other failure → keep the control hidden (never show a wrong "not rated" while offline).
- UI = an **expandable card**: collapsed (one row): the user's stars **small** (≈18 dp) when rated, or
  "Tap to rate" on the left when not; a **down chevron** on the right. Expanded: title "Your rating",
  five tappable stars at normal size (≈28 dp, ≥ 48 dp touch targets), hint "Tap a star to rate. Tap it
  again to remove your rating.", and an **up chevron** in the top-right. Expansion state survives
  rotation/process state; animate size.
- Tap a star: not the selected one → `PUT` that value; the **selected star again → `DELETE`**.
  While in flight, stars are disabled (`isUpdating`). On success update the card; on failure restore
  the previous value and show a message.
- Status mapping (`RatingOutcome`): 2xx `DONE`; 404 `UNAVAILABLE` ("This dictionary is no longer
  available, so it can't be rated."); 400/403 `NOT_ALLOWED` (own dictionary, not downloaded, not a
  member — show the generic failure text); anything else/exception `FAILED` ("Couldn't save your
  rating. Check your connection and try again.").
- Accessibility: collapsed small stars read as one phrase ("Your rating: N out of 5"); star buttons
  "Rate N out of 5"; the row has a click label "Show your rating"/"Hide your rating".
- The same card component is used on **both** the Forum details and the Bibliotheca details of an
  imported dictionary. Put it in the shared UI layer.

---

## 4. Localization

Every new key must exist in **all** supported locales (Android: 19 — base + `ar az de el es fa fr it
ja ko lt nl pl pt ru tr uk zh`). Translate everything a user reads; the terms text
(`profileTermsText`) stays an untranslated English placeholder for now. English values below; take the
other 18 translations from the Android resources (`core/src/main/res/values-*/strings.xml`,
`forum/src/main/res/values-*/strings.xml`, `bibliotheca/src/main/res/values-*/string.xml`) — the keys
are identical, so you can copy them verbatim.

| Area | Keys |
|---|---|
| Profile | `optionsProfile`, `profileEmailLabel`, `profileDisplayNameLabel`, `profileDisplayNameSupporting`, `profileDisplayNameRequired`, `profileTermsTitle`, `profileTermsShow`, `profileTermsHide`, `profileTermsAgree`, `profileTermsText`(untranslated), `profileUpdate`, `profileUpdated`, `profileUpdateError`, `leaveForumDialogTitle/Message/Confirm/Cancel` |
| Forum | `forumListScreenTitle/Subtitle`, `forumTabPopular`, `forumTabFresh`, `forumListEmpty`, `forumListLoadError`, `forumListLoadMoreError`, `forumListNoMatches`, `forumFilterTagsChip`, `forumFilterTagsTitle`, `forumFilterUserChip`, `forumFilterUserHint`, `forumSearch*`, `forumAddLanguagePair`, `forumRemoveLanguagePair`, `forumLanguagePair*`, `forumDownload`, `forumPublishedBy`, `forumUnknownPublisher`, `forumDetails*`, `forumRatingContentDescription`, `forumMoreOptions`, `forumMembershipTitle/Message`, `forumBadgeYours`, `forumDownloadSucceeded/Unavailable/Failed` |
| Rating | `ratingYourRating`, `ratingHint`, `ratingStarDescription`, `ratingTapToRate`, `ratingShow`, `ratingHide`, `ratingCurrent`, `errorRatingFailed`, `errorRatingUnavailable` |
| Bibliotheca | `dictionaryBadgeDownloaded`, `dictionaryOptionsUnsubscribe`, `unsubscribeDialogTitle/Message`, `errorUnsubscribeFailed`, `dictionarySharingTitle/On/Off/LastShared`, `unshareDialogTitle/Message/Confirm`, `errorSharingFailed`, `errorDeleteLastShared` |

If the KMP project uses a different key style, keep the *semantics* and map keys 1:1 in one table so the
translations can be copied mechanically.

---

## 5. Error → behaviour table (copy this into your tests)

| Situation | Status | Client behaviour |
|---|---|---|
| `/users/me` has no profile | 404 | create profile (random UUID) → `/me` again |
| create profile, name has reserved word | 400 | retry once without name, new UUID |
| create profile, already exists | 409 | do not repost; `/me` |
| update profile broke a rule | 400 + `errorDetail` | show `errorDetail`, keep drafts |
| browse as non-member | 403 | wait 1 s, retry once; then re-check membership / blocker |
| browse, `pair`/`tag` > 10 or `publisher` < 3 chars | 400 | prevent in the UI so it never happens |
| import race | 500 | retry once |
| import, not a member | 403 | refresh membership + failure message |
| import, made private/deleted | 404 | remove from list + "no longer available" |
| rating, not downloaded / own | 400 / 403 | failure message, restore previous |
| rating read, not rated | 404 | `null` (not an error) |
| delete dictionary, last shared | 400 `SharingRequiredException` | snackbar, lift tombstone |
| unshare, last shared | 400 `SharingRequiredException` | snackbar, switch unchanged |
| create dictionary without `isPublic` | 400 `InvalidRequestException` | resend with local `isPublic` |
| imported dictionary unreadable | 404 | drop local copy (vault refresh) |
| any 401 | 401 | existing token-refresh path; "aud claim not valid" = backend config |

---

## 6. iOS and Web specifics

**Shared behaviour is identical.** The differences are only where Android used Android APIs:

| Concern | iOS | Web |
|---|---|---|
| Delayed post-membership sync (Step 5.9) | App-scoped coroutine `delay(3_000)` then run the sync; a new request cancels the pending job. (No BGTaskScheduler needed — the user is in the app on the Profile screen.) | Same, a coroutine/`setTimeout` in the app scope; cancel-and-replace. |
| Persistence for imported tables | Same store the KMP app uses for Bibliotheca | Same (SQLite-WASM / IndexedDB-backed); keep the two tables transactional for `replaceWords` |
| Paging / infinite scroll | `LazyColumn`-equivalent (List with onAppear trigger of the last N items) | Intersection-observer sentinel near the end of the list |
| Expandable rating card | Native disclosure with animated height | Same; keep it keyboard-accessible (button, `aria-expanded`) |
| Dialogs (leave Forum, unshare, unsubscribe) | Native alert with destructive style on the confirm action | Modal dialog, focus trapped, Esc = cancel |
| Switch contrast | Respect the platform `Toggle`; verify the off-state track is visibly distinct from the card background in light *and* dark | Same; check WCAG contrast of the off track/border |
| "Tap to rate" copy | keep as is (translations exist) | consider "Click to rate" only if the product asks; keep the same key |
| Deep links to details | Optional | Support direct URL to a Forum details route: it must work with no list loaded (details degrade as in Step 6.6) |
| Token storage / refresh | Existing KMP auth layer | Existing KMP auth layer; never put tokens in `localStorage` if the project already chose otherwise |

---

## 7. Tests to write (mirror the Android ones)

Unit tests (commonTest, fake the API classes, no network):

- **EnsureUserProfile**: existing profile → no POST; 404 → POST with random UUID; 400-with-name →
  retried without name and a *different* UUID; 409 → `/me` re-read, no second POST; no email → fails.
- **UpdateProfileInfo**: 400 → `InvalidProfileException(errorDetail)`; 500 → generic.
- **ProfileForm**: `canUpdate` truth table (no change; name emptied while ticked; unticked + empty name
  allowed when it differs from saved; saving), `isLeavingForum`.
- **ProfileViewModel**: leaving asks for confirmation and sends nothing; confirm sends
  `accepted = false`; dismiss keeps drafts; rename sends no confirmation; success resets the form;
  rejected update keeps drafts and clears `isSaving`.
- **LanguagePairRanking**: direction-insensitive grouping, tie → recent activity, `from == to` ignored,
  limit 3, display direction of the latest.
- **Listings use case**: first 403 → one retry; second 403 → access-denied; non-403 rethrown. Page
  conversion: duplicates dropped, rating clamp, blank publisher → null.
- **Import use case**: 201 → Imported; 500 then 201 → Imported (two calls); 403/404/other mapping.
- **ForumDownloads**: own/downloaded/in-flight are no-ops; 404 → unavailable set; success marks
  downloaded.
- **Rating use cases**: 404 on read → null; other failure throws; status → `RatingOutcome` table.
- **ImportedDictionaryService**: refresh keeps publisher name/import time; grace window respected;
  unreadable → deleted; fetch exception → kept; unsubscribe: remote first, local only after success;
  non-member → observe flows empty; unknown profile → not hidden.
- **Persistence**: `replaceWords` keeps levels for surviving ids; migration creates both tables
  (verify against a fresh-install schema); logout wipe empties both.
- **DictionaryService**: delete → `DELETED`/`PENDING`(tombstone)/`SHARING_REQUIRED`/`REFUSED`;
  `setShared` outcomes; `createDictionary` isPublic rule; `SaveDictionary` fallback chain
  (omit → local → true).
- **Sync**: server `isPublic` overrides local even for locally modified rows; vault refresh failure
  does not fail the sync; imported rows never appear in the upload set.

---

## 8. Acceptance scenarios (manual, against the dev backend with a *member* and a *non-member*)

1. New account → Options → Profile shows the token's email read-only and (if the claim exists) its name.
2. Tick terms with an empty name → Update disabled; type a name → enabled → "Profile updated"; Forum
   now opens. Reserved word ("admin") → server message shown, drafts kept.
3. Existing dictionaries become shared ~3 s after joining; each dictionary's switch shows on.
4. Non-member opens Forum → blocker text; Bibliotheca shows **no** downloaded dictionaries.
5. Popular/Fresh load with the default top-3 pairs as chips; pull-to-refresh restarts at page 0; no
   duplicate cards when scrolling a few pages.
6. Download from the card → badge flips at once, dictionary appears in Bibliotheca with "Downloaded";
   details subtitle shows "by <name>"; edit/add/delete word controls are absent; Unsubscribe asks
   for confirmation and removes it.
7. Rate it → card collapses to small stars; tap the selected star → rating removed.
8. Switch off the only shared dictionary → blocked with the explanation; delete it → blocked with
   the same message; share a second one → now the first can be unshared / deleted.
9. Untick terms → confirmation dialog; Stay changes nothing; Leave → all dictionaries private after
   ~3 s, downloads disappear from Bibliotheca, Forum shows the blocker; re-join → downloads return.
10. Logout → profile cache and both imported tables are empty for the next account.

---

## 9. Known gaps — do not "fix" these silently

- Practice progress on downloaded words is device-local only (the server's level is the owner's).
- The vault refresh makes one request per downloaded dictionary (no batching yet).
- A dictionary downloaded on another device has no publisher name until re-downloaded.
- Terms text is a placeholder; `MarketplaceTerms.VERSION` is the date `2026-10-04`.
- Live-backend verification on Android was done mostly through unit tests; report anything that
  disagrees with this guide to the user instead of working around it.
