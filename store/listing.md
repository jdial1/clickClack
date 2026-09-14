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
| App name (30 max) | Click Clack |
| Default language | English (United States), en-US |
| App or game | App |
| Free or paid | Free |
| Category | Lifestyle |
| Tags | Relationships, Couples, Daily journal |
| Contact email | justinalandial@gmail.com |
| Countries | United States only |

## Short description (80 max)

> A daily question for two. Sealed until you both answer, then open it together.

(78 characters)

## Full description (4000 max)

```
Click Clack is a daily question for two people.

You each answer privately. Neither answer can be read until both are in. Then it
opens, and you talk about it. That's the whole idea: you answer honestly instead
of answering them.

ONE QUESTION A DAY
Both of your phones get the same question each morning. Some are light, some go
deep, and some ask you to guess what your partner will say. Special days like
anniversaries, birthdays and half-birthdays get questions of their own.

SEALED UNTIL YOU BOTH ANSWER
Your answer stays closed until theirs arrives. When it opens, react, keep the
ones worth keeping, and start a conversation right there.

KEEP TALKING
Every question and every mood can turn into a conversation. Write back as much
as you like, or just send an emote.

BUILD SOMETHING TOGETHER
Every answer earns bricks. You both place them on one shared plate, and at the
end of the week it's sealed with a date and kept on your shelf.

DECKS FOR EVERY KIND OF NIGHT
Pick a deck when you want more: getting to know you, everyday plans, travel,
food, late-night talks and silly stuff.

SEE HOW YOU FIT TOGETHER
Take longer profiles side by side: personality type, ways of loving, love
languages, attachment style, DISC, Enneagram and more. When you both finish, you
get a reading of what your results mean together.

REMEMBER THE DAYS THAT MATTER
Add your first date, anniversary and birthdays. On a special day, add a photo
and keep it on your timeline.

QUIET BY DESIGN
No ads and no feed. Notifications are bundled so your phone doesn't buzz every
time your partner taps something, and quiet hours keep nights quiet.

Click Clack works in pairs: one of you creates a code and the other enters it.
```

## Graphics

| Asset | File | Play requirement |
|---|---|---|
| App icon | `graphics/icon-512.png` | 512x512 PNG |
| Feature graphic | `graphics/feature-graphic.png` | 1024x500 PNG |
| Phone screenshots (8) | `graphics/phone/1-today.png` to `8-pairing.png` | 1236x2196, 9:16, 2 to 8 required |
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
