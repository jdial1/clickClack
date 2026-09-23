# Click Clack: the soul

DESIGN.md says what the app is for. This file places that design in the Game
Souls library (`C:\Users\justin.dial\game_souls`), where it is the source game
of the **Ampersand** soul (`souls/ampersand.txt`, with field notes in
`souls/field_notes/ampersand.txt`). It follows the library's two report
formats: an analysis of what the app is, then a check of the app against its
own soul.

> **The screen asks the question; the game is what two people say to each
> other afterward.**

When a change is argued about, the soul's litmus test (below) is the quickest
check, and DESIGN.md's "When in doubt" questions are the same test in plain
words.

---

## Soul Analysis: Click Clack

### Verb Inventory

| Verb | What the player literally does | Kinetic profile |
|---|---|---|
| Answer | Types a line or picks an option for today's question, then predicts the partner's answer, and submits under seal | Not real-time. Committed: no edits after submit. |
| Open and react | Opens the revealed day once both are in, reads both answers side by side, and taps one of six reactions | Not real-time. The reaction is cancellable: tapping it again clears it. |
| Place | Drags an earned brick from the tray onto the shared plate | Snappy attack, forgiving release: an illegal drop falls to the first legal row in that column. |

Everything else (decks, tests, moods, the bucket list) is a variation on
*answer*, not a new verb.

### Fact Sheet

- **Interface:** one phone each, two accounts, one couple. Four screens: Today, Build, Shelf and Us. Every pressable thing is a brick and every text field is a socket (DESIGN.md, "Materials").
- **Rules:** one question a day, picked by date so both phones agree. Answers are one record per person, unreadable to the partner until both exist, and never edited. Each person gets one reaction and one line back. Rewards are flat.
- **Goals:** none that the app states. The only instruction it ever gives is *Now go and ask them about it.*
- **Entities:** questions (pool, occasions, capsules, and wildcards the partners write for each other), days, answers, reactions, bricks, plates, sculptures, and the Shelf.
- **Entity Manipulation:** answering, predicting, reacting, placing bricks, naming a finished week with two sealed words each, marking that you talked about a day, and hiding a topic for both.
- **Economy shape: none meaningful.** The library files Ampersand under "No Meaningful Economy" in `components/scarcity_economy.txt`. Bricks are a small flat generator with no drain: nothing is confiscated, unplaced bricks carry over, and no brick buys anything. Scarcity lives in *weight*, not resources: one question, one answer and one reaction a day.
- **Session:** about two minutes a day, for as long as the couple lasts. It is not meant to be replayed.
- **What players tell each other afterward:** what the other one said, and what they didn't know.

### Axis Profile

| Axis | Position | Evidence | Confidence |
|---|---|---|---|
| Failure | Trivial (absence) / Narrative (the wrong guess) | A missed day costs a brick and nothing else, and unplaced bricks carry over. A wrong guess is stamped SURPRISE and pays the same brick as a right one. | High |
| Information | Sealed (the other's answer) / Total (the rules) | One answer record per person, readable only once the day is revealed, which is enforced in `firestore.rules`. The server functions never read an answer. | High |
| Authorship | Mutual | The designer writes the prompts and the players write each other. Wildcards and the turn-by-turn pick of tomorrow's theme hand them the prompts too. | High |
| Power | Understanding (of a person, never finished) | Guessing the partner is the only skill, it unlocks nothing, and it is counted only for the pair. Capsules ask the same question again months later. | High |
| Tone | Plain Warmth. MDA aesthetic targets: Fellowship, then Expression | No exclamation marks, no emoji, no congratulated numbers, and the partner always named (DESIGN.md, "Voice"). | High |
| Structure | Ritual | One question per calendar day in the home time zone, plates sealed weekly, and an archive that comes back. | High |

### Components

| Component | Strength | Evidence | Tuning |
|---|---|---|---|
| social_safety | Core | Paired by a code read aloud, with no third person in the data model. Hiding a topic hides it for both, and unpairing ends it for both. Every number describes the pair. | The danger is one partner grading or pressuring the other. |
| theme_as_mechanic | Core | Two colours on one plate. The waiting state spells *Click.* in full and *Clack.* unmade. Roof slopes face each other, so a shared win makes a peak. | The theme is two halves joined, and joining is the mechanic. |
| interface_voice | Core | The DESIGN.md voice rules. "Reaction sent!" is replaced by nothing, because the lit brick is the feedback. | Plain and quiet. |
| failure_as_content | Supporting | The wrong guess is the reveal worth opening, with the SURPRISE stamp and the "I didn't know that" reaction. | The miss is the conversation. |
| trusting_the_player | Supporting | No interpretation. Paired test results end in a question, never a reading. | Trust the couple to have the conversation. |
| scarcity_economy | Supporting | One question, one sealed answer, one reaction and one line back. | Scarcity is weight, not a budget. |
| environmental_storytelling | Trace | Sculptures show who built which part, without text. | Too minor to list. |
| shared_discovery | Refused | "There is no third person anywhere in the data model." | Conflicts with social_safety. |

**Relationship conflicts and how the app resolves them:**

- **trusting_the_player conflicts with short casual sessions.** That conflict is about sessions too short to wander in. Here there is nothing to find: the trust is about the conversation afterward, which happens off-screen and takes as long as the couple likes.
- **failure_as_content requires legible_failure.** This is met trivially. The reveal shows both answers and both guesses side by side, so the cause of every miss is on screen.
- **interface_voice requires systemic_consistency.** This is met by the seal being enforced in storage, and by every derived value (the question, colours, turns, the name template) being computed from shared state, so both phones say the same thing.
- **social_safety conflicts with leaderboards, strangers and hidden roles.** None of these exist, and the chat that could have leaked to an audience has been retired.

### Residue

1. **The Seal:** both people commit blind and the reveal lands for both at once. No component covers symmetric withholding: Mansion's Hidden is about dread, and Tribunal's is asymmetric.
2. **The Pair Is the Player:** every artifact needs both people and the payout is flat. social_safety covers the ethics, but not the rule that the unit of play is two specific people.
3. **The Game Ends in the Other Room:** the session is designed to end, the payoff is off-screen, and "we talked about it" is paid on trust. No other soul measures success by the player leaving.
4. **Time Answers Back:** an archive returned unasked (On this day, capsules), with distance required and dates on the whole rather than the parts.

### Cross-Comparison

| Soul | Axis Matches | Shared Components | Residue Overlap | Topology | Ideal Player |
|---|---|---|---|---|---|
| Gauge | 1 (tone ½, authorship ½) | social_safety, interface_voice, theme_as_mechanic, trusting_the_player | Invitation only, no levers, the Bad-Month Test | Gift | Social High; Achievement Low |
| Tribunal | 1 (authorship) | theme_as_mechanic | None. Its hidden roles are exactly what social_safety forbids. | None | Social High, but through deception |
| Hearthian | 1.5 (failure ½, power) | failure_as_content, trusting_the_player | Understanding as the only power | None | Mastery and Immersion High |
| Casebook | 1.5 (failure ½, power) | interface_voice, theme_as_mechanic, trusting_the_player, scarcity_economy | None | Drain | Mastery High |
| Elysium | 1 (failure ½, power ½) | failure_as_content, theme_as_mechanic | Conversation as the verb, but with authored characters | None | Immersion High |
| Toybox | 1 (failure ½, tone ½) | trusting_the_player | None | None | Action High |

### Verdict

**New Soul, and the source of Ampersand.** No soul reaches two axis matches,
and all four residue items are uncovered. Gauge is the closest relative
through social_safety, but it is a gift economy between people who hide
nothing, while Ampersand's pair must both act before anything happens.

---

## Instilling Ampersand into Click Clack

The soul was drawn from DESIGN.md, so every gap here is a place where the code
drifted from its own design. There have been three passes, all applied as
uncommitted changes in September 2026. The first two are recorded in the
library's field notes under Closed Bends. This file keeps the current state.

### Tone Killers Found

The tone killer for Plain Warmth is *a verdict: any score, reading, or
celebration that grades the pair.* All three instances found have been fixed:

| Was | Now |
|---|---|
| Bricks sized by whether the guess landed, and the rarest piece for calling a deep question right | Flat: width from how the question is answered, height from depth and effort |
| Paired test results interpreted ("Those differences are where you cover for each other") | Both results, then one question |
| Deck results with per-person towers, a verdict, and a "guessed right more often" line | One tower for the pair, "7 of 10 guesses landed" |

### Structural Conflicts

- **Topology:** none. The app has no meaningful economy and the soul asks for none. Bricks never buy anything, which keeps it that way.
- **Relationship tags:** all resolved (see Components above).
- **Ideal player:** two features serve motivations the soul rates Low.
  - *Achievement:* the **badge wall** on Us shows each test as a slot, with unclaimed ones drawn as outlines. That is a completion collection. The **streak counter** on Today, with milestone headlines and a character brick every seventh day, is a run to protect.
  - *Neither has teeth.* A missed day costs only the count, and runs under seven are never mentioned. Both are still the features to watch (see Pitfalls).

### Axis Gaps

| Axis | Soul | Target today | Recommended change |
|---|---|---|---|
| Information | Sealed in storage | The seal holds for answers, deck runs, test drafts and naming words. Tomorrow's theme is hidden by the UI only. | Seal it in storage, or stop describing it as hidden (see Pitfalls). |
| Structure | One calendar per couple | Day boundaries use the home time zone saved from the ZIP code. README still lists "each phone's clock" as to-do. | Verify, and remove the stale README line. *(Medium confidence)* |
| Others | — | Aligned | — |

### Component Gaps

| Component | Status | Recommended change |
|---|---|---|
| social_safety | Aligned after the copy pass below | Watch "News Is Spent Once": Today's still-open card repeats for up to seven days. That's acceptable because it is a waiting item, not news, but it should never grow a count. |
| interface_voice | Aligned after the copy pass below | Add a test that fails on an exclamation mark, emoji, or "your partner" in user-facing strings once paired ("Every String Is a Test Target"). |
| theme_as_mechanic, failure_as_content, trusting_the_player, scarcity_economy | Aligned | — |

### Pitfalls Found

**Fixed in this pass** (applied, not yet committed):

| Pitfall | Evidence in the design | Fix |
|---|---|---|
| The Guilt Engine | The deck list headed "Maya is waiting for you", the deck notification said "Yours is the half that is missing.", and the reminder said "Waiting for you." | "Your turn" and "It opens when you've played it too." What is waiting, never who is waiting on whom. |
| Interface Voice: Name the Pause, Not the Loss | "You missed Tuesday's question." | "Tuesday's question is still open." |
| Failure as Punishment (in the voice) | A wrong guess was stamped MISSED. | SURPRISE. The miss is the best reveal of the day. |
| Interface Voice: Even Errors Stay in Voice | "Sign-in failed." | "Sign-in didn't go through. Try again." |

**Still open:**

| Pitfall | Evidence in the design | Fix |
|---|---|---|
| The Seal by Convention | `DayEntry.nextTheme` is "hidden from the partner by the UI, not rules" (`Models.kt`). | Move the pick into a per-person record, or drop the claim that it is hidden. |
| The Guilt Engine (latent) | The streak counter on Today and the seven-day character brick. | Keep the count quiet, or count days together rather than consecutive days (see recommendation 3). |
| Ranked Group (latent) | The badge wall's unclaimed outlines. | Show only tests you have both finished. An empty slot is a to-do list. |

**Checked, and not found:** The Scoreboard, The Chat Room, The Verdict, The
Short Memory, The Nagging Feed, The Cheerful Toast (there are no toasts or
snackbars in the app), The Red Signal, The Flavourful Lie ("7 of 10 guesses
landed" counts exactly what landed), and Theme as Paint.

### Signature Pillars, Translated

- **The Seal:** answers, deck runs, test drafts and naming words are all sealed in storage, and the seal lifts for both when the second record lands. The only exception is tomorrow's theme.
- **The Pair Is the Player:** colours are kept apart on one plate, the payout is flat, every number describes the pair, either partner can veto, and "We did" pays both.
- **The Game Ends in the Other Room:** one line back each, then *Now go and ask them about it.* as the last line of both the reveal and deck results. Notifications are batched and respect quiet hours.
- **Time Answers Back:** On this day needs a season (90 days) and prefers whole years, reaching back up to four years with both answers shown in full. Capsules ask a question again 12, 6, 3 or 1 months later. Sculptures are dated as a whole.
- **Plain voice** (now the soul's interface_voice tuning): the copy above. DESIGN.md's voice rules are this component, word for word.

### Litmus Results

| Check | Pass/Fail | Note |
|---|---|---|
| The Seal | Pass | Answers, runs, drafts and words are sealed in storage. Tomorrow's theme is openly shared, not hidden. |
| The Payout | Pass | `Rewards.forDay` and `forQuiz` are flat. |
| The Other Room | Pass | One line back each, then the instruction. Chat is retired. |
| The Witness | Pass | There is no third person in the data model. |
| The Bad Month | Pass | No lever notifications remain, and the Today pips count this week's days together rather than a run. |

### Prioritized Recommendations

All five were applied on 23 September 2026.

| # | Mechanic → | Dynamic → | Tone | Status |
|---|---|---|---|---|
| 1 | Deploy the rules and functions before shipping the new build → | The naming words become genuinely unreadable until both have picked, and the chat stops taking messages → | The seal holds everywhere the app says it does. | Done 23 September: `onWords` created, `onMessage` and `onPlateUpdate` deleted, rules released |
| 2 | Show only tests both of you have finished on the badge wall → | Nobody opens Us to see what's left to collect → | Plain Warmth rather than a checklist. | Already true. `ProfileBadges` draws only groups both of you finished; the doc comment that said otherwise was stale and is fixed. |
| 3 | The Today pips count days you both answered this week, with no number and no milestone emphasis → | A gap stops feeling like a broken run, and the row resets with the plate, not with a miss → | The ritual survives a bad month. | Done |
| 4 | Stop hiding tomorrow's theme: both see it as soon as it's picked ("Maya picked food for tomorrow.") → | Nothing is called hidden that isn't sealed → | Trust in the seal is total. | Done |
| 5 | `VoiceTest` fails on "!" or emoji in UI strings and question files, or on "your partner" outside pairing → | Voice regressions are caught before a build ships → | The voice stays the same for years. | Done |

**Left as it is:** the character brick still arrives on every seventh
*consecutive* day (`Rewards.forDay`). It is a quiet reward with no penalty,
but it is the last streak-shaped thing in the app. Making it every seventh
day answered together would need an all-time count the app doesn't load.

### Re-review after the deploy (23 September 2026)

The library had not changed since the previous pass. The full pitfall index
was scanned again against the current code; only pitfalls with evidence are
listed.

| Pitfall or rule | Evidence | Weight |
|---|---|---|
| **The Seal: one calendar per couple** (DESIGN.md: "never device time") | `FirestoreCoupleRepository` takes `today = LocalDate.now()`, the phone's own date. Two phones in different time zones can answer different days, so the second answer never lifts the first's seal. | **Blocker** for 1.0 |
| **Social Safety: No Levers** | The rules check membership, not whose key is written (README, "Still to do"), so one partner's app could set or clear the other's reaction or line back. | **Blocker** for 1.0 |
| **Dead Servers** | The whole archive, which *is* Time Answers Back, lives only in Firestore, with no backup or export. | **Blocker** for 1.0 |
| **Detail Nobody Feels** | Horoscopes, Chinese zodiac, eras, local teams, weather, moods, a bucket list, photos and nine personality tests sit around a two-minute daily loop. None breaks the soul, but each is copy and tests to maintain. | Scope freeze, not a cut |
| Character brick on every seventh *consecutive* day | The last streak-shaped reward. | After 1.0 |

The litmus test still passes on all five checks. Everything else in the index is
either not present or belongs to another soul's areas.

### Brainstorm: features the soul suggests

Not recommendations yet, but traced so each can be judged. Each idea names the
pillar it serves, the litmus check it could fail if built carelessly, and a
rough size (S is a day or two, M is a week, L is more than that).

**Status (23 September 2026):**
- **Built:** *Ask us again* and *The first question again*. Both ride on the capsule path and come back sealed on their date, whatever the weekday, with nothing announcing them beforehand.
- **Already present:** *Question debt* (every reveal offers "Write a question for Maya") and *The reveal goes quiet* (nothing sits below the last line).
- **Everything else** waits until after 1.0.

#### The Seal

| Idea | Mechanic → | Dynamic → | Tone | Watch | Size |
|---|---|---|---|---|---|
| **Letters to next year** | On the anniversary, each person writes one sealed line that opens on the next anniversary. → | Something is written blind a year ahead, then found → | The Seal stretched across a year. | The Seal: the line must be sealed in storage, not just hidden on screen. | M |
| **Ask us again** | After a reveal, either person can mark the day "ask us again in a year." The question comes back sealed on that date, with the old pair beside the new one. → | Couples bookmark the answers they expect to change → | Time answers back, on their terms. | The Bad Month: it must never announce a date they're dreading. | S |
| **The week in one guess** | When a plate seals, each person privately picks the day of the week they think the other will call their favourite, and both picks are revealed together. → | Both re-read the week before choosing → | The week becomes a conversation, not a log. | The Payout: pays nothing, and says nothing about who guessed right. | S |

#### The Pair Is the Player

| Idea | Mechanic → | Dynamic → | Tone | Watch | Size |
|---|---|---|---|---|---|
| **Two-part pieces** | Some days award half an arch to each person, and the arch can only be placed once both halves exist. → | Days you both turned up visibly join on the plate → | Colours apart, object together. | Nothing Is Made by One: a lone half must never read as a debt. | M |
| **Swap day** | Once a month, each person answers *as the other*, under seal. The reveal puts "you as them" beside their real answer. → | The prediction grows into a whole answer, and the gap is funnier and kinder → | The miss is the conversation. | The Bad Month: easy to skip, and never on a heavy question. | S |
| **Question debt** | When a theme's pool runs thin, invite (never require) a wildcard: "Write one for Maya." → | The couple authors more of what they're asked → | Mutual authorship over a longer run. | No Levers: an invitation, never a to-do. | S |

#### The Game Ends in the Other Room

| Idea | Mechanic → | Dynamic → | Tone | Watch | Size |
|---|---|---|---|---|---|
| **Out-loud questions** | A rare question type with no text box. It shows the question to both and one button, "We asked it," which pays like a talked-about day. → | The whole exchange happens off the phone → | The pillar in its purest form. | The Seal: speech can't be sealed, so keep these for occasions, not the daily draw. | S |
| **The reveal goes quiet** | After the last line, the screen dims and nothing below it is tappable until tomorrow's question. → | There's nowhere left to scroll → | Design the Session to End. | The Other Room is already passing; this only makes it visible. | S |

#### Time Answers Back

| Idea | Mechanic → | Dynamic → | Tone | Watch | Size |
|---|---|---|---|---|---|
| **The year shelf** | On the anniversary, the year's sculptures stand in one row, dated as a whole: "Our second year." → | Fifty-two weeks read at a glance → | Time answers back, without a log. | Date the Whole: no per-week captions on the row. | M |
| **The first question again** | On the anniversary, the very first question the couple ever answered comes back, sealed as before. → | They meet their day-one selves → | Built for Year Two. | A Memory Needs Distance: only on whole years. | S |
| **Pause together** | Either person can pause the ritual with no reason given. Both phones show only the Shelf, and nothing is counted. → | A hard month costs nothing and needs no explanation → | Kind when barely speaking. | Either Can Veto: either person can resume, and neither is asked why. | M |

#### Refused on sight

Ideas that will come up, and why the soul says no. Keep these here so the
argument only happens once.

| Idea | The check it fails |
|---|---|
| "Maya is typing" or "Maya opened today's question" | No Levers: presence becomes pressure. |
| Read receipts on the reveal | No Levers, and the Bad Month. |
| Streak freezes, or a paid streak repair | The Guilt Engine, and it makes the streak something to buy. |
| A compatibility score, or a percentage on the question card | The Payout and The Verdict: the tone killer itself. |
| Share a reveal or a sculpture to social media | The Witness: an audience changes what people write under seal. |
| Couples comparing against other couples | The Witness, and the Ranked Group. |
| An AI summary of "your relationship this month" | The Verdict: the game interpreting the pair. |
| A nudge button | No Levers, word for word. |

### Cost Warning

- **Some numbers will fall on purpose.** Session length, repeat opens and messages sent all rise when the soul is failing. The only honest success signal is the "We did" tap, which is self-reported.
- **Removing chat takes away something people may already use.** The store listing now says why.
- **Content is paid for by the calendar:** a question a day is 365 a year. Capsules and wildcards are how the pool lasts.
