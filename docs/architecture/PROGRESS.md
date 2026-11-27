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
