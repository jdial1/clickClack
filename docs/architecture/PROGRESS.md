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

## Round 12: the fake accepts ZIPs like Firestore; one count phrase (T9, T13)

- **ZIP:** `FakeCoupleRepository` takes the pool's `ZipTable` and accepts a home
  ZIP only if `lookup` finds it, setting `region` and `homeState` as Firestore
  does. Before, it accepted any five digits.
- **Counts:** `model/Words.kt` `counted(n, noun)` replaces `Occasion`'s private
  `plural` and the inline copies in `Together`, `TodayCards` and `DecksScreen`.
  The other "pluralisations" the scan listed are not the same rule ("a deck",
  "One", "time."/"times." ending a sentence), so they stay as they are.
- **Validated:** full suite 282 tests, 27 skipped, 0 failures.

## Round 13: shared control in `components/`; release build (T17 rest)

- **`BrickChip`** was defined in `ui/quiz/DeckList.kt` but used by eight
  features (Today, Moods, Shape tomorrow, Shelf, Bucket list, Profile, Us, the
  tab bar). DESIGN_SYSTEM.md says a piece a second feature needs moves to
  `components/`; it is now `ui/components/BrickChip.kt`.
- **Kept in the UI on purpose (deletion test):** `DeckList.order` is display
  order with one caller and reads only model results; `finishedDetail` and
  `TodayCards.specialToday` are text built from model values. Moving them would
  relocate code without removing any.
- **Release:** `assembleRelease` with R8 and lint passes (needs
  `-Dorg.gradle.jvmargs=-Xmx6g`; with the default heap lint and R8 run out of
  memory, as noted in the project memory). The keep rule for `model.**` now
  also covers the files moved there in round 8.
- **Validated:** full suite 282 tests, 27 skipped, 0 failures.

## Round 14: last scan

- Re-scanned the scan agent's UI findings not yet handled. `glass` had a
  border colour branch that chose white either way; it is now one value
  (identical rendering). The two ink rules are recorded as P4: merging them
  changes a rendered colour, so it waits for a design decision.
- Kept on purpose: `createAuthRepository`/`createPairingRepository`/
  `createCoupleRepository` look shallow, but each is the single place that
  chooses fake vs Firebase; deleting them would spread that choice to every
  caller. `Push` writing the token field directly is the notify module's own
  job (the token is per device), now through `FirestoreSchema`.
- **Validated:** full suite 282 tests, 27 skipped, 0 failures.

## Round 15: the server side, and the findings still open after round 14

- **Scanned `functions/index.js` and `firestore.rules`.** Three app rules are
  necessarily restated in JavaScript: the mood labels (`MOOD_LABELS`), the
  anniversary names (`NAMED`) and the quiet-hours test (`isQuiet`). A single
  source would need code generation across two runtimes, so the app's copy is
  made the authority instead: `ServerContractTest` fails if the server's mood
  labels or anniversary names differ from `Mood` and `Together.Named`, or if
  the functions or rules touch a collection that is not in `FirestoreSchema`
  (apart from the server-only `outbox`, `chats`, `messages`). `isQuiet` is
  already unit-tested on the server (`exports._isQuiet`).
- **Placeholders:** the partner's placeholder name was "Partner" in pairing and
  "Them" in the couple repository's first frame; the placeholder colours were
  raw hex in four places. Now `Member.PARTNER_NAME`, `Member.placeholderYou`,
  `Member.placeholderPartner`, and `BrickColor.RED/BLUE` everywhere, including
  the theme defaults and the mapper's fallback.
- **P1 resolved by [ADR 0004](adr/0004-viewmodels-are-lifecycle-holders.md):**
  the ViewModels hold a repository across configuration changes and close it;
  the repository interface is the screens' contract. No forwarding methods added.
- **Considered and kept (deletion test fails), with the reason:**
  - *Sound + haptic pairs (47 sites).* They vary independently on purpose: a
    tap sound with a tick haptic, click/clack sounds with a place haptic, a
    badge that switches haptic by state, haptics that follow the sound
    setting. One `Cue` type would add a pair table without removing a
    decision. The one real inconsistency, the refusal sound, is P5.
  - *Two HTTP calls.* `NationalWeatherService.get` (anonymous GET, User-Agent
    required, 8 s, GeoJSON) and `deleteAccount` (authenticated callable POST,
    60 s read, user-facing error) share only `openConnection`/`disconnect`; a
    helper would take as many parameters as lines it saves.
  - *Push token write in `notify/Push`.* The token belongs to this device and
    rotates in `MessagingService.onNewToken`, where no repository exists. It is
    the notify module's own data, written through `FirestoreSchema`.
  - *Hand-pressed pills* (`ThemeBricks`, the bucket list's action pill, the
    Us settings pill). Each looks different from `BrickChip` (colour swap vs
    fade, one-shot action, icon content); folding them in changes the design.
  - *The `.clickable` sites outside `components/`* are the sanctioned
    `.panel().clickable { }` chain (DESIGN_SYSTEM.md) or gestures on brick
    art and canvases, not rows `ActionRow` would replace.
- **Validated:** full suite 286 tests, 27 skipped, 0 failures.

## Round 16: the owner's decisions on P3–P6

- **P4, one dark ink.** `ui/theme/Theme.kt` `DarkInk` (#15121F) is the only
  dark text colour; `Duo.inkOn` (blends) and `inkOnBrick` (bricks) both use it.
  `#0E1226` remains only as `NightBase`, the dark background, which it always
  also was. Text on pale blends shifts very slightly warmer.
- **P5, one refusal sound.** A refused move is `Sfx.DESELECT` + `Feel.REJECT`
  everywhere: `DragRank`'s cancelled drag changed from the place sound, and a
  brick dropped where it cannot go on the plate now makes the sound as well as
  the buzz (it was haptic only).
- **P3, deleted:** `model/Inbox.kt`, `model/Milestones.kt` and their tests.
- **P6, chat removed.** The `chats` rules are gone, so the old conversations
  are closed to everyone (Firestore denies what no rule allows; there is no
  catch-all). `rules-test/chat.test.mjs` became `photos.test.mjs`: its photo
  checks are unchanged, and its three chat checks became one "closed to
  everyone" check. **Kept on purpose:** `deleteAccount` still erases a
  person's old chat lines, because couples who used chat have them stored, and
  deleting an account must keep deleting that data. Both changes only reach
  production with the next `firebase deploy` of rules and functions.
- **P2, functions region:** `AppConfig.FUNCTIONS_REGION` is the one copy of
  `us-central1` (Firebase's default, since `functions/index.js` sets none);
  `ServerContractTest` fails if the functions start setting a region.
- **Validated:** unit suite 277 tests (286 − 10 deleted with `Inbox`/`Milestones`
  + 1 region check), 27 skipped, 0 failures. Every `rules-test` suite against the
  Firestore emulator, plus `delete.test.mjs` against the Firestore, Auth and
  Functions emulators: 107 checks, 0 failures, including "her message gone, his
  kept" (legacy chat erasure still works with the chat rules removed).
