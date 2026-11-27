# Architecture map

The current shape of the app, kept up to date by the refactoring rounds in
[PROGRESS.md](PROGRESS.md). Decisions behind it are in [adr/](adr/). Open work
is in [TASKS.md](TASKS.md).

## What the app is

Click Clack is an Android app for two people. Each day both answer one question
in private, and the answers open together once both are in ("the seal"). Answers
earn bricks; the couple builds a weekly sculpture from them. There are also decks
(multi-question quizzes), instruments (published personality scales), wildcards
(questions the couple writes), a bucket list, moods, special dates and photos.

Backend: Firebase Auth (Google sign-in through Credential Manager), Firestore
(rules in `firestore.rules`, tested by `rules-test/`), Cloud Functions
(`functions/index.js`: pushes, sealing helpers). Without a `google-services.json`
the whole app runs on in-memory fakes.

## Packages and dependency direction

```
            ┌──────────► auth ───┐
ui ─────────┼──────────► data ───┼──► model
            └──────────► notify ─┘
     all of them ──► com.ampersand (AppConfig, FirestoreSchema)
```

Allowed edges, all one way:

| From | May use | Must not use |
|---|---|---|
| `model/` | Kotlin + `java.time` only | Android, Firebase, any other app package |
| `data/` | `model`, root (`AppConfig`, `FirestoreSchema`) | `ui`, `auth`, `notify` |
| `auth/` | `model`, root | `ui`, `data`, `notify` |
| `notify/` | `model`, root | `ui`, `data`, `auth` |
| `ui/` | everything above | Firebase directly |

`model/` is pure on purpose: everything in it runs on the JVM in unit tests.

## Modules and their public interfaces

| Module | Owns | Public interface |
|---|---|---|
| `model/` | Every business rule: question types and scoring, rewards (`Rewards.due`), plates and placement, streaks, turns, moods, dates, teams, zodiac, instruments and their badges, taste profiles, draw rules (`DrawRules.kt`) | Data classes + pure functions/objects |
| `data/CoupleRepository` | The couple's shared state and every write to it | `state: StateFlow<CoupleState>` + one command per user intent |
| `data/FirestoreCoupleRepository` | Listening to Firestore and mapping documents to `CoupleState` | Implements `CoupleRepository` |
| `data/FakeCoupleRepository` | The same contract in memory, for previews and the no-Firebase build | Implements `CoupleRepository` |
| `data/QuestionPool` | Loading the bundled question/deck/instrument assets, and the daily draw over them | `daily(...)`, `deck(id)`, `question(id)`, `places` |
| `data/FirestoreMapping` | Firestore documents ↔ model types | `toX()` readers, `toMap()` writers, `toDateOrNull()` |
| `com.ampersand` root | Build config and Firestore names, shared by every package | `AppConfig`, `FirestoreSchema` |
| `auth/` | Sign-in and pairing two accounts into a couple | `AuthRepository`, `PairingRepository` |
| `notify/` | Push token registration and the local daily reminder | `Push`, `Reminder` |
| `ui/theme`, `ui/components` | Design tokens and shared controls (DESIGN_SYSTEM.md) | Composables + token objects |
| `ui/<feature>` | One screen each | A `@Composable` taking state and callbacks |

## State ownership

| State | Owner | Notes |
|---|---|---|
| Signed-in user | `AuthRepository` (held by `AuthViewModel`) | |
| Pairing (couple id, partner) | `PairingRepository` (held by `PairingViewModel`) | |
| Everything about the couple | `CoupleRepository.state` (held by `AmpersandViewModel`) | One immutable `CoupleState`, rebuilt whole |
| Today's date | The couple's home zone (`HomeZone`), never the phone's | |
| Local preferences (sound, appearance, reminder time) | SharedPreferences, per feature | |
| Screen-local UI state | `remember` in the screen | |

## Technical solutions in use

One per concern; anything else is a deviation and goes in TASKS.md.

| Concern | Solution |
|---|---|
| UI | Jetpack Compose + Material 3, themed by `ui/theme` |
| State | `StateFlow<CoupleState>` from a repository held in a ViewModel |
| Persistence | Firestore with snapshot listeners; fakes in memory |
| Background | Firebase Messaging (pushes), AlarmManager (reminder) |
| Config | `AppConfig`, which reads `BuildConfig` (filled from `google-services.json` at build time) |
| Tests | JUnit on the JVM; Robolectric + Roborazzi for screenshots; node tests for rules |

## Canonical homes

The one place each core concept lives. If you are changing one of these, this is
the file; a second copy anywhere else is a bug.

| Concept | Authority |
|---|---|
| Which bricks are owed | `Rewards.due` (`model/Blocks.kt`) |
| What one day/talk/deck/bucket/mood earns | `Rewards.forDay/forTalk/forQuiz`, `BucketList.reward`, `MoodEntry.block` |
| A week's plate: length, id, rollover, legal placement | `BuildPlate.PERIOD_DAYS`, `startingOn`, `unplaced`, `placement` |
| Today's question | `QuestionPool.daily`, fed once from `FirestoreCoupleRepository.recompose` |
| Draw rules from history | `model/DrawRules.kt` (`firstWeekDay`, `gentleReturn`), `Turns`, `Profiles` |
| Whose turn, who picked | `Turns.chooser`, `Turns.canPick`, `Turns.pickedBy` |
| A day by date | `CoupleState.dayOn` |
| Reveal (the seal) | `firestore.rules` + `FirestoreCoupleRepository.openSeal` (the one retry) |
| Document ↔ model | `data/FirestoreMapping.kt`; stored dates via `toDateOrNull` |
| Collection names | `FirestoreSchema` (mirrored by `firestore.rules`, `functions/index.js`) |
| Build configuration | `AppConfig` |
| Firestore listener failure policy | `Query.watch` / `DocumentReference.watch` |
| Play `Task` → coroutine | `com.ampersand.await()` |
| Showing a notification | `notify/Notifier` |
| Design tokens, controls, date formats | `ui/theme/Theme.kt`, `ui/components/`, `ui/theme/DateFormats.kt` (DESIGN_SYSTEM.md) |
| Quiet-hour defaults | `CoupleState.QUIET_FROM` / `QUIET_TO` |

## Acceptance check (after round 12)

| Criterion | State |
|---|---|
| Each core concept has one authoritative implementation | Yes, per the table above. The one deliberate pair is `FakeCoupleRepository` vs Firestore, which share every rule through `model/` and differ only in storage. |
| Each core module has a clear responsibility and public interface | Yes: see "Modules and their public interfaces". |
| No unexplained multiple implementations | Remaining look-alikes are documented as different semantics in PROGRESS.md (the two "ago" phrasings, the non-`counted` count phrases). |
| No circular dependencies between core modules | Yes: `model` depends on nothing, `data`/`auth`/`notify` only on `model` and the root kernel, `ui` on all. |
| Business logic decoupled from UI, storage, frameworks | Rules are in `model/` (pure Kotlin, JVM-tested). Known UI-side leftovers: deck display order and the special-day card text, kept on purpose (see TASKS.md). |
| Functionality, data compatibility, external contracts preserved | No Firestore field, collection, rule or function changed. Unit suite 270 → 282 tests, 0 failures; release build checked with R8. |
