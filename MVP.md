# Click Clack 1.0: the MVP plan

A checkpoint as of 23 September 2026. It covers what 1.0 is, where the project
stands, and the short list between here and the Play Store. DESIGN.md says what
the app is for, and SOUL.md checks the code against it. This file says what
ships.

> **1.0 is the smallest version that passes the soul's litmus test on two real
> phones, in two time zones, for a month, without either person needing help.**

---

## What 1.0 is

The core loop, and nothing that isn't needed to protect it:

| In 1.0 | Why it is load-bearing |
|---|---|
| Pairing by a code read aloud; unpairing and account deletion | No third person, and either person can leave (Social Safety) |
| One question a day, answered under seal, with a prediction | The Seal |
| The reveal: both answers, one reaction each, one line back, then *Now go and ask them about it.* | The Game Ends in the Other Room |
| "We did" the morning after | Reward the Offline Act on Trust |
| Flat bricks, a shared plate, weekly sculptures with a sealed two-word name | The Pair Is the Player |
| The Shelf: On this day, capsules, sculptures | Time Answers Back |
| Wildcards (a question written for your partner) | Mutual authorship, and the content engine |
| "Ask us this again in a year", and the first question you answered returning on its anniversary | Time Answers Back. Both reuse the capsule path. |
| Decks and the longer tests | Already built and aligned. Kept, not extended. |

**Scope freeze.** Moods, weather, horoscopes, zodiac, eras, local teams, the
bucket list, special-day photos and badges stay as they are. There's no new
feature work until 1.0 ships. Each is maintained, but none is extended (the
library's *Detail Nobody Feels* pitfall).

## Where it stands

**Done, but uncommitted.** Three soul-alignment passes left 35 files changed,
3 new (SOUL.md, MVP.md, VoiceTest.kt) and 1 deleted:
- flat rewards
- chat retired
- readings turned into questions
- pair-only numbers
- sealed naming words
- a four-year memory reach
- guilt-free copy
- the voice test

**Deployed.** The rules and the `onWords` function went out on 23 September.
The `onMessage` and `onPlateUpdate` functions were removed.

**Tests.** 235 unit tests pass, including `VoiceTest`. The rules emulator tests
for plates, chat, day and delete pass.

**Litmus.** 5 of 5 pass (SOUL.md).

---

## The plan

### 0. Checkpoint (today)

- [ ] Commit the soul-alignment work on a branch (`soul-alignment`), and open a PR into `main`. `master` is currently the working branch.
- [ ] Tag the merge `v0.9-soul`, as the known-good point before release work.

### 1. Blockers: the soul and the data (1 week)

Each of these breaks a promise the app already makes. See "Re-review after the
deploy" in SOUL.md.

**Status (23 September 2026):**
- 1.1, 1.2 and 1.4 are done, with 240 unit tests and all rules and function emulator tests green.
- **Still to do by hand:**
  - Deploy the 1.2 rules (`npx firebase-tools deploy --only firestore:rules --project clickclack-002827`).
  - Run the two backup commands in README ("Days, halves and backups"), then try one restore.

| # | Mechanic → | Dynamic → | Tone | Size |
|---|---|---|---|---|
| 1.1 ✅ | **Home-time-zone days.** The repository derives `today` from the couple's saved `timeZone`, not `LocalDate.now()`, and the Today screen rolls over at the couple's midnight. → | Two phones always answer the same day, so the second answer always lifts the seal → | The Seal holds across a trip or a move. | S–M |
| 1.2 ✅ | **Per-person writes in the rules.** Reactions, lines back, mood replies and deck progress may be written only under your own uid (`affectedKeys().hasOnly([me()])`, as the plate notes already do), with rules tests for each. → | Neither partner's app can speak for the other → | No levers, even by accident. | S |
| 1.3 ◐ | **Backups.** Turn on Firestore point-in-time recovery and a scheduled daily backup, and write the restore step into README. → | Years of answers survive a bad deploy or a bad script → | The archive is the long game, so it must outlive mistakes. | S (console) |
| 1.4 ✅ | **Test `onWords`.** An emulator test covers both new-style picks, and a new pick meeting an old-style pick on the plate. → | The week-naming path that only runs on the server is proven, not assumed → | The name appears for both, every week. | S |

### 2. Hard deadline: the functions runtime (before 30 October)

The deploy log warns that Node.js 20 is decommissioned on **30 October 2026**.
After that, no function can be deployed, including fixes.

- [ ] Set `"engines": { "node": "22" }` in `functions/package.json`.
- [ ] Upgrade `firebase-functions` (6.x → latest) and `firebase-admin`. Read the breaking changes the CLI warned about.
- [ ] Run the rules and delete emulator tests, deploy, and send one push end to end.
- [ ] While in there, drop the unused `uid` parameter from `joining()` in `firestore.rules` (compiler warning at 60:22).

### 3. Proof on real phones (1 week, overlapping 1–2)

Two real Google accounts on two physical phones, set to **different time
zones**, run the whole script:

- [ ] Sign in (including the Credential Manager fallbacks), pair by code, and try a code that has expired.
- [ ] Answer on both phones. The seal lifts on the second answer, and neither phone can read early.
- [ ] React, write a line back, and change a reaction. Check that the reveal's last line is the instruction.
- [ ] Mark "We did" the next morning. Both phones get the facing slopes.
- [ ] Place bricks and let the week seal. Pick words on both phones: the name shows for both, and only then.
- [ ] Write a wildcard. It arrives as a card and a push.
- [ ] Tap "Ask us this again in a year", take it back, and ask again. With the phone's date moved a year on (test build only), the question comes back sealed, beside the old pair.
- [ ] Quiet hours hold, and pushes batch.
- [ ] Unpair: both phones leave. Delete an account: the partner's own answers stay.

Then **a month of use by at least one real couple.** 1.0's definition says a
month, and Time Answers Back can't be checked in a day.

### 4. Release (1 week, after 1–3)

- [ ] `versionName` changes from `0.1.<commits>` to `1.0.0`. Keep `versionCode` as the commit count.
- [ ] Build `bundlePlay` with the upload key. Add the release SHA-1 to Firebase, or Google sign-in fails in the release build.
- [ ] Regenerate the store graphics (`-Pshots`) so `6-write.png` replaces `6-chat.png`, and upload in number order.
- [ ] Paste `store/listing.md` into the Play Console: description, data safety (in-app messages is still accurate, since lines back are messages), content rating and app access.
- [ ] Internal testing, then closed testing, then production. *(Verify: new personal Play developer accounts must run a closed test with a minimum number of testers for 14 days before production. Check the current requirement in the Play Console.)*

### Exit criteria for 1.0

- [ ] Soul litmus 5 of 5 on the release build (SOUL.md).
- [ ] Unit tests, `VoiceTest`, and all rules and function emulator tests are green.
- [ ] The real-phone script (section 3) passes in two time zones.
- [ ] Backups are on, and a restore has been tried once.
- [ ] The functions run on Node 22.

---

## After 1.0

These aren't in the MVP. They're kept here so they aren't lost. The full traced
brainstorm, and the ideas the soul refuses, are in SOUL.md.

| Idea | Pillar | Note |
|---|---|---|
| Character brick on every seventh day *together*, not every seventh *consecutive* day | Bad Month | Needs an all-time count the app doesn't load yet. |
| Letters to next year | Seal + Time | A sealed line that opens on the next anniversary. |
| The year shelf | Time | A year's sculptures in one row, dated as a whole. |
| Pause together | Bad Month | Either person pauses, and nothing is counted. |
| Question debt | Mutual | Invite, never require, a wildcard when a theme runs thin. |
| History rows with answers | Time | The Shelf index reads a subcollection per day. Load on demand if people use it. |
| Rename `com.ampersand` | — | Changes the `applicationId`. Only before the first public install, or never. |

## Cost warning

The numbers that usually mean success (session length, opens per day, messages
sent) should stay *low*. The only honest signal is the "We did" tap. 1.0 ships
without analytics, and that is a decision, not a gap.
