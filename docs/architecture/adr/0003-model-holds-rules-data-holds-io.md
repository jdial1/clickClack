# ADR 0003: `model/` holds every rule, `data/` only loads and stores

Status: accepted (round 8)

## Context

README.md says `model/` is "plain Kotlin on purpose", but four files of pure
rules lived in `data/`: `BadgeWall` (instrument badges), `CoupleReadings`,
`Profiles` (taste profiles for the draw) and `Instruments`/`Sources` (published
scales and their scoring keys). `QuestionPool.kt` also ended with two draw rules,
`firstWeekDay` and `gentleReturn`, beside its asset loading, and one of them had
the other's KDoc attached. Screens imported these from `data/`, so "what does
the UI depend on in `data/`" had no short answer.

## Options

1. Leave them where they are and document the exception.
2. A third package, e.g. `rules/`. That's a new layer that would sit beside
   `model/` with exactly the same constraints.
3. **Move them into `model/`.** They have no Android, Firebase or asset
   dependency, and they only reference `model/` types and each other.

## Decision

Option 3. `data/` keeps `QuestionPool`/`QuizJson` (asset loading), `HomeZone`
(SharedPreferences), `NationalWeatherService` (HTTP), the repositories, the
Firestore codec and `MockData`. The draw rules are in `model/DrawRules.kt` with
their KDoc fixed. Their tests moved with them to `test/.../model/`.

## Consequences

- Anything in `model/` can be unit tested without Android. The moved tests did
  not change.
- `ui/` imports from `data/` only for the repository, the pool and preview fixtures.
