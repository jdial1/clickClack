# ADR 0002: Config and Firestore names live in a shared kernel, not in `auth/`

Status: accepted (rounds 5–6)

## Context

- "Does this build have Firebase?" was `auth.hasFirebaseConfigFor(context)`. It
  ignored its `context`, and `notify/` and `ui/` imported `auth/` just to ask.
  `GoogleAuthRepository` read `BuildConfig` directly in three more places.
- `USERS`, `COUPLES` and `INVITES` were `internal` constants in
  `auth/FirestorePairingRepository.kt`. `data/` imported them, which gave
  `data → auth`, the only sideways edge between the backend packages. `notify/`
  spelled `"users"` and `"fcmToken"` as literals, and the other twelve collection
  names were private to the couple repository.
- The notification channel id `"daily"` was spelled once in `Reminder` and once
  in `MessagingService`.
- `AmpersandViewModel` downcast its repository to `FirestoreCoupleRepository`
  to call `close()`.

## Options

1. Move the constants into `data/` and let `auth/` import `data/`. This trades
   one sideways edge for another.
2. A new Gradle module for shared code. That's too heavy for two small files;
   `settings.gradle.kts` deliberately keeps one module.
3. **Two files in the root package `com.ampersand`**, beside `MainActivity`:
   `AppConfig` (build configuration) and `FirestoreSchema` (collection names).
   Every package may depend on the root; the root depends on nothing.

## Decision

Option 3. `AppConfig.hasFirebase`, `AppConfig.webClientId` and
`AppConfig.version` are the only reads of `BuildConfig` outside Settings' version
line. `FirestoreSchema` holds every collection name and the push-token field.
`Reminder.CHANNEL` is the channel id. `CoupleRepository.close()` is part of the
contract, with a no-op default for the fake.

The deletion test also removed `createPairingRepository`'s unused `context` and
`PairingViewModel.factory`'s along with it.

## Consequences

- `data/`, `auth/` and `notify/` no longer import each other.
- Renaming a collection is one edit in Kotlin, plus `firestore.rules` and
  `functions/index.js`, which the schema file names.
- `ui/` no longer knows the Firestore implementation class.
