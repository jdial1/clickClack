# Optimization task pool

Ordered by value for the risk. Each task becomes one round in
[PROGRESS.md](PROGRESS.md) when picked. `P` = pending product decision.

## Open

| # | Area | Entropy | Plan |
|---|---|---|---|

## Pending confirmation (product)

| # | Question | Why it needs a decision |
|---|---|---|
| P1 | Should the in-app ViewModels stay as thin holders (`AmpersandViewModel` only holds a repo; screens call `vm.repo::method`)? | A real ViewModel layer would move navigation state out of `SignedInApp`, but that is a UX/architecture investment, not entropy removal. Left as is. |
| P3 | `model/Inbox.kt` and `model/Milestones.kt` are tested but nothing in the app uses them | They look like features that were built ahead of their screens. Deleting them is right if they are dropped; keeping them is right if they are planned. |
| P2 | `deleteAccount` calls `us-central1-<project>.cloudfunctions.net` with the region hard-coded | Correct today; moving functions to another region is a deployment decision. Noted, not changed. |

## Done

T1–T19 (rounds 1–13). T10 was folded into T5. Details in PROGRESS.md.
