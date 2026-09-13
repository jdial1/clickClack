// Sealed plates take one line each about the week, and nothing else.
//
//   npx firebase emulators:exec --only firestore --project clickclack-rules "node plates.test.mjs"
import { readFileSync } from 'node:fs'
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing'
import { doc, setDoc } from 'firebase/firestore'

const RULES = readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8')
const env = await initializeTestEnvironment({
  projectId: 'clickclack-plates',
  firestore: { rules: RULES, host: '127.0.0.1', port: 8080 },
})

const CID = 'c1'
const plate = (db) => doc(db, 'couples', CID, 'plates', 'plate_2026-09-06')
const as = (uid) => env.authenticatedContext(uid).firestore()

async function seed(data) {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    await setDoc(doc(db, 'couples', CID), { members: ['alice', 'bob'], endedAt: null })
    await setDoc(plate(db), data)
  })
}

const results = []
async function check(name, fn) {
  try { await fn(); results.push(['PASS', name]) }
  catch (e) { results.push(['FAIL', name, String((e && e.message) || e)]) }
}

const sealed = { startedOn: '2026-09-06', completedOn: '2026-09-13', blocks: [], placed: [] }

await check('you may write your line on a sealed week', async () => {
  await seed(sealed)
  await assertSucceeds(setDoc(plate(as('alice')), { notes: { alice: 'The week we painted the hall' } }, { merge: true }))
})

await check('you may not write their line', async () => {
  await seed(sealed)
  await assertFails(setDoc(plate(as('alice')), { notes: { bob: 'Not yours' } }, { merge: true }))
})

await check('a sealed week still refuses bricks', async () => {
  await seed(sealed)
  await assertFails(setDoc(plate(as('alice')), { blocks: [{ id: 'x' }] }, { merge: true }))
})

await check('a line cannot smuggle a brick in with it', async () => {
  await seed(sealed)
  await assertFails(setDoc(plate(as('alice')), { notes: { alice: 'Hi' }, blocks: [{ id: 'x' }] }, { merge: true }))
})

await check('you may pick your two words for a sealed week', async () => {
  await seed(sealed)
  await assertSucceeds(setDoc(plate(as('alice')), { words: { alice: { describing: 'Wobbly', thing: 'Castle' } } }, { merge: true }))
})

await check('you may not pick their words', async () => {
  await seed(sealed)
  await assertFails(setDoc(plate(as('alice')), { words: { bob: { describing: 'Wobbly', thing: 'Castle' } } }, { merge: true }))
})

await check('words cannot carry anything else', async () => {
  await seed(sealed)
  await assertFails(setDoc(plate(as('alice')), { words: { alice: { describing: 'Wobbly', thing: 'Castle', extra: 1 } } }, { merge: true }))
})

await check('an open week still takes bricks', async () => {
  await seed({ ...sealed, completedOn: null })
  await assertSucceeds(setDoc(plate(as('alice')), { blocks: [{ id: 'x' }] }, { merge: true }))
})

await env.cleanup()
for (const r of results) console.log(r[0].padEnd(5), r[1], r[2] ? '\n      ' + r[2] : '')
process.exit(results.some((r) => r[0] === 'FAIL') ? 1 : 0)
