# Optimization task pool

Ordered by value for the risk. Each task becomes one round in
[PROGRESS.md](PROGRESS.md) when picked. `P` = pending product decision.

## Open

| # | Area | Entropy | Plan |
|---|---|---|---|
| T13 | Plurals | Seven hand-rolled pluralisations (`Occasion.plural` private, `Together`, `Reminder`, `TodayCards`, `DecksScreen`, `ResultChart`, `ShapeTomorrow.count`) | One public `plural` in `model/` |
| T17 | Rules in UI | `pickedBy` (ShapeTomorrow), day lookup across today/history/recalled (AmpersandApp), deck ordering (DeckList) computed in composables | Move each to the model type it describes (`Turns`, `CoupleState.dayOn`, deck rules) |
| T9 | Fake fidelity | The fake validates a ZIP as "any five digits"; the real repository checks the bundled table | Use the pool's place table in both |

## Pending confirmation (product)

| # | Question | Why it needs a decision |
|---|---|---|
| P1 | Should the in-app ViewModels stay as thin holders (`AmpersandViewModel` only holds a repo; screens call `vm.repo::method`)? | A real ViewModel layer would move navigation state out of `SignedInApp`, but that is a UX/architecture investment, not entropy removal. Left as is. |
| P3 | `model/Inbox.kt` and `model/Milestones.kt` are tested but nothing in the app uses them | They look like features that were built ahead of their screens. Deleting them is right if they are dropped; keeping them is right if they are planned. |
| P2 | `deleteAccount` calls `us-central1-<project>.cloudfunctions.net` with the region hard-coded | Correct today; moving functions to another region is a deployment decision. Noted, not changed. |

## Done

T1–T8, T11, T12, T14–T16, T18, T19 (rounds 1–11); T17 partly (round 10). Details in PROGRESS.md.
