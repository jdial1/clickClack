/**
 * The one job a client cannot do for itself.
 *
 * Sending a push needs credentials that let you message any device in the
 * project. Put those in an APK and you have published them, so the part of the
 * app that says "they just answered" has to run somewhere the two people do
 * not have a copy of. This is that somewhere, and it does nothing else.
 *
 * It deliberately never reads an answer. The seal is the product: what it
 * needs to know is that a document was written, not what is in it.
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const db = getFirestore();

/** The other member of the couple, and the name to say in the notification. */
async function partnerOf(coupleId, uid) {
  const couple = await db.doc(`couples/${coupleId}`).get();
  const members = couple.get("members") || [];
  const partnerUid = members.find((m) => m !== uid);
  if (!partnerUid) return null;

  const [partner, sender] = await Promise.all([
    db.doc(`users/${partnerUid}`).get(),
    db.doc(`users/${uid}`).get(),
  ]);

  const token = partner.get("fcmToken");
  if (!token) return null;
  return { token, senderName: sender.get("displayName") || "They" };
}

/**
 * Data-only, always.
 *
 * A notification payload is drawn by the OS when the app is backgrounded,
 * with the OS's idea of the icon and no idea where a tap should go. The app
 * draws its own, so the wording is the only thing sent.
 */
async function push(target, title, body) {
  await getMessaging().send({
    token: target.token,
    data: { title, body },
    android: { priority: "high" },
  });
}

/** Your partner answered the day. Nothing about *what* they said. */
exports.onAnswer = onDocumentCreated(
  "couples/{coupleId}/days/{day}/answers/{uid}",
  async (event) => {
    const { coupleId, uid } = event.params;
    const target = await partnerOf(coupleId, uid);
    if (!target) return;

    await push(
      target,
      `${target.senderName} answered`,
      "Answer yours and the day opens.",
    );
  },
);

/** Your partner finished a deck and is waiting on you. */
exports.onQuizRun = onDocumentCreated(
  "couples/{coupleId}/quizzes/{quizId}/runs/{uid}",
  async (event) => {
    const { coupleId, uid } = event.params;
    const target = await partnerOf(coupleId, uid);
    if (!target) return;

    await push(
      target,
      `${target.senderName} played a deck`,
      "Yours is the half that is missing.",
    );
  },
);

/**
 * Somebody wrote a question.
 *
 * Worth a push on its own: a wildcard is the only content in the app either of
 * them made, and it is the one notification that is genuinely about a person
 * rather than about a schedule.
 */
exports.onWildcard = onDocumentCreated(
  "couples/{coupleId}/wildcards/{id}",
  async (event) => {
    const { coupleId } = event.params;
    const uid = event.data.get("byUid");
    if (!uid) return;

    const target = await partnerOf(coupleId, uid);
    if (!target) return;

    await push(
      target,
      `${target.senderName} wrote you a question`,
      "It will be the next one you are both asked.",
    );
  },
);
