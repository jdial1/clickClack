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
  return { token, uid: partnerUid, senderName: sender.get("displayName") || "They" };
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

/**
 * Hold a push in the couple's outbox. [flushOutbox] sends it, batched with
 * anything else for the same person, outside quiet hours.
 */
async function queue(coupleId, target, title, body, kind = "update") {
  await db.collection(`couples/${coupleId}/outbox`).add({
    to: target.uid, from: target.senderName || "", title, body: body || "", kind, at: Date.now(),
  });
}

/** Your partner answered the day. Nothing about *what* they said. */
exports.onAnswer = onDocumentCreated(
  "couples/{coupleId}/days/{day}/answers/{uid}",
  async (event) => {
    const { coupleId, uid } = event.params;
    const target = await partnerOf(coupleId, uid);
    if (!target) return;

    await queue(coupleId, target,
      `${target.senderName} answered`,
      "Answer yours and the day opens.",
      "answered",
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

    await queue(coupleId, target,
      `${target.senderName} played a deck`,
      "Yours is the half that is missing.",
      "deck",
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

    await queue(coupleId, target,
      `${target.senderName} wrote you a question`,
      "It will be the next one you are both asked.",
      "question",
    );
  },
);

// ---- Pushes added for replies, moods, week names and special dates --------

const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");

/** Keys of [field] whose value changed to something non-empty between two snapshots. */
function changedKeys(before, after, field) {
  const a = before.get(field) || {};
  const b = after.get(field) || {};
  return Object.keys(b).filter((k) => b[k] && JSON.stringify(a[k]) !== JSON.stringify(b[k]));
}

/** A reaction, a line back or a mood reply on a day. */
exports.onDayUpdate = onDocumentUpdated(
  "couples/{coupleId}/days/{day}",
  async (event) => {
    const { coupleId } = event.params;
    const before = event.data.before;
    const after = event.data.after;

    for (const uid of changedKeys(before, after, "reactions")) {
      const target = await partnerOf(coupleId, uid);
      if (target) await queue(coupleId, target, `${target.senderName} reacted`, "See it on the day you both answered.", "reacted");
    }
    for (const uid of changedKeys(before, after, "notes")) {
      const target = await partnerOf(coupleId, uid);
      if (target) await queue(coupleId, target, `${target.senderName} wrote a line back`, after.get("notes")[uid], "message");
    }
    for (const uid of changedKeys(before, after, "moodReplies")) {
      const target = await partnerOf(coupleId, uid);
      if (target) await queue(coupleId, target, `${target.senderName} replied to your mood`, after.get("moodReplies")[uid], "message");
    }
  },
);

const MOOD_LABELS = {
  SNAPPED_IN: "Happy", CLICKED: "Content", PLAYING_IT_COOL: "Fine", NEW_SET: "Excited", FLAT_PLATE: "Meh",
  SOMETHING_TO_TELL: "Got news", RARE_PIECE: "Grateful", SOLID_GROUND: "Steady", STILL_IN_THE_BOX: "Tired",
  HEAVY: "Down", HANDLE_WITH_CARE: "Stressed", MISSING_A_PIECE: "Missing you", KNOCKED_OVER: "Overwhelmed",
  STEPPED_ON_ONE: "Annoyed",
};

/** Your partner set a new mood. */
exports.onMoodUpdate = onDocumentUpdated("users/{uid}", async (event) => {
  const { uid } = event.params;
  const before = event.data.before.get("mood") || {};
  const after = event.data.after.get("mood");
  if (!after || (before.id === after.id && before.on === after.on)) return;
  const coupleId = event.data.after.get("coupleId");
  if (!coupleId) return;
  const target = await partnerOf(coupleId, uid);
  if (!target) return;
  const label = MOOD_LABELS[after.id] || "a new mood";
  await queue(coupleId, target, `${target.senderName} is feeling ${label.toLowerCase()}`, "Tap their mood on Today to reply.", "mood");
});

/** Words picked toward a finished week's name, and the name once both are in. */
exports.onPlateUpdate = onDocumentUpdated(
  "couples/{coupleId}/plates/{plateId}",
  async (event) => {
    const { coupleId } = event.params;
    const picked = changedKeys(event.data.before, event.data.after, "words");
    if (picked.length === 0) return;
    const words = event.data.after.get("words") || {};
    const both = Object.keys(words).length >= 2;
    for (const uid of picked) {
      const target = await partnerOf(coupleId, uid);
      if (!target) continue;
      if (both) await queue(coupleId, target, "Your week has a name", `${target.senderName} picked the last words. See it on the Shelf.`, "week");
      else await queue(coupleId, target, `${target.senderName} picked words for the week`, "Pick yours to see its name.", "week");
    }
  },
);

/**
 * Hourly: at 8am in each couple's own time zone (saved from their home ZIP,
 * Central when none is set), push birthdays, anniversaries and dates that fall today.
 */
exports.specialDates = onSchedule({ schedule: "0 * * * *", timeZone: "UTC" }, async () => {
  const couples = await db.collection("couples").where("endedAt", "==", null).get();
  for (const couple of couples.docs) {
    const zone = couple.get("timeZone") || "America/Chicago";
    const local = new Date(new Date().toLocaleString("en-US", { timeZone: zone }));
    if (local.getHours() !== 8) continue;
    const md = `${String(local.getMonth() + 1).padStart(2, "0")}-${String(local.getDate()).padStart(2, "0")}`;
    const onToday = (iso) => typeof iso === "string" && iso.slice(5, 10) === md;

    const members = couple.get("members") || [];
    const users = await Promise.all(members.map((m) => db.doc(`users/${m}`).get()));
    const events = [];
    if (onToday(couple.get("anniversary"))) events.push("It's your anniversary");
    if (onToday(couple.get("marriedOn"))) events.push("It's your wedding anniversary");
    const milestone = togetherOn(local, couple.get("anniversary"), couple.get("marriedOn"));
    if (milestone) events.unshift(milestone);
    // A half-birthday is six months on from the birthday.
    const halfOnToday = (iso) => {
      if (typeof iso !== "string") return false;
      const d = new Date(`${iso}T12:00:00Z`);
      d.setUTCMonth(d.getUTCMonth() + 6);
      return `${String(d.getUTCMonth() + 1).padStart(2, "0")}-${String(d.getUTCDate()).padStart(2, "0")}` === md;
    };
    users.forEach((u) => {
      if (onToday(u.get("birthday"))) events.push(`It's ${u.get("displayName") || "a"}'s birthday`);
      if (halfOnToday(u.get("birthday"))) events.push(`It's ${u.get("displayName") || "a"}'s half-birthday`);
    });
    const dates = await couple.ref.collection("dates").get();
    dates.docs.forEach((d) => { if (onToday(d.get("date"))) events.push(`Today: ${d.get("label")}`); });
    if (events.length === 0) continue;

    for (const u of users) {
      await queue(couple.id, { uid: u.id, senderName: "" }, events[0],
        events.length > 1 ? events.slice(1).join(". ") : "Today's question is about it.", "day");
    }
  }
});

/** Whether [hour] falls in quiet hours from [from] until [to] (wrapping past midnight). */
function isQuiet(hour, from, to) {
  if (from === to) return false;
  return from > to ? hour >= from || hour < to : hour >= from && hour < to;
}

/** Wait this long after the newest item, so a burst (a chat, say) goes out as one push. */
const SETTLE_MS = 10 * 60 * 1000;
/** Never hold a burst longer than this outside quiet hours. */
const MAX_HOLD_MS = 60 * 60 * 1000;
/** At most one push per person in this window; a special day is the exception. */
const MIN_GAP_MS = 30 * 60 * 1000;

/** "Tasia: answered, sent 3 messages, reacted" from queued items. */
function summarize(items) {
  const name = items.map((d) => d.get("from")).find((n) => n) || "";
  const counts = {};
  items.forEach((d) => { const k = d.get("kind") || "update"; counts[k] = (counts[k] || 0) + 1; });
  const words = {
    answered: () => "answered today's question",
    deck: (n) => n > 1 ? `played ${n} decks` : "played a deck",
    question: (n) => n > 1 ? `wrote you ${n} questions` : "wrote you a question",
    reacted: () => "reacted",
    message: (n) => n > 1 ? `sent ${n} messages` : "sent a message",
    mood: () => "set a mood",
    week: () => "named the week",
    day: () => "it's a special day",
    update: (n) => `${n} updates`,
  };
  const parts = Object.entries(counts).map(([k, n]) => (words[k] || words.update)(n));
  return { title: name || "Click Clack", body: parts.join(", ") };
}

/**
 * Every five minutes: for each couple, send what is waiting in the outbox as
 * one push per person, unless it is quiet hours in their home time zone.
 */
exports.flushOutbox = onSchedule({ schedule: "every 5 minutes", timeZone: "UTC" }, async () => {
  const couples = await db.collection("couples").where("endedAt", "==", null).get();
  const now = Date.now();
  for (const couple of couples.docs) {
    const pending = await couple.ref.collection("outbox").get();
    if (pending.empty) continue;

    const zone = couple.get("timeZone") || "America/Chicago";
    const hour = new Date(new Date().toLocaleString("en-US", { timeZone: zone })).getHours();
    const from = couple.get("quietFrom") ?? 20;
    const to = couple.get("quietTo") ?? 8;
    if (isQuiet(hour, from, to)) continue;

    const byPerson = {};
    pending.docs.forEach((d) => { (byPerson[d.get("to")] = byPerson[d.get("to")] || []).push(d); });

    for (const [uid, items] of Object.entries(byPerson)) {
      const times = items.map((d) => d.get("at") || 0);
      const newest = Math.max(...times);
      const oldest = Math.min(...times);
      const special = items.some((d) => d.get("kind") === "day");
      if (!special && now - newest < SETTLE_MS && now - oldest < MAX_HOLD_MS) continue;
      const last = (couple.get("pushedAt") || {})[uid] || 0;
      if (!special && now - last < MIN_GAP_MS) continue;

      const user = await db.doc(`users/${uid}`).get();
      const token = user.get("fcmToken");
      if (token) {
        items.sort((a, b) => (a.get("at") || 0) - (b.get("at") || 0));
        if (items.length === 1) {
          await push({ token }, items[0].get("title"), items[0].get("body"));
        } else {
          const { title, body } = summarize(items);
          await push({ token }, title, body);
        }
        await couple.ref.set({ pushedAt: { [uid]: now } }, { merge: true });
      }
      const batch = db.batch();
      items.forEach((d) => batch.delete(d.ref));
      await batch.commit();
    }
  }
});

exports._isQuiet = isQuiet;

const NAMED = { 1: "paper", 5: "wood", 10: "tin", 15: "crystal", 20: "china", 25: "silver", 30: "pearl", 35: "coral", 40: "ruby", 45: "sapphire", 50: "gold", 60: "diamond" };

/** The relationship milestone on [local] (a Date in the couple's zone), matching Together.on in the app. */
function togetherOn(local, anniversary, marriedOn) {
  const today = Date.UTC(local.getFullYear(), local.getMonth(), local.getDate());
  const named = (iso, what) => {
    if (typeof iso !== "string") return null;
    const [y, m, d] = iso.split("-").map(Number);
    if (m - 1 !== local.getMonth() || d !== local.getDate()) return null;
    const years = local.getFullYear() - y;
    return NAMED[years] ? `${years} ${years === 1 ? "year" : "years"} ${what}: the ${NAMED[years]} anniversary` : null;
  };
  const wedding = named(marriedOn, "married");
  if (wedding) return wedding;
  const together = named(anniversary, "together");
  if (together) return together;
  if (typeof anniversary !== "string") return null;
  const [y, m, d] = anniversary.split("-").map(Number);
  const start = Date.UTC(y, m - 1, d);
  const six = new Date(start); six.setUTCMonth(six.getUTCMonth() + 6);
  if (six.getTime() === today) return "Six months together";
  const days = Math.round((today - start) / 86400000);
  if ([100, 500, 1000, 2000, 5000, 10000].includes(days)) return `${days.toLocaleString("en-US")} days together`;
  return null;
}

exports._togetherOn = togetherOn;

/** A message in a conversation. */
exports.onMessage = onDocumentCreated(
  "couples/{coupleId}/chats/{chatId}/messages/{mid}",
  async (event) => {
    const { coupleId } = event.params;
    const uid = event.data.get("byUid");
    if (!uid) return;
    const target = await partnerOf(coupleId, uid);
    if (!target) return;
    const text = event.data.get("text") || "sent an emote";
    await queue(coupleId, target, `${target.senderName}`, text, "message");
  },
);
