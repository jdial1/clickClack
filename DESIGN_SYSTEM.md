# Click Clack: design system

DESIGN.md says what the app is for and what things are made of. This file is the
working rulebook: which token, which component, which layout. If a screen needs
something that is not here, add it here first, then use it.

`DesignSystemTest` enforces the rules marked **(test)**. The tokens live in
`ui/theme/Theme.kt` and the shared controls in `ui/components/`.

The brick art, the theme and the splash are out of scope. They draw to the pack's
geometry, not to this scale.

---

## Spacing **(test)**

Every gap is a `Space` token. There are eight, and nothing goes between them.

| Token | dp | Use it for |
|---|---|---|
| `Space.hair` | 2 | A label and its line, inside a pill |
| `Space.xs` | 4 | Lines of one thing: a title and its detail |
| `Space.s` | 8 | An icon and its label; tight rows |
| `Space.m` | 12 | Items in a group; a section label and what it heads |
| `Space.l` | 16 | A panel's own padding |
| `Space.xl` | 24 | Between sections |
| `Space.xxl` | 32 | Between unrelated sections |
| `Space.page` | 48 | Top and bottom of a page |

**The one exception is the brick grid.** A `BrickSlab` rounds its height up to whole
courses, so 2dp too much content adds a whole row of bricks. That is how buttons with
an icon came out double height. Padding inside a brick is fitted to the course and
marks itself `// course-fit`. Do not use that marker for anything else.

## Layout

- **Side margin:** every screen uses `Inset.screen`. No screen picks its own.
- **Width:** the page is 12 studs. Bricks line up on that, so anything full width is 12 studs.
- **Top of a screen:** `BackAction`, then anything else on that line, then the content.
  If the line will not fit a phone, the extra item goes on its own line beneath.
- **Order:** the thing the screen is for comes first. Today leads with the question, and
  everything else waits under "More today" until you have answered.
- **Bottom of a page:** end on `Space.page`, so the last thing clears the tab bar.

## Corners **(test)**

`Radius` for a shape, `Corner` where a function takes a `Dp`.

| Token | dp | Use it for |
|---|---|---|
| `hair` | 3 | Thin bars only |
| `small` | 8 | Small images and marks |
| `medium` | 12 | Glass panels, status boxes, small cards |
| `control` | 20 | Compact panels |
| `card` | 28 | Full panels |

## Type **(test)**

Use one of these roles. Raw `fontSize` belongs in the theme.

| Role | Style | Use it for |
|---|---|---|
| Hero | `displaySmall` | Today's question, and nothing else |
| Screen title | `headlineMedium` | "Decks", "Settings" |
| Heading | `headlineSmall` | A question on a reveal; a sculpture's name |
| Card title | `titleMedium` | Button labels, panel titles |
| Row title | `titleSmall` | A row in a list |
| Input | `bodyLarge` | What you are typing, and answers |
| Text | `bodyMedium` | Sentences |
| Secondary | `bodySmall` | The line under a title |
| Action | `labelLarge` | `IconAction`, `QuietAction`, small tags |
| Section | `labelSmall` | `SectionLabel`, uppercase |

Two named exceptions live in the theme: `NavLabel` and `InviteCodeText`.

## Colour **(test)**

Raw colours belong in the theme, and a person's colour comes from `BrickColor`.

| Role | Token |
|---|---|
| Text on the wall | `onBackground` |
| Secondary text | `onSurfaceVariant` |
| A quiet action | `primary` |
| Text on a brick | `inkOnBrick(slug)`, never a fixed colour |
| Text across both colours | `inkOnBricks(a, b)` |
| A special day | `SpecialGold`, for that and nothing else |
| Something wrong | `error` |

## Controls **(test)**

Things you press are bricks, and things you type into are sockets (DESIGN.md).

| You want | Use | Not |
|---|---|---|
| The main action on a screen | `AmpButton` | Material buttons |
| To go back | `BackAction` | An icon and text, or `TextButton` |
| A way out beside a main action | `QuietAction` | `TextButton` |
| A small action with an icon | `IconAction` | A custom row |
| A section heading | `SectionLabel` | A styled `Text` |
| An empty or waiting message | `QuietNote` | A styled `Text` |
| To type | `AmpField` | `TextField` |
| To choose | `AmpRadioGroup`, `AmpCheckboxGroup`, `AmpDropdown`, `AmpScale`, `AmpDragRank` | Material selection controls |
| To react | `AmpReactionRow` | A custom row |

**One main action per view.** A coloured `AmpButton` means "this is what you came for".
Grey icon buttons are secondary, so colour stays for what matters.

## Panels and rows **(test)**

Three surfaces, and the choice follows what the thing is for.

| You have | Use | Looks like |
|---|---|---|
| Something you read: a stat, a memory, a list entry | `Modifier.panel()` | A glass pane on the wall |
| A row you press on its own: a setting, a toggle | `ActionRow` | One brick, the same height as `AmpButton` |
| The one thing the screen is about: today's question | `BrickSlab` with `.glass(ink)` inside | A wall of bricks, used once per screen |

- A panel's padding is `Space.m` on all sides, or `Space.s` vertically for a compact list.
- Order the chain `.panel().clickable { }.padding(...)` so the press area fills the pane.
- `Emphasis` says how far a panel lifts: `NORMAL`, `RAISED` for a special day or a list on
  a busy wall, `SELECTED` for the chosen tile. Do not pass a raw strength.
- Do not put a pane behind text on a brick that is already one colour. It costs height and
  adds nothing, and it is why Settings rows used to be three courses tall.
- A solid card over the wall is `.clip(Radius.medium).background(surface.copy(alpha = Alpha.strong))`.

**Staying one brick on every phone.** Bricks scale with the screen but text does not, so
fixed padding only fits one width. `AmpButton` and `ActionRow` pass `snug = true` to
`BrickSlab`, which lets empty padding overhang instead of adding a course. Only use it where
the content is mostly padding, because real content would overflow.

## Opacity **(test)**

Six steps in `Alpha`, each with a job. For colour on text, prefer `onSurfaceVariant` first.

| Token | Value | Use it for |
|---|---|---|
| `faint` | 0.12 | A wash: the sign-in wall, an owner's tint |
| `unlit` | 0.25 | Bricks not earned yet: an empty pip, an unlit stud |
| `dim` | 0.4 | Off: disabled, not selected |
| `quiet` | 0.6 | Resting, or waiting on someone else |
| `secondary` | 0.8 | Secondary text on a brick |
| `strong` | 0.9 | Nearly solid: a card or sticky header; a pressed brick |

A control that fades up as it is selected uses `Alpha.rise(from, t)`. The glass and wall
scrims in `brick/` and the splash keep their own values, tuned to the art.

## Icons **(test)**

| Token | dp | Use it for |
|---|---|---|
| `IconSize.mark` | 14 | Inside a line of small text |
| `IconSize.small` | 20 | A marker next to a label |
| `IconSize.medium` | 22 | The default: actions and buttons |
| `IconSize.large` | 32 | The icon leading a row: weather, a mood, a special day |
| `IconSize.tile` | 44 | The icon that is the point of a tile: a deck, a theme |

## Where things live

- `ui/theme/Theme.kt`: every token.
- `ui/components/`: anything used by more than one screen. `Panel.kt`, `Common.kt`, `Forms.kt`, `UiIcon.kt`.
- `ui/<feature>/`: a screen, and pieces only that feature uses, in files named for what they are.
  Today is `TodayScreen`, `AnswerInput`, `Moods`, `ShapeTomorrow` and `TodayCards`. Us is
  `UsScreen`, `SettingsScreen` and `BucketList`.
- If a second feature needs a piece, it moves to `components/`.
- No file over about 800 lines. When one grows past that, split it by what the pieces are.
