# Click Clack

A daily question for two people. You each answer privately; neither answer is
readable until both are in. Then it unseals, and you talk.

Answering earns bricks. You both drop them on one shared plate, and at the end
of the week the sculpture is sealed with a date and a fresh plate starts.

Scaffold stage: the UI is complete and driven by an in-memory mock repository.
There is no backend yet.

See [DESIGN.md](DESIGN.md) for what the app is *for* — the name, the voice, the
rules that settle arguments. This file is how it is built.

> **The code still says Ampersand.** That was the name until the rename, and the
> package is `com.ampersand`, the theme composable is `AmpersandTheme` and every
> control carries an `Amp` prefix. Renaming those touches every file in the
> project and changes the `applicationId`, which makes the new build refuse to
> install over the old one — so it has deliberately not been done. Only the
> things a person actually sees were renamed: the launcher label, the app theme,
> and the waiting state.

## Running it

Open the folder in Android Studio and run the `app` configuration, or:

```bash
./gradlew assembleDebug
```

Every screen also has `@Preview` composables, so the whole UI renders in the
Studio preview pane without an emulator.

### The emulator

Start the `Pixel_9` AVD with **ANGLE**, not software rendering:

```bash
emulator -avd Pixel_9 -gpu angle_indirect -no-boot-anim -memory 4096
```

`-gpu swiftshader_indirect` was the previous advice, on the grounds that
hardware GL corrupted the framebuffer. It is far worse: roughly 30s cold starts,
near-constant SystemUI ANRs, and `system_server` dying outright twice in one
session, each time taking the installed APK with it. ANGLE goes through D3D11
and is usable.

Expect one "System UI isn't responding" dialog about a minute after a cold boot.
Give it 45 seconds and tap **Wait**; it settles and does not come back. If a
screenshot comes back black, or `dumpsys window | grep mCurrentFocus` reports
`null`, it is wedged — kill it and cold-boot rather than retrying the install.

### Screenshot testing does not work here yet

AGP's `com.android.compose.screenshot` plugin renders `@Preview` composables to
PNG on the JVM, which would replace the emulator entirely. It does not work
against this toolchain, and the failure is silent, so here is how far it gets
before you waste an afternoon on it:

- The plugin is **not** versioned with AGP. Only `0.0.1-alphaNN` is published;
  `version.ref = "agp"` fails to resolve.
- `android.experimental.enableScreenshotTest=true` is needed in **both**
  `gradle.properties` (so the plugin can apply) and the module's `android` block
  as `experimentalProperties[...]` (so the source set is created). Each error
  message names only the other one.
- The plugin does not add its own JUnit engine under AGP 9 —
  `debugScreenshotTestRuntimeClasspath` comes back completely empty. Adding
  `com.android.tools.screenshot:screenshot-validation-junit-engine` manually
  fixes the classpath.
- After all of that the engine loads and discovers **zero** previews, whether
  they are top-level functions or members of a class, and reports nothing at
  all. Alpha16 predates AGP 9's built-in Kotlin compiler, which writes classes
  to `intermediates/built_in_kotlinc/...`; the discovery step appears not to
  cope.

Until that plugin catches up, the Studio preview pane is the verification tool.
Every screen has `@Preview`s, including the sealed-and-waiting state and the
reveal with both reactions set.

If you do want it on a device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.ampersand/.MainActivity
```

### Scaffold controls

There is an unlabelled 28dp tap target in the **top-right corner** of any screen.
Tapping it opens a panel that fakes the things that normally need a second
person, a second phone, or a week of waiting:

- **Partner answers** — fakes Maya submitting, so the seal lifts.
- **Partner reacts** — toggles a reaction from her on today, so both halves of
  the reveal can be seen.
- **Seal the week** — archives the current plate onto the shelf and starts a
  fresh one. Otherwise this is a thing that happens once every seven days and is
  impossible to design.
- **Reset day**, **Form kit**, and a question-type switcher.

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
| `SCALE` | Draggable, 1–6 studs | The number |
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

## Reactions

Once a day unseals, you each get **one** reaction — six to choose from, mapped
meaning-first in `Reaction`:

```kotlin
LOVE, LAUGH, MELT, SURPRISE, CURIOUS, KNEW_IT
```

The enum stores the glyph *and* the sentence it stands in for, the same way
`Sfx` maps meaning to file. Changing an emoji later does not change what a
stored reaction meant, and the partner's reaction is displayed with its label
rather than the glyph alone — an emoji on its own is ambiguous in exactly the
way this app is trying not to be.

Reactions live on the day document, not in the sealed per-person subcollection:
there is nothing left to spoil after the reveal, so no rule has to hide them.
They are also the one thing in the app you *can* change your mind about — an
answer is a statement made under seal, a reaction is a gesture. Tapping your
current one clears it.

The reveal ends with the only instruction the app ever gives: *Now go and ask
them about it.*

## Google sign-in

The code is in and compiles; the Firebase project does not exist yet.

`AuthRepository` is the seam, shaped like `CoupleRepository` and for the same
reason. `createAuthRepository()` returns `GoogleAuthRepository` when a Firebase
config is present and `FakeAuthRepository` when it is not, detected by the
presence of the generated `google_app_id` string. So the app builds, runs and
is designed with no Firebase account in existence, and starts doing real auth
the moment `google-services.json` lands in `app/`.

The google-services plugin is applied **conditionally** in `app/build.gradle.kts`
for the same reason: it hard-fails a build that has no config file, and this
repo deliberately does not check one in.

Without the file, `FirebaseInitProvider` logs one warning at startup and skips
initialisation. That is expected and harmless — nothing touches `FirebaseAuth`
on the fake path.

### The flow, and the hop people miss

Credential Manager, not the deprecated `GoogleSignIn` API. Two hops:

1. Credential Manager returns a **Google ID token** — proof of who they are to
   Google.
2. That is exchanged with Firebase for a **Firebase session** — which is what
   Firestore security rules actually check.

Skip the second and you have a signed-in user the database has never heard of.

`webClientId` is the OAuth **web** client id, not the Android one. Using the
Android client here fails with a token-audience error that says nothing useful.
It is read by name at runtime rather than through `R.string`, because the
resource only exists once the plugin has run — the one place `getIdentifier`
earns its keep.

### The project

| | |
|---|---|
| Project | `clickclack-002827` ("Click Clack") |
| Android app | `1:522013714152:android:66c18f123b33d8de53cb5d` |
| Package | `com.ampersand` |
| Debug SHA-1 | `6A:8C:7C:40:98:31:DC:8C:76:2F:72:65:C3:9E:53:58:E3:C3:5B:AC` |

`clickclack` alone was taken — project ids are global across all of Google
Cloud — hence the suffix, which is the same convention as the other projects on
this account.

The SHA-1 is this machine's `~/.android/debug.keystore`, valid to 2056, and is
already registered. A release build is signed with a different key and will
need its own added.

`google-services.json` is **gitignored**: it is per-project and regenerated in
one command.

```bash
firebase apps:sdkconfig android 1:522013714152:android:66c18f123b33d8de53cb5d   --project clickclack-002827 --out app/google-services.json
```

Google **is** enabled as a sign-in provider, so the config carries two OAuth
clients — an Android one bound to the SHA-1 above, and the web one that becomes
`default_web_client_id`. Enabling it is console-only; there is no `firebase`
command, and until it was done the config had an empty `oauth_client` array and
no web client id was generated.

To go back to the fake auth path while designing a screen, **delete
`app/google-services.json`**. Detection is automatic, so removing the file is
the whole of it.

### Verified on device

Up to the account picker: the app opens Google's sheet, correctly branded
("to continue to Click Clack", with the launcher icon), and dismissing it logs
`Sign-in dismissed` and returns to the sign-in screen with no error shown —
which is the intended handling of `GetCredentialCancellationException`.

The final hop is unverified: the emulator has no Google account attached, so
the picker only offers "Add another account". Completing sign-in needs real
credentials and therefore a person. What that will exercise for the first time
is `signInToFirebase` — the ID-token-for-session exchange — and the first write
of a user to Firebase Auth.

## Pairing

Two signed-in people become a couple by one of them making a code and the other
typing it.

```
users/{uid}      displayName, coupleId, inviteCode
invites/{code}   fromUid, fromName, createdAt
couples/{cid}    members[2], createdAt
```

Codes are **8 characters from a 32-symbol alphabet with no I, O, 0 or 1** —
the pairs that get misheard and mistyped, and a code is worthless if it cannot
survive being read down a phone. That is 2^40, which matters because the rules
deliberately let any signed-in user `get` an invite by id: the code is the
secret, so it has to be one. `get` is allowed and `list` never is, or the whole
collection of outstanding invites would be readable.

Redeeming is **one transaction**: read the invite, create the couple, point both
user documents at it, delete the invite. Two people tapping at the same moment
outside a transaction is how you get two couples each holding one person.

The local user document is **watched**, not fetched. That is what makes pairing
land on both phones — the person who made the code never touches their own
document again, so without a listener their app would sit on the invite screen
until it was restarted.

`FakePairingRepository` starts **paired**, matching the mock couple, so the fake
path is still the whole product. The scaffold panel has an **Unpair** chip for
reaching the pairing screen deliberately.

### Binding the accounts to the couple

`bindLocalUser` / `bindPartner` rewrite a member *and every reference to them*:
uids are map keys on every answer and reaction, `ownerUid` on every brick, and
embedded in block ids of the form `date_uid`. Renaming the member alone is the
obvious version and it silently guts ten weeks of history — the answers end up
belonging to a user who no longer exists and the plate shows your bricks as
your partner's.

None of this is needed once Firestore owns the couple; it exists because the
sample couple was invented before anyone had signed in.

Codes **expire after an hour**, and are consumed on a valid match. An hour is
the window for "read this out to the person next to you"; a code with no expiry
sits in a message thread and still works months later. The ceiling is enforced
in the rules too (`expiresAt` must be a timestamp inside two hours), so a client
cannot mint a long-lived one. Redeeming an expired code deletes it — the only
thing a dead invite can do is confuse whoever types it next.

**Unpairing ends it for both of you.** `endedAt` is written on the *couple*,
which both members may update, rather than clearing your own `coupleId`, which
only you may write. That is the whole fix: the partner's listener sees the
shared document change and their app leaves too. The couple document is kept
rather than deleted — it is the parent of every answer and sculpture you made,
and deleting a parent in Firestore orphans subcollections rather than removing
them.

## The real repository

`FirestoreCoupleRepository` backs everything a person creates: answers,
reactions, colours, bricks and sealed sculptures, all keyed to their account.
Two phones on two profiles see each other's contributions, which is what makes
any of this testable.

**Questions are still mock.** The pool arrives as JSON, so today's question is
picked out of `MockQuestions` *by date*. Picking rather than storing matters
more than it looks: both phones must land on the same question before either
has written anything, or whoever opens the app second gets a different one.

Four listeners — both user documents, today, the last ten weeks, the plates —
all feeding one `recompose()`. A `CoupleState` is a whole picture, and updating
it field by field from four callbacks arriving in any order is how you get a
screen that is briefly wrong.

Two details worth keeping:

- **The answers listener fails before the reveal, on purpose.** The rules deny
  the collection query rather than filtering it, so the error path falls back to
  reading only your own answer. The seal is enforced by the database; the app
  never has to hide anything.
- **`revealed` can only become true once both answer documents exist.** The rule
  checks with `exists()` on both, so a client cannot flip it early to peek.
  Properly this belongs in a Cloud Function — the rule is what makes the client
  version honest.

The scaffold controls that fake your partner are **no-ops against Firestore**.
There is no honest way to answer as someone else once the rules only let you
write your own documents; use a second account.

### Still to do

- No Cloud Function. Sealing a plate at the end of a week, and lifting the seal,
  both depend on a client being open.
- History rows carry no answers — they live in a subcollection that would need a
  read each. The Shelf shows questions, dates and reactions; the Reveal screen
  loads a single day in full.
- Streaks are computed on the client from the last ten weeks, so a couple older
  than that undercounts.

## The build

Bricks are earned by answering and placed by hand on a shared plate. Bricks keep
their owner's colour, so the finished piece shows who built which bit. That is
most of the charm, and the reason this beats a progress bar.

### Sculptures are dated; bricks are not

A `BuildPlate` carries `completedOn`. Null means still building; a date means
sealed, and a sealed plate is read-only — no tray, no placing, no awarding.

The date belongs to the finished thing, not to the bricks in it. A brick that
remembered its own day would turn the sculpture into a scrollable diary and the
plate into an index; what you actually want to be able to say is *"that is the
one we made the week of the ninth"*.

When a plate seals, **unplaced bricks carry over** to the next one. They were
earned by answering, and confiscating them would punish you for not having
tidied the tray before a deadline you did not choose.

### Geometry follows the art

Bricks are the [Kenney Brick Pack](https://kenney.nl) (CC0). The grid is derived
from the pack's own pixel sizes rather than invented: a stud is 64px, the
shortest brick body is 28px, and the nub is 16px and overhangs the body. So a
**grid cell is 64 × 28** — wider than tall, like a real brick — heights come in
1, 2 and 4 rows (`low` / `medium` / `high`), and widths in 1, 2, 4 and 6 studs.
Slopes exist at medium and high, 2 and 4 studs wide.

Picking a square grid and stretching the art onto it would have been the obvious
move, and it would look wrong.

The art ships in the pack's own six colours, so bricks are drawn **untinted**.
An earlier version multiplied a greyscale source with `BlendMode.Modulate`,
which cannot reach the pack's saturated reds without clipping and left the
bricks slightly off from the rest of the UI. Character bricks carry their own
art and ignore the owner's colour entirely.

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
if the pack changes. Bitmaps are decoded **lazily, per name**: all 173 at once is
roughly 14MB of bitmap.

## The Shelf

The memory screen, ordered by feeling rather than chronology:

1. **On this day** — an entry from six months or a year back, with both answers
   shown in full. `CoupleState.onThisDay` matches on day-of-month rather than on
   exact anniversaries, so there is something to find more than once a year, and
   excludes anything inside the last month: "three weeks ago today" is not a
   memory, it is just recent.
2. **The shelf** — sealed sculptures, the selected one large, the rest as a
   strip. `Sculpture` crops to `BuildPlate.builtHeight()` rather than to the
   grid; drawn to the full 14 rows every week's work would be two thirds empty
   sky and they would all look identical.
3. **The last ten weeks** — 70 one-stud bricks, one per day, in one `Canvas`
   with hit-testing in the same coordinates rather than seventy clickable boxes.
4. **Every day** — the index, which is the least interesting part and is
   therefore last.

It is the **Shelf** tab, not History. A shelf is where finished things live; a
history is a log, and a log is what that screen used to be. It is also the only
label that fits a 2-stud nav tile without shrinking the type.

## The form kit

The app is mostly forms, so the controls are the product. They live in
`ui/components/` and are drawn rather than taken from Material defaults.

| Composable | Notes |
|---|---|
| `AmpField` | A recessed socket. The one control that is deliberately *not* a brick. |
| `AmpSegmented` | Pill tab strip: a tray, with the selected tab filled solid inside it. |
| `AmpRadioGroup` | A 1-stud brick in each colour, one per option. |
| `AmpCheckboxGroup` | Same row anatomy as the radio, so they read as one family. Optional `max`, which disables the unpicked rows rather than hiding them. |
| `AmpDropdown` | Plain `DropdownMenu` with our own anchor, not `ExposedDropdownMenuBox` — the anchor then matches the other controls exactly. |
| `AmpScale` | Drag a run of studs from 1 to 6. A slider would imply precision the answer does not have. |
| `AmpButton` | A 12-stud `BrickSlab` in a colour slug. Disabled is a black brick at full opacity, not a 35%-alpha ghost. |
| `AmpDragRank` | Six-stud bricks that reorder and snap. See below. |
| `AmpReactionRow` | Six 2-stud bricks, one per `Reaction`, twelve studs in total. |
| `ColorChip` | A real 2-stud brick from the pack. |
| `BrickWall` | The background: a running-bond wall in both your colours, with gaps. |

Open the **Form kit** from the scaffold panel to see them live, or the `Gallery`
previews in Android Studio.

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

The same class of bug bit twice more: once in the build tray (fixed with
`key(block.id)`) and once in navigation, where holding the open `DayEntry`
instead of its date meant a reaction added on the reveal screen never appeared
on the reveal screen. If something captured at the start of an interaction is
read at the end of it, check it.

## Brick styling for UI chrome

Pack art only comes in 1, 2, 4 and 6 stud widths, so a full-width surface cannot
be a single image. It is laid as a **course of real bricks** instead:
`BrickSlab` decomposes the width via `BrickArt.runOf` (12 studs → a 6, a 4 and a
2) and reverses alternate courses so seams stagger like real bond rather than
stacking into a visible column.

Height comes from the **content**, not from a course count, and the course count
is computed against `height − nub`: a course's studs sit *above* its body, so
without that subtraction the top row draws outside the slab and overlaps
whatever is laid out above it.

`inkOnBrick(slug)` picks the text colour. The neutral brick is `#DBDBDB` and
yellow is `#FFB733`; white text on either is unreadable, which is exactly how
the first version of the stat cards shipped.

Anything too small or too odd-shaped for real art is drawn in the same language
via `Modifier.brickFace(...)`: a body, studs along the top, and a darker lip
underneath that shortens when pressed, the way a key goes down.

One rule keeps it coherent:

> **Things you press are bricks.** Studs on top, lip underneath, lip shortens on
> press or selection.
> **Things you type into are sockets.** Recessed, studless — studs behind a
> cursor is just clutter.

Still drawn rather than real art, and fair game for conversion: the segmented
tray, the dropdown anchor and its chevron, `PersonDot`, and the ten-week plate
backing.

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

## The icon

An adaptive icon, hand-written as two vectors rather than exported: two 2-stud
medium bricks in running bond, blue under red, on the app's own night ground.

The geometry is the pack's, scaled — a stud is 18 units, a body row is 28/64 of
that, the nub is 16/64 and sits above the body — and the bottom brick's right
nub is covered by the top brick's body, because the paths are ordered exactly
the way `drawBrickArt` orders a plate. The whole mark is 54 × 36 units, inside
the 66-unit circle every launcher mask is guaranteed to show.

There is a `monochrome` layer for themed icons, where the colours are gone and
the join has to survive as a shape.

Note for future edits: `--` is illegal inside an XML comment, so the em-dash
convention used throughout the Kotlin will fail `mergeDebugResources`.

## Credits

Brick art and UI audio: [Kenney](https://kenney.nl) — Brick Pack 2.0 and the
interface sound pack, both CC0. Credit is not required by the licence; it is
here because the packs are good.

## Layout

```
model/Models.kt          Question, Answer, Reaction, DayEntry, CoupleState.
model/Blocks.kt          Brick geometry, BuildPlate, placement rules, Rewards.
model/BrickColor.kt      The four pickable colours, lifted from the pack SVGs.
data/MockData.kt         16 questions, a couple with history, three sculptures.
data/CoupleRepository.kt The seam Firebase slots into. Fake implementation here.
ui/theme/Theme.kt        The blended two-person palette.
ui/brick/                Atlas, geometry, drawBrickArt, BrickSlab, Sculpture.
ui/today/                Question, input, seal, waiting.
ui/reveal/               Both answers, the score, and the reaction row.
ui/history/              The Shelf: on this day, sculptures, ten weeks, index.
ui/us/                   Streak, sync score, live colour pickers, sound toggle.
```

`model/` is plain Kotlin on purpose — when the Firestore layer arrives it maps
documents to these types and nothing in `ui/` changes.

## Visual language

Deep navy ground, filled surfaces, and depth from real brick shading rather than
outlines or grey drop shadows. Shared surfaces are built from **both** people's
bricks: the hero question is a wall of yours and theirs, alternating.

The `ColorScheme` is built by hand rather than seeded from a tonal palette. The
blend of two chosen colours is computed in **HSL along the shorter hue arc**
with a saturation floor and a clamped lightness — averaging two complementary
colours in RGB gives grey, which is how the first version of this painted the
app's signature surface mud, and an unclamped blend put white text on magenta at
about 3:1.

Coloured shadows need API 28. Below that `glow` degrades to a standard black
shadow, which still reads fine.

## What is not built yet

- Firebase Auth, Firestore, FCM (see below)
- Pairing by invite code
- Notifications
- Automatic weekly sealing — the rule exists, the clock does not
- Undo on a placed brick, and brick rotation
- Wildcard (one of you writes the question) and time capsule (questions return
  in six months, then vs. now)
- `Rewards` never awards slopes, only flats plus a character at 7-day streaks

## Planned Firestore model

```
users/{uid}                      displayName, colorArgb, coupleId, tz, fcmToken
couples/{cid}                    members[2], homeTz, streak, freezes, queue[], lastDay
couples/{cid}/days/{yyyy-mm-dd}  questionId, type, submitted[], revealed, score, reactions
  .../answers/{uid}              response, guess, submittedAt      <- the sealed part
couples/{cid}/plates/{pid}       startedOn, completedOn, blocks[], placed[]
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

Reactions are a map on the day document, writable by either member only once
`revealed` is true, and each member may only write their own key.

A sealed plate — `completedOn != null` — should reject writes in rules as well
as in the repository. Otherwise a late listener could append a brick to last
week's sculpture.

Day boundaries follow the couple's `homeTz`, never device time. That is the
difference between working and not working if you are ever in different
timezones, and it is far cheaper to decide now than to migrate later.

## Toolchain

Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.10, Compose BOM 2026.08.00, compileSdk 37,
minSdk 26 (`java.time` without desugaring). Bytecode targets 17; the build JVM
is Temurin 25.

AGP 9 has Kotlin built in and **rejects** the `org.jetbrains.kotlin.android`
plugin. Do not add it back.

This host needs `-Djdk.net.unixdomain.tmpdir` pointed away from `%LOCALAPPDATA%\Temp`
or the Gradle daemon cannot accept a connection. It is set in
`~/.gradle/gradle.properties`, which is why this project deliberately does **not**
define `org.gradle.jvmargs` — a project-level value would override it and break
the build on this machine. Command-line builds also want it in `GRADLE_OPTS`.

When scripting a build, redirect to a log and echo `$?`. Piping Gradle through
`tail` reports the exit code of `tail`, which cheerfully returns 0 on a failed
build.
