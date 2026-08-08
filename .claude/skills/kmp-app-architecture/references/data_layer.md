# Data layer contracts

Answers one decision: **how does data get in and out of a slice, and what happens when the network
or the local write fails?**

## Shapes and boundaries

- Repository interface + implementation live in the slice's `data/` package; the interface is what
  the domain layer depends on. The domain model lives in `domain/`, the row the screen renders in
  `ui/model/` — three shapes, mapped at each boundary, none of them shared.
- Every repository method returns `Outcome<T>` — never throws, never returns `null` to mean failure.
- Fakes are legitimate production stand-ins while an endpoint is pending; swap the Koin binding, not
  the call sites.
- DTOs (`@Serializable`) stay in the data layer and are mapped to domain models before crossing into
  `ui/`. Do not let a DTO reach a composable.
- **Not every endpoint uses `Envelope`.** The dictionary service answers with the payload directly,
  so those calls go through `plainApiCall` / `statusApiCall`; `apiCall` is for enveloped endpoints.

## The local copy

**The local copy is a store interface with two implementations, chosen per platform.**
`DictionaryStore` / `WordStore` are the single source of truth a slice observes.
`DatabaseDictionaryStore` / `DatabaseWordStore` back them with Room on iOS, so the library survives
a restart; `InMemoryDictionaryStore` / `InMemoryWordStore` back them on web, which has no local
database and keeps the library for the session. The feature's Koin module picks with
`getOrNull<BibliothecaDatabase>()`. Store reads are **suspending** because a database cannot answer
synchronously.

`LocalCache` is **not** the database and not a substitute for it: it is plaintext `NSUserDefaults`
on iOS and a no-op on web, so putting user content in it is a data-at-rest decision, not a detail.

## Sync, deletes and failed writes

- **Tombstones are the delete contract, on both platforms.** A delete flags the row `isDeleted` and
  hides it at once; a merge never lets the server resurrect it, and only drops the tombstone once
  the server has forgotten the row too. A merge likewise never wipes a row the server has not seen
  yet (`isSynced = false`), or work done offline would be lost. `DictionaryStoreContract` /
  `WordStoreContract` are the shared tests, run against both implementations.
- **A failed write is not automatically an error.** `VerborumError.isWorthKeeping()` decides: a
  request that never landed (`Network`, 5xx) leaves the row pending and reports success, because the
  save did happen locally; one the server refused (4xx, `Serialization`, `Unauthorized`) is rolled
  back and reported. Never widen this to "any failure keeps the row" — a row the backend rejects
  would then retry forever.
- **`UploadPendingChangesUseCase` runs in front of every pull**, inside `SyncService`. It is the
  only thing that drains offline changes, and the ordering is what stops a download racing an unsent
  row. Best-effort by design: a row that fails stays pending for the next pass.
