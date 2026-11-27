# Optimization task pool

Ordered by value for the risk. Each task becomes one round in
[PROGRESS.md](PROGRESS.md) when picked. `P` = pending product decision.

## Open

| # | Area | Entropy | Plan |
|---|---|---|---|

## Pending confirmation (product)

| # | Question | Why it needs a decision |
|---|---|---|
| P3 | `model/Inbox.kt` and `model/Milestones.kt` are tested but nothing in the app uses them | They look like features that were built ahead of their screens. Deleting them is right if they are dropped; keeping them is right if they are planned. |
| P4 | Two "dark ink" colours: `Duo.inkOn` uses `#0E1226`, `inkOnBrick` uses `#15121F`. `DragRank` and `AnswerPanel` pick ink for a brick by luminance instead of `inkOnBrick(slug)`, which DESIGN_SYSTEM.md names for text on a brick | The two rules take different inputs (any colour vs a brick slug) and agree on which colours get dark ink, but choosing one dark ink changes the rendered text colour, which is a design call. |
| P5 | A refused move sounds different in two places: `AmpRadio`/`Forms` plays `Sfx.DESELECT` with `Feel.REJECT`, `DragRank` plays `Sfx.PLACE` with `Feel.REJECT` | Pick one refusal sound; it is an audible design choice. |
| P6 | `chats/{id}/messages` has rules, rules tests and server code (pushes, account deletion) but no client code | Either chat is coming (keep) or it was dropped (delete rules, tests and server code together). |
| P2 | `deleteAccount` calls `us-central1-<project>.cloudfunctions.net` with the region hard-coded | Correct today; moving functions to another region is a deployment decision. Noted, not changed. |

## Resolved by decision

- P1 (ViewModels): kept as lifecycle holders, [ADR 0004](adr/0004-viewmodels-are-lifecycle-holders.md).

## Done

T1–T19 (rounds 1–13). T10 was folded into T5. Details in PROGRESS.md.
