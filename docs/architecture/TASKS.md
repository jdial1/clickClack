# Optimization task pool

Ordered by value for the risk. Each task becomes one round in
[PROGRESS.md](PROGRESS.md) when picked. `P` = pending product decision.

## Open

| # | Area | Entropy | Plan |
|---|---|---|---|

## Pending confirmation (product)

_None._

## Resolved by decision

- P1 (ViewModels): kept as lifecycle holders, [ADR 0004](adr/0004-viewmodels-are-lifecycle-holders.md).
- P2 (functions region): named once as `AppConfig.FUNCTIONS_REGION`, pinned to the functions by `ServerContractTest`.
- P3 (unused `Inbox`, `Milestones`): deleted with their tests (owner, 2026-11-27).
- P4 (dark ink): one `DarkInk` (#15121F) in `ui/theme` for bricks and blends alike (owner).
- P5 (refusal sound): every refusal plays `Sfx.DESELECT` with `Feel.REJECT` (owner).
- P6 (chat): removed (owner). See PROGRESS round 16 for what stays and why.

## Done

T1–T19 (rounds 1–13). T10 was folded into T5. Details in PROGRESS.md.
