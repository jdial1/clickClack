// Today's question, against the real firestore.rules.
//
// Written because the app quietly could not do this at all: it watched the
// answers *collection*, and a query over a collection where one document is
// deliberately unreadable is refused outright -- even when the only document
// in it is your own. The listener died on its first snapshot, so answering
// wrote the document and then nothing on screen moved.
//
//   npm install
//   npx firebase emulators:exec --only firestore --project clickclack-rules \
//     "node rules-test/day.test.mjs"
import { readFileSync } from 'node:fs'
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing'
import { doc, getDoc, setDoc, getDocs, collection, serverTimestamp, arrayUnion } from 'firebase/firestore'

const RULES = readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8')
const env = await initializeTestEnvironment({
  projectId: 'clickclack-day',
  firestore: { rules: RULES, host: '127.0.0.1', port: 8080 },
})

const CID = 'c1', DAY = '2026-09-11'
const dayRef = (db) => doc(db, 'couples', CID, 'days', DAY)
const answerRef = (db, uid) => doc(db, 'couples', CID, 'days', DAY, 'answers', uid)
const answers = (db) => collection(db, 'couples', CID, 'days', DAY, 'answers')

async function seed({ dayDoc = null, given = {} } = {}) {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    await setDoc(doc(db, 'couples', CID), { members: ['alice', 'bob'], endedAt: null })
    if (dayDoc) await setDoc(dayRef(db), dayDoc)
    for (const [uid, a] of Object.entries(given)) await setDoc(answerRef(db, uid), a)
  })
}

const as = (uid) => env.authenticatedContext(uid).firestore()
const results = []
async function check(name, fn) {
  try { await fn(); results.push(['PASS', name]) }
  catch (e) { results.push(['FAIL', name, String(e && e.message || e)]) }
}

// What the app actually does when you tap "Seal it".
async function submit(uid, values) {
  const db = as(uid)
  await setDoc(dayRef(db),
    { questionId: 'q', revealed: false, submitted: arrayUnion(uid) }, { merge: true })
  await setDoc(answerRef(db, uid),
    { values, guess: null, submittedAt: serverTimestamp() })
}

await check('both answer, the seal lifts, and each can read the other', async () => {
  await seed()
  await assertSucceeds(submit('alice', ['mine']))
  await assertSucceeds(submit('bob', ['theirs']))
  // Whichever phone notices the second name in `submitted` flips it.
  await assertSucceeds(setDoc(dayRef(as('alice')), { revealed: true }, { merge: true }))
  await assertSucceeds(getDoc(answerRef(as('alice'), 'bob')))
  await assertSucceeds(getDoc(answerRef(as('bob'), 'alice')))
})

await check('your own answer is readable before the day document exists', async () => {
  await seed({ given: { alice: { values: ['x'] } } })
  await assertSucceeds(getDoc(answerRef(as('alice'), 'alice')))
})

await check('your own answer is readable while the day is sealed', async () => {
  await seed({ dayDoc: { questionId: 'q', revealed: false }, given: { alice: { values: ['x'] } } })
  await assertSucceeds(getDoc(answerRef(as('alice'), 'alice')))
})

await check("their answer stays unreadable while the day is sealed", async () => {
  await seed({ dayDoc: { questionId: 'q', revealed: false }, given: { bob: { values: ['y'] } } })
  await assertFails(getDoc(answerRef(as('alice'), 'bob')))
})

// The bug, kept as a test: this is what the app used to do every time it
// opened, and it is refused in every state the day can be in.
await check('listing the answers is refused while sealed, even with only yours in', async () => {
  await seed({ dayDoc: { questionId: 'q', revealed: false }, given: { alice: { values: ['x'] } } })
  await assertFails(getDocs(answers(as('alice'))))
})

// The stranded case: a day document written before `submitted` existed can
// never come to hold both names, so a client that waits for it waits forever.
// The rules do not care about that list at all -- which is why asking them is
// the thing that gets two people unstuck.
await check('a day with no submitted list still opens once both answers are in', async () => {
  await seed({
    dayDoc: { questionId: 'q', revealed: false },
    given: { alice: { values: ['mine'] }, bob: { values: ['theirs'] } },
  })
  await assertSucceeds(setDoc(dayRef(as('alice')), { revealed: true }, { merge: true }))
})

await check('a day with no submitted list stays shut on one answer', async () => {
  await seed({
    dayDoc: { questionId: 'q', revealed: false },
    given: { alice: { values: ['mine'] } },
  })
  await assertFails(setDoc(dayRef(as('alice')), { revealed: true }, { merge: true }))
})

await check('a phone that lies in submitted still cannot open the day', async () => {
  await seed()
  await assertSucceeds(submit('alice', ['mine']))
  const db = as('alice')
  // The lie itself is now refused: nobody writes the other person's name.
  await assertFails(setDoc(dayRef(db), { submitted: arrayUnion('bob') }, { merge: true }))
  // And even if a list said so, Bob has not answered, so the day stays shut.
  await assertFails(setDoc(dayRef(db), { revealed: true }, { merge: true }))
})

// The one bit of a day that is written the morning *after* it opened. It goes
// on the day document beside the reactions rather than in the sealed
// subcollection, because there is nothing left to spoil once both answers are
// in -- and additively, so two phones marking the same day cannot clobber each
// other.
await check('either of you may say a revealed day came up again', async () => {
  await seed({
    dayDoc: { revealed: true },
    given: { alice: { values: ['a'] }, bob: { values: ['b'] } },
  })
  await assertSucceeds(
    setDoc(dayRef(as('alice')), { talkedAbout: arrayUnion('alice') }, { merge: true }),
  )
  await assertSucceeds(
    setDoc(dayRef(as('bob')), { talkedAbout: arrayUnion('bob') }, { merge: true }),
  )
})

await check('an outsider cannot mark a day as talked about', async () => {
  await seed({
    dayDoc: { revealed: true },
    given: { alice: { values: ['a'] }, bob: { values: ['b'] } },
  })
  await assertFails(
    setDoc(dayRef(as('mallory')), { talkedAbout: arrayUnion('mallory') }, { merge: true }),
  )
})

// Marking is a merge onto the day document, and the day rule refuses any write
// that leaves `revealed` true without both answers behind it. A sealed day
// therefore cannot be marked, which is correct: there is no conversation to
// have had about a question only one of you has answered.
await check('a sealed day cannot be marked as talked about', async () => {
  await seed({ dayDoc: { revealed: false }, given: { alice: { values: ['a'] } } })
  await assertFails(
    setDoc(dayRef(as('alice')), { revealed: true, talkedAbout: arrayUnion('alice') }, { merge: true }),
  )
})

// ---- the forecast, saved before anyone answers ---------------------------------

await check('a member may save the forecast on a day nobody has answered', async () => {
  await seed()
  await assertSucceeds(setDoc(dayRef(as('alice')), { weather: { tempF: 84, summary: 'Sunny' } }, { merge: true }))
})

await check('saving the forecast cannot open the day', async () => {
  await seed()
  await assertFails(setDoc(dayRef(as('alice')), { weather: { tempF: 84 }, revealed: true }, { merge: true }))
})

// ---- the seal cannot be sidestepped -----------------------------------------

await check('a day cannot be created already open', async () => {
  await seed()
  await assertFails(setDoc(dayRef(as('alice')), { questionId: 'q', revealed: true }))
})

await check('an opened day cannot be shut again', async () => {
  await seed({
    dayDoc: { questionId: 'q', revealed: true },
    given: { alice: { values: ['a'] } },
  })
  await assertFails(setDoc(dayRef(as('alice')), { revealed: false }, { merge: true }))
})

await check('an ended couple cannot be brought back', async () => {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'couples', CID), { members: ['alice', 'bob'], endedAt: new Date() })
  })
  await assertFails(setDoc(doc(as('alice'), 'couples', CID), { endedAt: null }, { merge: true }))
})

await check('a live couple can still be ended by either of you', async () => {
  await seed()
  await assertSucceeds(setDoc(doc(as('bob'), 'couples', CID), { endedAt: new Date() }, { merge: true }))
})

// ---- your half only ---------------------------------------------------------

const opened = { questionId: 'q', revealed: true, submitted: ['alice', 'bob'] }
const bothAnswered = { alice: { values: ['a'] }, bob: { values: ['b'] } }

await check('you may react, write a line back and say "we did" as yourself', async () => {
  await seed({ dayDoc: opened, given: bothAnswered })
  await assertSucceeds(setDoc(dayRef(as('alice')), {
    reactions: { alice: 'LOVE' }, notes: { alice: 'Hi' }, talkedAbout: arrayUnion('alice'), askAgain: arrayUnion('alice'),
  }, { merge: true }))
})

await check('you may not react or write a line back for them', async () => {
  await seed({ dayDoc: opened, given: bothAnswered })
  await assertFails(setDoc(dayRef(as('alice')), { reactions: { bob: 'LOVE' } }, { merge: true }))
  await assertFails(setDoc(dayRef(as('alice')), { notes: { bob: 'Not yours' } }, { merge: true }))
  await assertFails(setDoc(dayRef(as('alice')), { moodReplies: { bob: 'Not yours' } }, { merge: true }))
})

await check('you may not add or remove their name from a list', async () => {
  await seed({ dayDoc: { ...opened, talkedAbout: ['bob'], askAgain: ['bob'] }, given: bothAnswered })
  await assertFails(setDoc(dayRef(as('alice')), { talkedAbout: arrayUnion('bob', 'alice'), askAgain: [] }, { merge: true }))
  await assertFails(setDoc(dayRef(as('alice')), { askAgain: [] }, { merge: true }))
  await assertSucceeds(setDoc(dayRef(as('alice')), { askAgain: arrayUnion('alice') }, { merge: true }))
})

await check('nobody submits for the other person', async () => {
  await seed()
  await assertFails(setDoc(dayRef(as('alice')), { questionId: 'q', revealed: false, submitted: arrayUnion('bob') }, { merge: true }))
})

await check("tomorrow's theme is signed by whoever picks it", async () => {
  await seed({ dayDoc: opened, given: bothAnswered })
  await assertFails(setDoc(dayRef(as('alice')), { nextTheme: 'Food', nextThemeBy: 'bob' }, { merge: true }))
  await assertSucceeds(setDoc(dayRef(as('alice')), { nextTheme: 'Food', nextThemeBy: 'alice' }, { merge: true }))
})

await env.cleanup()
for (const r of results) console.log(r[0].padEnd(5), r[1], r[2] ? '\n      ' + r[2] : '')
process.exit(results.some((r) => r[0] === 'FAIL') ? 1 : 0)
