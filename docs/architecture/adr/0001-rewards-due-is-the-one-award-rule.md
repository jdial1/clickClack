# ADR 0001: `Rewards.due` is the one place that decides which bricks are owed

Status: accepted (round 1)

## Context

Bricks are earned for a revealed day, a talked-about day, a finished deck, a
ticked bucket-list item and a finished mood day. The rule "what is owed and not
yet laid" was written eight times:

- `FirestoreCoupleRepository`: `awardForToday`, `awardForQuizzes`,
  `awardForTalks`, `awardForBucket`, `awardForMoods`, each computing its own
  "already laid" set and its own write.
- `FakeCoupleRepository`: inline copies in `submitToday`, `tickBucket` and
  `markTalkedAbout`, which award at the moment of the action instead of from
  state, never award moods or decks, and award a talk on the same day where the
  real one waits for the morning after.

The copies had drifted: `awardForToday` checked only the live plate for an
existing brick, while the others checked every plate.

## Options

1. **Leave the copies, align them by hand.** Lowest effort, but the next reward
   type is again two or three edits.
2. **A shared helper in `data/`** used by both repositories. Removes the copies,
   but keeps a business rule in the data layer, untestable without it.
3. **One pure function in `model/`:** `Rewards.due(state, today, decks, plates)`
   returns every brick earned and not on any plate. Each repository decides only
   *when* to ask and *how* to write.

## Decision

Option 3. The function is pure, so it is unit tested on the JVM, and it is
idempotent (block ids are per-person keys), so asking on every state change is
safe. Firestore writes the result with one `arrayUnion`; the fake appends it.

Every award checks every known plate, including the live one, before laying a
brick. The fake now awards on the same schedule as Firestore: a talk is laid the
morning after, and moods and decks earn bricks in the fake too.

## Consequences

- A new reward type is one branch in `Rewards.due` and one test.
- The fake's preview data shows the same bricks a real couple would see.
- The repository no longer knows any reward rule.
