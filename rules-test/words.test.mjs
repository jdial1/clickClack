// onWords against the Firestore emulator: the week's name is sealed per person
// and lifted by the function once both halves exist.
//
//   npx firebase emulators:exec --only firestore --project clickclack-rules "node words.test.mjs"
import { createRequire } from 'node:module'

process.env.GCLOUD_PROJECT = 'clickclack-rules'
process.env.FIRESTORE_EMULATOR_HOST ||= '127.0.0.1:8080'
const require = createRequire(new URL('../functions/index.js', import.meta.url))
const fns = require('./index.js')
const { getFirestore } = require('firebase-admin/firestore')
const db = getFirestore()

const results = []
const check = (name, ok) => results.push([ok ? 'PASS' : 'FAIL', name])

const plate = db.doc('couples/c1/plates/plate_2026-09-06')
const words = (uid) => plate.collection('words').doc(uid)

async function seed(plateData = {}) {
  await db.recursiveDelete(db.doc('couples/c1'))
  await db.doc('couples/c1').set({ members: ['alice', 'bob'], endedAt: null })
  await db.doc('users/alice').set({ displayName: 'Alice', coupleId: 'c1' })
  await db.doc('users/bob').set({ displayName: 'Bob', coupleId: 'c1' })
  await plate.set({ startedOn: '2026-09-06', completedOn: '2026-09-13', ...plateData })
}

// What the trigger receives when a words document is created.
async function pick(uid, describing, thing) {
  await words(uid).set({ describing, thing })
  await fns.onWords.run({ params: { coupleId: 'c1', plateId: plate.id, uid }, data: await words(uid).get() })
}

// One pick: nothing is lifted, so the partner still can't see it.
await seed()
await pick('alice', 'Wobbly', 'Castle')
check('one pick leaves the name sealed', (await plate.get()).get('words') === undefined)

// The second pick lifts the seal for both at once.
await pick('bob', 'Cosmic', 'Pretzel')
const both = (await plate.get()).get('words') || {}
check('the second pick lifts both halves onto the plate',
  both.alice?.describing === 'Wobbly' && both.alice?.thing === 'Castle'
  && both.bob?.describing === 'Cosmic' && both.bob?.thing === 'Pretzel')

// A pick made before the seal moved here, still on the plate, counts as the other half.
await seed({ words: { alice: { describing: 'Quiet', thing: 'Harbour' } } })
await pick('bob', 'Sunny', 'Porch')
const mixed = (await plate.get()).get('words') || {}
check('an old-style pick on the plate meets a new one',
  mixed.alice?.thing === 'Harbour' && mixed.bob?.thing === 'Porch')

for (const r of results) console.log(r[0].padEnd(5), r[1])
process.exit(results.some((r) => r[0] === 'FAIL') ? 1 : 0)
