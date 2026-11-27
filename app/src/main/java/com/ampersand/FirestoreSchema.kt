package com.ampersand

/**
 * Firestore collection names: the one copy of the layout in README.md, "Firestore
 * model", shared by `auth/` (users, invites, couples), `data/` (everything under a
 * couple) and `notify/` (the push token on the user document). `firestore.rules`
 * and `functions/index.js` use the same names and must change with these;
 * `ServerContractTest` fails if they mention one that is missing here. The server
 * also keeps `outbox` and `chats/{id}/messages`, which no client code touches.
 */
object FirestoreSchema {
    const val USERS = "users"
    const val INVITES = "invites"
    const val COUPLES = "couples"

    // Under couples/{cid}.
    const val DAYS = "days"
    const val QUIZZES = "quizzes"
    const val INSTRUMENTS = "instruments"
    const val PLATES = "plates"
    const val WILDCARDS = "wildcards"
    const val BUCKET = "bucket"
    const val DATES = "dates"
    const val PHOTOS = "photos"
    const val KNOWING = "knowing"

    // Under a day, deck, instrument or plate.
    const val ANSWERS = "answers"
    const val RUNS = "runs"
    const val DRAFTS = "drafts"
    const val WORDS = "words"

    /** The push token field on users/{uid}. */
    const val FCM_TOKEN = "fcmToken"
}
