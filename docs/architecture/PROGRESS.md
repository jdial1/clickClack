# Refactoring progress

Branch `refactor/architecture-entropy`, from `master` 63e07c2. Each round is one
closed loop: scan, options, choose the least total complexity, change, validate,
record. Validation is `gradlew testDebugUnitTest` (JVM unit + screenshot tests).

Baseline on 63e07c2: 270 tests, 27 skipped, 0 failures.

## Round 1: one rule for brick awards (T1)

- **Found:** the "what bricks are owed" rule in eight places (five in Firestore,
  three in the fake), with different dedupe scopes.
- **Options:** align by hand; a shared helper in `data/`; one pure function in
  `model/`. See [ADR 0001](adr/0001-rewards-due-is-the-one-award-rule.md).
- **Chosen:** `Rewards.due(state, today, decks, plates)` in `model/Blocks.kt`.
  Firestore has one `award()` write; the fake applies it after every change.
- **Deleted:** `awardForToday`, `awardForQuizzes`, `awardForTalks`,
  `awardForBucket`, `awardForMoods`, `awardIfComplete` and two inline copies.
- **Behaviour:** today's brick now dedupes against every plate, like the rest.
  The fake awards moods and decks, and lays a talk the morning after, as
  Firestore always did.
- **Validated:** `RewardsDueTest` (7 cases). Full suite: 277 tests, 27 skipped, 0 failures.

## Round 2: plate rules in one place (T2)

- **Found:** placement legality written twice (the fake lacked the "week has
  ended" check), the weekly rollover written twice with different plate ids
  (`plate_<date>` in Firestore, `plate_<n>` in the fake), and the week length as
  three separate constants (`FirestoreCoupleRepository.PeriodDays`,
  `MockCouple.PeriodDays`, a literal `7L` in the mapper).
- **Deletion test:** `CoupleRepository.sealPlate()` had no caller. Firestore
  seals a week itself when it ends, so the interface method and the fake's
  version were deleted, not merged.
- **Chosen:** `BuildPlate.PERIOD_DAYS`, `BuildPlate.startingOn(from, carried)`,
  `BuildPlate.unplaced()` and `BuildPlate.placement(blockId, x, y, today)` in
  `model/Blocks.kt`. Both repositories call them; `emptyPlate` is gone.
- **Behaviour:** the fake now refuses a brick on a week that has ended, as
  Firestore does.
- **Validated:** `PlateRulesTest` (4 cases). Full suite: 281 tests, 27 skipped, 0 failures.

## Round 3: one daily draw call (T3)

- **Found:** `recompose()` called `pool.daily(...)` twice with the same 17
  arguments (once as the fallback when a pinned id no longer resolves, once as
  the normal draw), and parsed `marriedOn` from the couple document three times.
- **Options:** a `DrawInputs` value type in `model/`; a local `draw()` function.
  The value type would add a second shape for arguments `QuestionPool.daily`
  already names, with no second caller to share it, so it fails the deletion
  test. Chosen: the local function, and `marriedOn` read once.

## Round 4: the Firestore codec is its own file (T4)

- **Found:** 250 lines of document↔model mapping at the bottom of the
  1600-line repository, and `runCatching { LocalDate.parse(it) }.getOrNull()`
  written 15 times.
- **Chosen:** `data/FirestoreMapping.kt` holds every `DocumentSnapshot`→model
  reader and model→map writer, plus `String?.toDateOrNull()`, the one way a
  stored date is read. The repository keeps listening, recomposing and writing.
- **Validated (rounds 3–4):** full suite 281 tests, 27 skipped, 0 failures.

## Round 5: a shared kernel for config and Firestore names (T5, T8)

- **Found:** `data → auth` (collection names), `notify → auth` and `ui → auth`
  (Firebase check), a Firebase check that ignored its `context`, `BuildConfig`
  read in four places, `"users"`/`"fcmToken"`/`"daily"` as literals in `notify/`.
- **Chosen:** `com.ampersand.AppConfig` and `com.ampersand.FirestoreSchema`, see
  [ADR 0002](adr/0002-shared-kernel-for-config-and-schema.md). `Reminder.CHANNEL`
  is the one channel id.
- **Deleted:** `hasFirebaseConfigFor`, the `internal` constants in
  `FirestorePairingRepository`, the twelve private names in the couple
  repository, the unused `context` of `createPairingRepository` and
  `PairingViewModel.factory`.

## Round 6: the repository contract includes closing (T7)

- **Found:** `AmpersandViewModel` downcast to `FirestoreCoupleRepository` to
  call `close()`, so `ui/` knew the implementation class.
- **Chosen:** `CoupleRepository.close()` with a no-op default.
- **Validated (rounds 5–6):** full suite 281 tests, 27 skipped, 0 failures.

## Round 7: one listener policy, named defaults, dead code (T6, T15, T16, T19)

- **Listeners:** all 19 Firestore listeners go through `Query.watch` or
  `DocumentReference.watch`. A failed snapshot is logged and skipped, so the
  last good data stays. Before this, nine query listeners each had their own
  copy of the check, and the ten document listeners ignored the error and
  wrote `null` into state.
- **Defaults:** quiet hours are `CoupleState.QUIET_FROM`/`QUIET_TO`, used by the
  model's defaults and the Firestore fallback (two copies of 20/8 before).
- **Deleted (no callers):** `Theme.ColorChoices`, `BadgeWall.fitPercent` with
  its private `readings`, `ALIGNED` and `SPAN`, `BuildPlate.earnedBy`, and unused
  `MockCouple`/`MockQuestions` imports in seven screens.
- **Config:** Settings reads `AppConfig.versionName`. `BuildConfig` is now read
  only in `AppConfig`.
- **Scan findings** from a read-only pass over `ui/` and `model/` were added to
  TASKS.md as T11–T19, and the decisions that need a product call as P1–P2.
- **Validated:** full suite 281 tests, 27 skipped, 0 failures.

## Round 8: pure rules move to `model/` (T11)

- **Moved:** `BadgeWall`, `CoupleReadings`, `Profiles`, `Instruments`/`Sources`
  from `data/` to `model/`. `firstWeekDay`, `gentleReturn` and `QuietDays` moved
  out of `QuestionPool.kt` into `model/DrawRules.kt`, with the misattached KDoc
  fixed. Seven tests moved to `test/.../model/` unchanged apart from the
  package. See [ADR 0003](adr/0003-model-holds-rules-data-holds-io.md).
- **Validated:** full suite 281 tests, 27 skipped, 0 failures.

## Round 9: named date formats (T12)

- **Found:** ten `DateTimeFormatter` patterns across seven UI files, five of
  them duplicates (`"EEEE, MMMM d"` twice, `"MMMM d, yyyy"` four times,
  `"MMM d"` twice), one built inline on every recomposition in Settings.
- **Chosen:** `ui/theme/DateFormats.kt` with `Weekday`, `Full`, `MonthDay`, `Short`
  and `MonthShort`, named by what the date is for. Every screen uses them, and
  no UI file builds its own pattern. `model/Moments` and `model/Sun` keep
  theirs, because they produce text in the model, not on screen.
- **Not merged:** the two "N months ago" helpers. `HistoryScreen.ago` is a
  heading on the same calendar day ("A year ago today", capitalised, never
  under a month); `TodayScreen.agoIn` is mid-sentence relative time with a
  "a few weeks ago" case. Merging would add flags to one function for two
  callers, which fails the deletion test.
- **Validated:** full suite 281 tests, 27 skipped, 0 failures. DESIGN_SYSTEM.md gains a Dates section.

## Round 10: rules computed in composables move to their model types (T17, part)

- **`Turns.pickedBy(today, history, a, b)`** replaces `ShapeTomorrow.pickedBy`,
  which re-derived the chooser from the screen and called `history.first {}`
  (it would throw if yesterday was missing, a case `themeFor` happened to rule
  out). The screen now only turns the uid into "Your pick"/"Maya's pick".
- **`CoupleState.dayOn(date)`** is the one lookup across today, the history
  window and recalled days; `AmpersandApp` used an inline copy.
- **Left for later:** deck ordering in `DeckList` (it is only used there and
  reads like presentation order; moving it needs a look at what "order" means
  to the product), and `TodayCards.specialToday` (UI copy as much as rule).
- **Validated:** new `TurnsTest` case. Full suite: 282 tests, 27 skipped, 0 failures.

## Round 11: one notification path, one Task bridge (T14, T18)

- **Notifications:** `notify/Notifier` owns the channel, the tap intent, the
  builder and the guarded `notify`, plus the two ids (`REMINDER`, `PUSH`).
  `ReminderReceiver` and `MessagingService` each had a full copy; both are now
  one `Notifier.show(...)` call. `Reminder.CHANNEL` (round 5) moved to
  `Notifier.CHANNEL`, where the channel is created.
- **Task bridge:** `com.ampersand.await()` is the one `Task` → coroutine bridge.
  It replaces pairing's private `await(task)` (five calls) and three hand-written
  `suspendCancellableCoroutine` blocks in `GoogleAuthRepository`. The browser
  sign-in keeps its rule that a closed browser tab is not an error, now as a
  `catch`.
- **Validated:** full suite 282 tests, 27 skipped, 0 failures.
