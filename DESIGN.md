# Click Clack — design ideals

What the app is for, what it sounds like, and the rules that decide arguments.
The README says how it is built. This says why it is built that way, so a future
change can be judged against something.

---

## The name

**Click Clack** is the sound of two bricks going down.

It is two halves that only mean something as a pair. A click on its own is a
switch, a pen, a bad speaker. It becomes the name of this app only when the
clack follows it. Nobody says clack first.

That is the product: two people, one thing between them, useless with one.

So the name is not decoration and it is not a logo waiting to happen:

- The **launcher icon** is two bricks in running bond, one in each person's
  colour. Not a heart, not a speech bubble, not a letter. Two separate pieces
  that only make a shape because they are stacked — the click and the clack.
- The **waiting state** is the name doing its job. You have answered and they
  have not, so the screen says **Click.** in full and **Clack.** breathing and
  unmade. It is the one place where the title of the app is also the state the
  app is in.
- The **shelf** captions a finished sculpture "built together", never "by you".

Two things never to do. Do not split the name across the UI as decoration —
"click" and "clack" appear as a pair, in that order, or not at all. And do not
let it become onomatopoeia for its own sake: the sound is the moment two people
land in the same place, not a noise the app makes at you.

> The app was called **Ampersand** until the rename. The `&` carried the same
> idea — a character whose only job is to join two things — and the code still
> uses `com.ampersand` and an `Amp` prefix throughout. That is a leftover, not a
> second identity.

---

## The one mechanic

**Sealed until both answer.**

Everything else is negotiable. This is not. It exists so you cannot read their
answer before writing yours, because the moment you can, you are responding
instead of answering, and the app stops producing anything worth talking about.

Consequences that follow from it and are therefore also not negotiable:

- Answers are **one document per person**, because Firestore rules are
  per-document and cannot hide a field inside a shared one.
- **No edits after submit.** `allow update, delete: if false`. What you said
  under seal is what you said.
- Day boundaries follow the couple's **home timezone**, never device time.

---

## What the app is not

Listed because each one is a thing this app will be asked to become, and the
answer is already no:

| Not a | Because |
|---|---|
| Chat app | You have four of those. The reveal is designed to end the session and send you into the other room. |
| Social network | There is no third person anywhere in the data model. There is no feed, no profile, no share. |
| Compatibility test | `syncPercent` is a curiosity on one screen, not a verdict, and it is never shown next to a question. |
| Streak-maximiser | Missing a day costs you a brick, not a lecture. |
| Journal | You write one answer a day and you cannot go back and improve it. |

---

## Voice

The app talks like **a person who knows you both and is not making a fuss.**

Not a brand. Not a coach. Not a robot with a smiley voice. Think of the friend
who says "you two should talk about that" and then leaves.

### Rules

1. **Say the thing.** "Maya has not answered yet." Never "Waiting for your
   partner to complete today's question!"
2. **Never exclaim.** No exclamation marks anywhere in the product. Not one.
3. **Never congratulate a number.** "Sealed 8 September" is a fact. "Amazing —
   7 day streak!!" is a slot machine.
4. **Never coach.** The app does not tell you what your answers mean, and never
   scores your relationship. It shows you both answers and gets out of the way.
5. **Use their actual names.** Never "your partner". The app knows who they are.
6. **Second person for you, first name for them.** "You said" / "Maya said".
7. **Contractions are fine; slang is not.** It should still read well in five
   years and in a language that is not American.
8. **The last line of the reveal is an instruction, and it is the only one the
   app ever gives:** *Now go and ask them about it.*

### Worked examples

| Instead of | Say |
|---|---|
| "Waiting for partner…" | "Maya has not answered yet." |
| "Congratulations! Streak extended" plus a party popper | "7 day streak" |
| "You're 84% compatible!" | "84% of guesses landed" |
| "Tap to reveal your results!" | "Open it" |
| "No entries found" | "Nothing sealed yet. Your first sculpture lands at the end of the week." |
| "Reaction sent!" | *(nothing — the brick is lit, that is the feedback)* |

---

## Materials

Which token and component to use is in [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md). This
section is why they are what they are.

One rule decides most component arguments:

> **Things you press are bricks.** Studs on top, a lip underneath that shortens
> on press, and where possible the pack's real side-view art rather than a drawn
> approximation.
>
> **Things you type into are sockets.** Recessed, studless. Studs behind a
> cursor is clutter, and a text field that looks pressable is a lie.

Three more that follow:

- **Never draw a brick from above.** Nothing else in the app is seen from above,
  so a top-down stud reads as a different object at every size. The 1-stud
  `BrickDot` replaced every drawn circle for exactly this reason.
- **No emoji. Anywhere.** An emoji is somebody else's art direction rendered by
  somebody else's font: it looks different on each phone the couple owns, it
  cannot be tinted, and it will never match a brick. Where a picture genuinely
  helps, use pack art -- Kenney's Emotes for the reactions, Game Icons for the
  hit and miss marks. Where no honest icon exists, use none: twenty holidays
  illustrated by whatever happened to be closest is worse than twenty named
  plainly. A deck is identified by its brick colour, which is already the app's
  identity system.
- **Geometry comes from the art, not from a grid.** A stud is 64px and a brick
  row is 28px, so the cell is 64 × 28. Inventing a square grid and stretching
  the art onto it is the obvious move and it looks wrong.
- **Real bricks over drawn ones, wherever the width allows it.** The pack only
  ships 1, 2, 4 and 6 stud widths, so anything wider is laid as a *course* of
  real bricks (`BrickSlab`) with alternate rows reversed, not as one stretched
  image and not as a rounded panel with dots along the top.

---

## The build, and why it is not a progress bar

Answering earns a brick. Both of you drop bricks on one shared plate. At the end
of the week the plate is **sealed with a date** and a fresh one starts.

Three decisions hold this together:

**Bricks do not remember.** A block knows its owner, its colour and the day it
was earned — and the UI never surfaces the last of those. A brick that could be
tapped to replay a conversation would turn the sculpture into a scrollable diary
and the plate into an index. What you actually want to be able to say is *"that
is the one we made the week of the ninth"*, so the **sculpture** carries the
date and the bricks stay anonymous.

**Payout is flat.** One brick each per completed day, shaped by how the
question is answered and how deep it was, never by the score. Paying more for a
better score would make the plate a scoreboard, and the point is that it records
turning up together.

**Unplaced bricks carry over when a plate seals.** They were earned by
answering; confiscating them would punish you for not tidying the tray before a
deadline you did not choose.

---

## Memory

The Shelf tab is the emotional payload, and it is ordered by feeling rather than
by chronology:

1. **On this day** — a day from six months or a year back, brought forward
   unasked, with both answers shown in full. Not a link to them: making you tap
   to find out what you said turns the best thing on the screen into a chore,
   and the whole value is in reading it by accident.
2. **The shelf** — finished sculptures, the selected one large.
3. **The last ten weeks** — the habit as a shape.
4. **Every day** — the index, which is the least interesting part and is
   therefore last.

It is called Shelf and not History on purpose. A shelf is where finished things
live; a history is a log, and a log is what this screen used to be.

---

## Colour

Each person picks one of four: **red, yellow, green, blue.** Four, not a picker,
because they are the colours the brick pack actually ships and a brick in a
colour the pack does not have is a tinted lie.

- Your surfaces lean your hue, theirs lean theirs.
- Shared surfaces are built from **both** — the hero question is a wall of your
  bricks and theirs, alternating.
- Blending happens in **HSL along the shorter hue arc** with a saturation floor.
  Averaging two complementary colours in RGB gives grey, which is how the first
  version of this ended up painting the app's signature surface mud.
- **Black is structure, not a choice.** It joins the nav bricks into one bar and
  backs a disabled button. It is not offered as a personal colour.

---

## Sound

Kenney's UI pack through `SoundPool`. `Sfx` maps **meaning** to file, not
filename to file, so retuning is a one-line change.

- Sounds are **feedback, never decoration.** Nothing plays that was not caused
  by a finger.
- A play that arrives before the pool has loaded is **dropped, not queued.** For
  UI chirps, a click that arrives late is worse than one that never arrives.
- The whole thing is one switch on the Us screen, and it is honoured everywhere.

---

## When in doubt

Ask which of these the change serves:

1. Does it get two people talking to each other, out loud, today?
2. Does it survive being used for two years by the same two people?
3. Would it still be true if the couple were having a bad month?

A feature that fails (3) is usually a feature that celebrates. Be careful with
those.
