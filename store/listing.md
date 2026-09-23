# Google Play listing: Click Clack

Everything the Play Console asks for, in the order it asks. Images are in
`store/graphics` (regenerate with
`gradlew testDebugUnitTest --tests com.ampersand.ui.StoreAssets -Pshots`).
The bundle is `app/build/outputs/bundle/play/clickclack-<version>-play.aab`
(`gradlew bundlePlay`).

## App details

| Field | Value |
|---|---|
| Package name | `com.ampersand` (free on Play as of September 14, 2026) |
| App name (30 max) | Click Clack: Couples Questions |
| Default language | English (United States), en-US |
| App or game | App |
| Free or paid | Free |
| Category | Lifestyle |
| Tags | Relationships, Couples, Daily journal |
| Contact email | justinalandial@gmail.com |
| Countries | United States only |

## Short description (80 max)

> One question a day for two. Answer it, then build a brick sculpture together.

(77 characters)

## Full description (4000 max)

```
You've been together long enough to finish each other's sentences. Great. Now answer this without looking at theirs.

Click Clack gives you and your person one question a day. You each answer on your own phone, and it stays sealed until you're both in. No reading theirs first, no tweaking yours to match. Then it opens, and the fun part starts.

BUILD A LITTLE SCULPTURE, TOGETHER
Every answer earns bricks in your color. Your partner earns bricks in theirs. All week you both stack them on one shared plate, balancing, building up, and occasionally wrecking each other's tower. At the end of the week it's sealed for good, and you name it together, like "The Cosmic Castle of Wobbly Pretzel."

A SHELF THAT GROWS WITH YOU
Every finished week goes on your shelf with its name, its date and a line from each of you about the week. Scroll back months from now and there it is: a row of tiny, slightly lopsided proof that you kept showing up for each other.

WHAT A DAY LOOKS LIKE
Morning: a question shows up. Sometimes it's easy ("What is my favorite dish of all time?"), sometimes it goes deeper ("What is the biggest thing you are carrying right now?"), and sometimes you have to guess what they'll say.
Whenever: you answer. They answer. Click. Clack.
Later: it opens. React, laugh, keep the good ones, talk about it, and go spend your bricks.

THEN PUT THE PHONE DOWN
When it opens, you each get one reaction and one line back. That's on purpose. Click Clack isn't another place to text each other. It's the reason to go and talk in the same room.

DECKS FOR EVERY MOOD
Want more than one question tonight? Pick a deck: getting to know you, travel, food, silly stuff, or the late-night kind of talk.

SEE HOW YOU FIT
Take the longer tests side by side: personality type, love languages, attachment style, ways of loving, and more. When you've both finished, Click Clack puts your results next to each other and asks you one question about them. What it means is yours to work out.

THE DAYS THAT MATTER
Add your anniversary, birthdays and first date. Click Clack remembers them (half-birthdays too) and gives those days questions of their own. Snap a photo on the big days to keep on your timeline.

QUESTIONS THAT KNOW YOU
Grew up in the 90s? Expect a question about it. Into horoscopes? Turn them on. Not a fan of a topic? Turn it off. It's your daily question, after all.

NOT ANOTHER NOISY APP
No ads. No feed. No strangers. Notifications get bundled so your phone doesn't buzz every time they tap something, and quiet hours keep bedtime quiet.

It takes two: one of you makes a code, the other types it in, and you're paired. Go ahead, send your person the code.
```

(2671 characters)

## Graphics

| Asset | File | Play requirement |
|---|---|---|
| App icon | `graphics/icon-512.png` | 512x512 PNG |
| Feature graphic | `graphics/feature-graphic.png` | 1024x500 PNG |
| Phone screenshots (8) | `graphics/phone/1-today.png` to `8-decks.png`, upload in number order (6 is now `6-write.png`; regenerate before uploading) | 1236x2196, 9:16, 2 to 8 required |
| Launch splash (reference) | `graphics/splash.png` | Not uploaded; matches the in-app Android 12+ splash |

Tablet screenshots are optional and skipped.

## Store settings

- **Privacy policy URL:** https://clickclack-002827.web.app/privacy
  (Firebase Hosting serves `store/privacy.html`; redeploy with
  `firebase deploy --only hosting`).
- **Sign in details (App access):** Yes, restricted. Add a paired Google test
  account, and in "Any other information" say: "Tap 'Skip for now and look
  around' on the sign-in screen to open every screen with sample data and no
  account. Signing in with the account above shows the live, paired version."
- **Ads:** No, the app contains no ads.
- **Target audience:** 18 and over. Not designed for children.
- **Content rating (IARC questionnaire):** Category "Social or communication".
  Users can interact and exchange content: yes, but only with the one person they
  paired with. No violence, sexual content, gambling or drugs. Shares location:
  no (a ZIP code is used only for local weather). Expect a Teen or Mature 17+
  rating because of the private messaging.
- **News app:** No. **Government app:** No. **Financial features:** None.
  **Health app:** No.

## Data safety form

Data is encrypted in transit (HTTPS/TLS). Nothing is sold or shared with third
parties for advertising. Data is not collected for ads or analytics.

| Data type | Collected | Shared | Purpose | Optional |
|---|---|---|---|---|
| Name | Yes | No | App functionality, account management | Required |
| Email address | Yes (Google sign-in) | No | Account management | Required |
| User IDs | Yes (Firebase user id) | No | App functionality, account management | Required |
| Other info: date of birth, gender, relationship dates | Yes | No | App functionality (special days, questions) | Optional |
| Approximate location: ZIP code | Yes | No | App functionality (local weather and time zone) | Optional |
| Messages: in-app messages | Yes | No | App functionality | Optional |
| Photos | Yes | No | App functionality (special-day photos) | Optional |
| Other user-generated content: answers, moods, reactions | Yes | No | App functionality | Required for core use |
| Device or other IDs: push token | Yes | No | App functionality (notifications) | Optional |

Data deletion: users can request deletion. **Delete account URL:**
https://clickclack-002827.web.app/delete

## Account deletion

The web link is live: sign in with Google, confirm, and the `deleteAccount`
Cloud Function removes the account, profile, answers, runs, drafts, messages,
photos, dates, bucket items and written questions, strips per-person fields from
shared documents, and ends the pairing. The partner's own answers stay; once both
accounts are gone the whole couple is deleted. Tested in
`rules-test/delete.test.mjs`.

In the app: Us, Settings, Delete account (two taps), which calls the same function.

## After the first upload

Play App Signing re-signs the bundle with Google's own key. Google sign-in fails
on Play installs until that key's SHA-1 is added to the Firebase Android app:
Play Console, Test and release, App integrity, App signing key certificate,
copy SHA-1. Then Firebase console, Project settings, Your apps, `com.ampersand`,
Add fingerprint. Download the new `google-services.json` and rebuild.

The upload key is shared with Chronicle Cryptogram (SHA-1
`50:5E:C3:4B:5E:E6:A7:05:76:09:C8:03:8A:B3:DA:8C:A5:67:21:0B`). Its settings
are in `keystore.properties`, which is gitignored.

Version codes come from the commit count, so every new commit uploads as a newer
version.
