# The sender

Three Firestore triggers that push to the *other* phone: your partner answered
the day, finished a deck, or wrote you a question.

This exists because a client cannot send a push to another client without
credentials that can message every device in the project, and those cannot live
in an APK. The app half is complete and shipping -- token registration, the
service, the notification -- and it simply receives nothing until this deploys.

## Not deployed yet

```
firebase deploy --only functions
```

fails with:

> Your project clickclack-002827 must be on the Blaze (pay-as-you-go) plan.

Cloud Functions have required Blaze since 2022. For an app with two users the
bill is nothing -- the free tier is two million invocations a month and this
sends at most a handful a day -- but it needs a card on the account, which is a
decision rather than a step.

<https://console.firebase.google.com/project/clickclack-002827/usage/details>

Nothing else is blocked on it. The app works, the daily local reminder works,
and the seal lifts on its own; what is missing until this deploys is only the
part that tells you the moment it happened.
