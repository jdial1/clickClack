# Ampersand

A daily question for two people. You each answer privately; neither answer is
readable until both are in. Then it unseals, and you talk.

Scaffold stage: the UI is complete and driven by an in-memory mock repository.
There is no backend yet.

## Running it

Open the folder in Android Studio and run the `app` configuration, or:

```bash
./gradlew assembleDebug
```

Every screen also has `@Preview` composables, so the whole UI renders in the
Studio preview pane without an emulator.

To check it on a device instead — the `Pixel_9` AVD works:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.ampersand/.MainActivity
```

On a cold-booted emulator, SystemUI can throw an "isn't responding" dialog for
the first half-minute and swallow input. Dismiss it and give it twenty seconds
before driving the UI.

### Scaffold controls

There is an unlabelled 28dp tap target in the **top-right corner** of any screen.
Tapping it opens a panel that fakes the things that normally need a second
person and a second phone:

- **Partner answers** — fakes Maya submitting, so the seal lifts and the reveal
  becomes reachable.
- **Reset day** — back to an unanswered question.
- **Reflect / Guess Me / This or That** — swaps today's question so all three
  input styles can be designed quickly.

This panel and `FakeCoupleRepository` both come out when Firestore lands.

## The one mechanic

Sealed until both answer. It stops you anchoring on your partner's answer, and
it is the reason the app produces conversation rather than a feed.

`DayEntry.isRevealed` is simply "two answers present". In the Firestore model
this is not a UI convention — the partner's answer is genuinely unreadable until
rules allow it.

## Question types

| Type | Control | Scored on |
|---|---|---|
| `REFLECT` | Text field | Nothing — deliberately |
| `GUESS_ME` | Text field ×2 (yours + prediction) | Exact-ish match |
| `THIS_OR_THAT` | Radio, 2 options | The pick |
| `PICK_ONE` | Radio, 3+ options | The pick |
| `MULTI_SELECT` | Checkboxes, optional cap | Set equality |
| `RANK` | Drag to order | The top choice only |
| `SCALE` | Five pips, 1–5 | The number |
| `DROPDOWN` | Menu, long option lists | The pick |

Every scoreable type asks for two things: your own answer, and a prediction of
your partner's. That is where the score comes from, and it is also what makes
the reveal worth opening.

`RANK` scores on the top choice alone. Exact-order matching on four items is
almost never achieved, and a scoreboard nobody can move is a scoreboard nobody
looks at — "did you know what matters most to them" is the interesting question
anyway.

Every question carries a `Depth` of 1–3 so the pool can keep Monday light and
avoid landing a heavy one at the wrong moment.

## The form kit

The app is mostly forms, so the controls are the product. They live in
`ui/components/` and are drawn rather than taken from Material defaults.
Indicators animate on a spring; nothing snaps.

| Composable | Notes |
|---|---|
| `AmpField` | Filled, borderless at rest, accent ring arriving on focus. No floating label. |
| `AmpSegmented` | Pill tab strip: a tray, with the selected tab filled solid inside it. |
| `AmpRadioGroup` | Whole row is the target and fills with the accent when picked. Dot drawn on a `Canvas`. |
| `AmpCheckboxGroup` | Same row anatomy as the radio, so they read as one family. Optional `max`, which disables the unpicked rows rather than hiding them. The tick is drawn in two strokes in proportion, so it appears to be written. |
| `AmpDropdown` | Plain `DropdownMenu` with our own anchor, not `ExposedDropdownMenuBox` — the anchor then matches the other controls exactly. |
| `AmpScale` | Five tappable pips. A slider would imply precision the answer does not have, and is fiddly one-handed. |
| `AmpButton` | Filled primary action carrying its own glow. |
| `ColorChip` | The rounded-square colour tile the reference designs put on every list row. |
| `CurveBackdrop` | Bezier curves sweeping from off the bottom-left up and off the right, in the couple's own colours. Geometry is in fractions of the canvas so it fits any screen, strokes start and end outside the bounds so none has a visible end, and weights are mixed — equal-weight lines read as a graph, mixed weights read as movement. |
| `AmpDragRank` | Drag to reorder. See below. |

Open the **Kit** tab in the running app to see all of them live, or the
`Gallery` previews in Android Studio.

### AmpDragRank

Compose has no built-in reorderable list outside lazy lists, so this is
hand-rolled. Three things it gets right that the obvious version does not:

- Rows are wrapped in `key(item)`, so a row keeps its identity across a
  reorder. Without it Compose reuses slots positionally and every row visually
  teleports.
- The gesture is keyed on the **item**, not the list. Keying `pointerInput` on
  the list restarts it the instant the order changes, cancelling the drag on the
  first swap. This is the single most common bug in hand-rolled reorderables.
- The live list is read through `rememberUpdatedState`, so the gesture resolves
  the item's *current* index rather than the one captured at drag start.

Everything except the row under the finger animates its placement via a small
`animatePlacement` modifier, so the list reflows instead of snapping.

## The build

Answering earns bricks. Both of you drop them on one shared baseplate, and at
the end of the period the sculpture is archived and a fresh plate starts. The
streak stops being a number and becomes a thing you made together.

Bricks keep their owner's colour, so the finished piece shows who built which
bit. That is most of the charm, and the reason this beats a progress bar.

### Geometry follows the art

Bricks are the [Kenney Brick Pack](https://kenney.nl) (CC0). The grid is derived
from the pack's own pixel sizes rather than invented: a stud is 64px, the
shortest brick body is 28px, and the nub is 16px and overhangs the body. So a
**grid cell is 64 × 28** — wider than tall, like a real brick — heights come in
1, 2 and 4 rows (`low` / `medium` / `high`), and widths in 1, 2, 4 and 6 studs.
Slopes exist at medium and high, 2 and 4 studs wide.

Picking a square grid and stretching the art onto it would have been the obvious
move, and it would look wrong.

Bricks are drawn from the **greyscale** (`White`) set and tinted with
`BlendMode.Modulate`, which multiplies: the `#FFFFFF` highlight becomes the
owner's colour and the `#BFBFBF` shadow becomes a darker version of it. That
keeps the pack's shading while letting every couple build in their own two
colours. A flat `SrcIn` tint would flatten the bricks; shipping the pack's six
fixed colours would throw the personal palette away. Character bricks carry
their own art and are drawn untinted.

**Earning** is deliberately modest — one brick each per completed day, sized by
the question's depth (2-stud low / 4-stud low / 4-stud medium), with a character
brick at every seventh day of streak. Paying out more for a better score would
turn the plate into a scoreboard; the point is that it records turning up
together, not winning.

**Placing** is drag-from-tray with three rules in `BuildPlate.canPlace`: inside
the grid, not overlapping, and resting on the baseplate or on at least one stud
of something already placed. That last rule is what makes it read as *built*
rather than as stickers on a wall.

Dropping is forgiving on purpose: if the exact row under your finger is illegal,
the brick falls down that column to the first legal row (`dropRow`).
Pixel-accurate placement on a phone is not a fair ask.

Validity is checked in the **repository**, not the UI, so the Firestore version
can run the identical check inside a transaction and reject a placement race the
same way. Awarding is idempotent — block ids are `date_uid` — because the same
completed day will arrive more than once from a listener.

The whole plate paints in **one** `Canvas` via `drawBrick`, with bricks sorted
bottom-row-first so each nub is overlapped by whatever sits on it. A composable
per brick is the obvious build and the slow one.

`BrickAtlas.kt` maps asset name → drawable id explicitly rather than via
`getIdentifier()`, so a missing or renamed asset is a compile error instead of a
blank brick at runtime. It is generated from the pack file list — regenerate it
if the pack changes.

## Brick styling for UI chrome

The sculpture uses the real pack art, but that art only comes in 1, 2, 4 and 6
stud widths — it cannot stretch to a text field or a nav bar without looking
mangled. So UI chrome is **drawn** in the same language instead, via
`Modifier.brickFace(...)`: a body, a row of studs along the top, and a darker lip
underneath that shortens when pressed, the way a key goes down.

One rule keeps it coherent:

> **Things you press are bricks.** Studs on top, lip underneath, lip shortens on
> press or selection.
> **Things you type into are sockets.** Recessed, studless — studs behind a
> cursor is just clutter.

Studs sit *inside* the bounds rather than overhanging. Overhang is what real
bricks do, but `drawBehind` clips to bounds and a clipped stud reads as a bug.
Stud count is derived from the element's width, so a nav tab gets one and the
hero gets nine, with no per-call configuration.

## Sound

Kenney's UI audio pack, via `SoundPool` — short, overlapping, off the UI thread.
`Sfx` maps **meaning** to file (`TAP`, `NAV`, `SELECT`, `DESELECT`, `FOCUS`,
`PICK_UP`, `PLACE`, `PRESS`, `RELEASE`, `SEAL`), so swapping a sound is a
one-line change. That matters: the clips were picked from the pack by filename,
not by ear, so expect to retune them.

Ten clips, 88 KB. Sounds are decoded up front and a play before the pool has
finished loading is **dropped rather than queued** — for UI chirps, a click that
arrives late is worse than one that never arrives.

`LocalSounds` defaults to null so previews and tests never touch the audio
system, and a `SoundKit?.play()` extension means call sites need no null checks.
The toggle lives on the **Us** screen.

## Credits

Brick art and UI audio: [Kenney](https://kenney.nl) — Brick Pack 2.0 and the
interface sound pack, both CC0. Credit is not required by the licence; it is
here because the packs are good.

## Layout

```
model/Models.kt          Question, Answer, DayEntry, CoupleState. No Android imports.
data/MockData.kt         16 sample questions + a couple with ten days of history.
data/CoupleRepository.kt The seam Firebase slots into. Fake implementation lives here.
ui/theme/Theme.kt        The blended two-person palette.
ui/today/                Question, input, seal, waiting.
ui/reveal/               Both answers, each in its owner's colour, with hit/miss.
ui/history/              Ten weeks as dots, plus the answered list.
ui/us/                   Streak, sync score, live colour pickers.
```

`model/` is plain Kotlin on purpose — when the Firestore layer arrives it maps
documents to these types and nothing in `ui/` changes.

## Visual language

Deep navy ground, filled surfaces, big radii, one vivid accent, and depth from a
soft **coloured** glow rather than outlines or grey drop shadows.

| Token | Value |
|---|---|
| Corner | chip 14 · control 20 · card 28 · hero 32 · pill ∞ |
| Depth | `Modifier.glow(color, shape)` — a wide shadow tinted with the element's own hue |
| Surfaces | filled and layered; **no** hairline borders at rest |
| Selection | the accent takes the whole control, not a 12% tint of it |
| Type | geometric sans, heavy where it matters; size and weight carry hierarchy |

Coloured shadows need API 28. Below that `glow` degrades to a standard black
shadow, which still reads fine.

### The blend

Each person picks a colour. Their surfaces lean their hue, and the **hero
question card is painted in the gradient between the two** — your colour on one
edge, theirs on the other, the question sitting in the blend. That one surface
says what the app is about without words, and no two couples get the same one.

The `ColorScheme` is built by hand rather than seeded from a tonal palette, so
the blend stays the literal average of the two chosen colours. Change them on
the **Us** screen and the whole app repaints live, hero gradient included.

Default seeds are coral `#FF6B5B` and indigo `#6C63FF`; their blend is an
orchid. The swatches are saturated on purpose — the entire palette derives from
them, and muted seeds make a muted app.

## What is not built yet

- Firebase Auth, Firestore, FCM (see below)
- Pairing by invite code
- Notifications
- Wildcard (one of you writes the question) and time capsule (questions return
  in six months, then vs. now)
- Post-reveal notes and reactions — the Reveal screen has the slot, not the
  behaviour

## Planned Firestore model

```
users/{uid}                      displayName, colorArgb, coupleId, tz, fcmToken
couples/{cid}                    members[2], homeTz, streak, freezes, queue[], lastDay
couples/{cid}/days/{yyyy-mm-dd}  questionId, type, submitted[], revealed, score
  .../answers/{uid}              response, guess, submittedAt      <- the sealed part
  .../notes/{noteId}             uid, text, createdAt              <- post-reveal only
questions/{qid}                  text, type, options[], category, depth, source
```

Answers **must** be one document per person. Firestore rules are per-document
and cannot hide a field inside a shared one, so a single day document holding
both answers would be readable by both from the moment the first one writes.

```js
match /couples/{cid}/days/{d}/answers/{uid} {
  allow read: if isMember(cid) &&
    (uid == request.auth.uid ||
     get(/databases/$(database)/documents/couples/$(cid)/days/$(d)).data.revealed == true);
  allow create: if uid == request.auth.uid && isMember(cid);
  allow update, delete: if false;   // no edits after submit — that is the point
}
```

Day boundaries follow the couple's `homeTz`, never device time. That is the
difference between working and not working if you are ever in different
timezones, and it is far cheaper to decide now than to migrate later.

## Toolchain

Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.10, Compose BOM 2026.08.00, compileSdk 37,
minSdk 26 (`java.time` without desugaring). Bytecode targets 17; the build JVM
is Temurin 25.

This host needs `-Djdk.net.unixdomain.tmpdir` pointed away from `%LOCALAPPDATA%\Temp`
or the Gradle daemon cannot accept a connection. It is set in
`~/.gradle/gradle.properties`, which is why this project deliberately does **not**
define `org.gradle.jvmargs` — a project-level value would override it and break
the build on this machine. Command-line builds also want it in `GRADLE_OPTS`.
