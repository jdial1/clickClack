// Whole-instrument sittings, against the real firestore.rules.
//
// The seal matters more here than anywhere else in the app. A love-language
// sitting is forty ratings of what you want and what you give, and the finding
// it is built on is the *gap* between one person's wants and the other's
// habits -- which is worth nothing if either of you could read the other's
// answers while giving your own.
//
//   npm install
//   npx firebase emulators:exec --only firestore --project clickclack-rules \
//     "node rules-test/instrument.test.mjs"
import { readFileSync } from 'node:fs'
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing'
import { doc, getDoc, setDoc, deleteDoc, getDocs, collection, serverTimestamp, arrayUnion } from 'firebase/firestore'

const RULES = readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8')
const env = await initializeTestEnvironment({
  projectId: 'clickclack-instrument',
  firestore: { rules: RULES, host: '127.0.0.1', port: 8080 },
})

const CID = 'c1', KIT = 'oejts'
const kitRef = (db) => doc(db, 'couples', CID, 'instruments', KIT)
const runRef = (db, uid) => doc(db, 'couples', CID, 'instruments', KIT, 'runs', uid)
const runs = (db) => collection(db, 'couples', CID, 'instruments', KIT, 'runs')

const THIRTY_TWO = Array.from({ length: 32 }, () => 3)

async function seed({ kitDoc = null, given = {}, ended = null } = {}) {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    await setDoc(doc(db, 'couples', CID), { members: ['alice', 'bob'], endedAt: ended })
    if (kitDoc) await setDoc(kitRef(db), kitDoc)
    for (const [uid, r] of Object.entries(given)) await setDoc(runRef(db, uid), r)
  })
}

const as = (uid) => env.authenticatedContext(uid).firestore()
const results = []
async function check(name, fn) {
  try { await fn(); results.push(['PASS', name]) }
  catch (e) { results.push(['FAIL', name, String((e && e.message) || e)]) }
}

// Exactly what the app does on "Seal it".
async function submit(db, uid, responses = THIRTY_TWO) {
  await setDoc(
    kitRef(db),
    { revealed: false, takenOn: '2026-09-12', submitted: arrayUnion(uid) },
    { merge: true },
  )
  await setDoc(runRef(db, uid), { responses, completedAt: serverTimestamp() })
}

await check('a member may submit their own sitting', async () => {
  await seed()
  await assertSucceeds(submit(as('alice'), 'alice'))
})

await check('nobody may write a run under another uid', async () => {
  await seed()
  await assertFails(setDoc(runRef(as('alice'), 'bob'), { responses: THIRTY_TWO }))
})

await check('an outsider may not submit at all', async () => {
  await seed()
  await assertFails(submit(as('mallory'), 'mallory'))
})

await check('you may always read back your own run', async () => {
  await seed({ kitDoc: { revealed: false }, given: { alice: { responses: THIRTY_TWO } } })
  await assertSucceeds(getDoc(runRef(as('alice'), 'alice')))
})

// The seal itself.
await check('their run is unreadable while sealed', async () => {
  await seed({ kitDoc: { revealed: false }, given: { bob: { responses: THIRTY_TWO } } })
  await assertFails(getDoc(runRef(as('alice'), 'bob')))
})

await check('their run opens once revealed', async () => {
  await seed({
    kitDoc: { revealed: true },
    given: { alice: { responses: THIRTY_TWO }, bob: { responses: THIRTY_TWO } },
  })
  await assertSucceeds(getDoc(runRef(as('alice'), 'bob')))
})

// The reason the app watches two named documents rather than the collection:
// a query over a collection holding one unreadable document is refused whole,
// which is what silently broke the day screen once already.
await check('listing the runs is refused while sealed', async () => {
  await seed({ kitDoc: { revealed: false }, given: { alice: { responses: THIRTY_TWO } } })
  await assertFails(getDocs(runs(as('alice'))))
})

// The client asks to open it and lets the rules decide, so this is the check
// that stops a phone simply declaring itself revealed.
await check('revealed cannot be set with only one run in', async () => {
  await seed({ kitDoc: { revealed: false }, given: { alice: { responses: THIRTY_TWO } } })
  await assertFails(setDoc(kitRef(as('alice')), { revealed: true }, { merge: true }))
})

await check('revealed may be set once both runs exist', async () => {
  await seed({
    kitDoc: { revealed: false },
    given: { alice: { responses: THIRTY_TWO }, bob: { responses: THIRTY_TWO } },
  })
  await assertSucceeds(setDoc(kitRef(as('alice')), { revealed: true }, { merge: true }))
})

// A sitting is a statement made under seal. Editing it after the fact would
// let somebody read their partner's answers and then change their own.
await check('a submitted run cannot be edited', async () => {
  await seed({ kitDoc: { revealed: true }, given: { alice: { responses: THIRTY_TWO } } })
  await assertFails(setDoc(runRef(as('alice'), 'alice'), { responses: Array(32).fill(5) }))
})

await check('a sitting cannot be created already open', async () => {
  await seed()
  await assertFails(setDoc(kitRef(as('alice')), { revealed: true }))
})

await check('an ended couple cannot start a new sitting', async () => {
  await seed({ ended: new Date() })
  await assertFails(submit(as('alice'), 'alice'))
})

// ---- drafts: a half-finished sitting -------------------------------------
//
// The one thing that must never leak. A sealed run at least has a moment when
// it is meant to open; a draft is answers somebody is still giving, and there
// is no point at which the other half of the couple is entitled to them.
const draftRef = (db, uid) => doc(db, 'couples', CID, 'instruments', KIT, 'drafts', uid)
const drafts = (db) => collection(db, 'couples', CID, 'instruments', KIT, 'drafts')

async function seedDraft(uid, responses) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(draftRef(ctx.firestore(), uid), { responses })
  })
}

await check('you may save your own draft', async () => {
  await seed()
  await assertSucceeds(setDoc(draftRef(as('alice'), 'alice'), { responses: [1, 2, 3] }))
})

await check('you may rewrite your own draft, because that is what resuming is', async () => {
  await seed()
  await seedDraft('alice', [1, 2, 3])
  await assertSucceeds(setDoc(draftRef(as('alice'), 'alice'), { responses: [1, 2, 3, 4] }))
})

await check('you may delete your own draft, because sealing consumes it', async () => {
  await seed()
  await seedDraft('alice', [1, 2, 3])
  await assertSucceeds(deleteDoc(draftRef(as('alice'), 'alice')))
})

await check('their draft is unreadable, sealed or not', async () => {
  await seed()
  await seedDraft('bob', [5, 5, 5])
  await assertFails(getDoc(draftRef(as('alice'), 'bob')))
})

// Even once the instrument has opened. A draft has no later moment at which it
// becomes fair game -- if it still exists after a reveal it is a leftover, and
// a leftover is not an invitation.
await check('their draft stays unreadable even after the seal lifts', async () => {
  await seed({
    kitDoc: { revealed: true },
    given: { alice: { responses: THIRTY_TWO }, bob: { responses: THIRTY_TWO } },
  })
  await seedDraft('bob', [5, 5, 5])
  await assertFails(getDoc(draftRef(as('alice'), 'bob')))
})

await check('nobody may write a draft under another uid', async () => {
  await seed()
  await assertFails(setDoc(draftRef(as('alice'), 'bob'), { responses: [5, 5, 5] }))
})

await check('listing the drafts is refused', async () => {
  await seed()
  await seedDraft('alice', [1])
  await assertFails(getDocs(drafts(as('alice'))))
})

await check('an outsider may not touch drafts at all', async () => {
  await seed()
  await seedDraft('alice', [1])
  await assertFails(getDoc(draftRef(as('mallory'), 'alice')))
  await assertFails(setDoc(draftRef(as('mallory'), 'mallory'), { responses: [1] }))
})

await check('an ended couple cannot save a draft', async () => {
  await seed({ ended: new Date() })
  await assertFails(setDoc(draftRef(as('alice'), 'alice'), { responses: [1] }))
})

// ---- progress: the half that is meant to be shared ------------------------
//
// A count, not the answers. Both of you need it -- it is what turns "neither of
// us has started" into "we have both finished round one" -- and it gives away
// nothing about what anybody said.
await check('a progress count is readable by both', async () => {
  await seed({ kitDoc: { revealed: false, progress: { alice: 20 } } })
  await assertSucceeds(getDoc(kitRef(as('bob'))))
})

await check('a member may write their own progress without opening the seal', async () => {
  await seed({ kitDoc: { revealed: false } })
  await assertSucceeds(
    setDoc(kitRef(as('alice')), { revealed: false, progress: { alice: 12 } }, { merge: true }),
  )
})

await check('writing progress cannot smuggle the seal open', async () => {
  await seed({ kitDoc: { revealed: false } })
  await assertFails(
    setDoc(kitRef(as('alice')), { revealed: true, progress: { alice: 40 } }, { merge: true }),
  )
})

await env.cleanup()
for (const [state, name, err] of results) {
  console.log(`${state}  ${name}${err ? `\n      ${err}` : ''}`)
}
const failed = results.filter(([s]) => s === 'FAIL').length
console.log(`\n${results.length - failed}/${results.length} passed`)
process.exit(failed ? 1 : 0)
