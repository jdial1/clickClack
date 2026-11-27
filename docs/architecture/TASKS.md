# Optimization task pool

Ordered by value for the risk. Each task becomes one round in
[PROGRESS.md](PROGRESS.md) when picked. `P` = pending product decision.

## Open

| # | Area | Entropy | Plan |
|---|---|---|---|
| T6 | Listener errors | Nine copies of `if (error != null) { Log.w(...); return }`, and the user/couple/day listeners ignore errors entirely | One listen helper that logs and skips failures the same way everywhere |
| T9 | Fake fidelity | The fake validates a ZIP as "any five digits"; the real repository checks the bundled table | Use the pool's place table in both |

## Pending confirmation (product)

_None yet._

## Done

T1–T5, T7, T8 (rounds 1–6). Details in PROGRESS.md.
