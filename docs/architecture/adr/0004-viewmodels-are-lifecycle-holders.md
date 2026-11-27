# ADR 0004: The ViewModels are lifecycle holders; the repository is the interface

Status: accepted (round 15). Resolves P1.

## Context

`AmpersandViewModel`, `AuthViewModel` and `PairingViewModel` each hold one
repository, and screens call it directly (`vm.repo::submitToday`, 51 references
in `AmpersandApp`). The scan flagged them as shallow forwarders.

## Deletion test

Delete them and two things spread to the callers:

1. **Surviving configuration changes.** A rotation would rebuild the Firestore
   repository and re-attach its 19 listeners, briefly showing the placeholder state.
2. **Closing.** `onCleared` is the one place the couple repository is closed when
   its pairing or its day goes away (the ViewModel is keyed by couple and date).

So the layer does encapsulate something: lifecycle, not behaviour. What it does
not do, and should not, is restate the repository's 30 commands. Wrapping each in
a ViewModel method would be the meaningless forwarding layer the goal warns about.

## Options

1. Delete the ViewModels and manage lifetime with `remember` + `DisposableEffect`.
   This loses survival across configuration changes.
2. Give the ViewModel its own command methods that forward to the repository.
   That would be 30 pass-through methods.
3. **Keep each ViewModel as a holder of exactly one repository**, whose interface
   (`CoupleRepository`, `AuthRepository`, `PairingRepository`) is the screens'
   contract.

## Decision

Option 3. The public interface for couple data is `CoupleRepository`: one
`StateFlow<CoupleState>` and one command per user intent. The ViewModels'
only job is when that object is created and closed.

Navigation state stays in `SignedInApp` as saveable vars collapsed into one
`Screen` value. Moving it into a ViewModel is a separate design change with its
own trade-offs (process death, deep links), not entropy removal.
