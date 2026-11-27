# Optimization task pool

Ordered by value for the risk. Each task becomes one round in
[PROGRESS.md](PROGRESS.md) when picked. `P` = pending product decision.

## Open

| # | Area | Entropy | Plan |
|---|---|---|---|
| T1 | Rewards | Brick awards are computed in 8 places: 5 `awardFor*` functions in `FirestoreCoupleRepository` and 3 inline copies in `FakeCoupleRepository`, each with its own dedupe. `awardForToday` dedupes against the live plate only, while the others check every plate | One pure `Rewards.due(...)` in `model/`; both repositories call it |
| T2 | Plates | Placement legality and the weekly seal (carry unplaced bricks over) are written twice, and the fake lacks the "week has ended" check. `PeriodDays`/`emptyPlate` live in the Firestore file | `BuildPlate.place(...)`, `BuildPlate.next(...)`, `BuildPlate.empty(...)` in `model/` |
| T3 | Daily draw | `recompose()` builds ~15 draw inputs and calls `pool.daily(...)` twice with the same argument list | One draw-inputs value built once; one `daily` call |
| T4 | Firestore mapping | 250 lines of document↔model mapping sit inside the 1600-line repository; `runCatching { LocalDate.parse(it) }.getOrNull()` is repeated | Move the codec to its own file with one date-parse helper |
| T5 | Schema constants | `USERS`/`COUPLES`/`INVITES` live in `auth/` and `data/` imports them; other collection names are private to the repository | One Firestore schema file shared by `auth/` and `data/` |
| T6 | Listener errors | Nine copies of `if (error != null) { Log.w(...); return }`, and the user/couple/day listeners ignore errors entirely | One listen helper that logs and skips failures the same way everywhere |
| T7 | Repository lifecycle | `AmpersandViewModel` downcasts to `FirestoreCoupleRepository` to call `close()` | `close()` on the interface, a no-op in the fake |
| T8 | Config | `hasFirebaseConfigFor(context)` takes a context it never uses; three callers | One `hasFirebase` property |
| T9 | Fake fidelity | The fake validates a ZIP as "any five digits"; the real repository checks the bundled table | Use the pool's place table in both |

## Pending confirmation (product)

_None yet._

## Done

_See PROGRESS.md._
