// The shared bucket list, against the real firestore.rules.
//
//   npx firebase emulators:exec --only firestore --project clickclack-rules "node bucket.test.mjs"
import { readFileSync } from 'node:fs'
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing'
import { doc, setDoc, updateDoc, deleteDoc } from 'firebase/firestore'

const RULES = readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8')
const env = await initializeTestEnvironment({
  projectId: 'clickclack-bucket',
  firestore: { rules: RULES, host: '127.0.0.1', port: 8080 },
})

const CID = 'c1'
const item = (db, id = 'i1') => doc(db, 'couples', CID, 'bucket', id)
const as = (uid) => env.authenticatedContext(uid).firestore()

async function seed({ existing = null, ended = null } = {}) {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    await setDoc(doc(db, 'couples', CID), { members: ['alice', 'bob'], endedAt: ended })
    if (existing) await setDoc(item(db), existing)
  })
}

const results = []
async function check(name, fn) {
  try { await fn(); results.push(['PASS', name]) }
  catch (e) { results.push(['FAIL', name, String((e && e.message) || e)]) }
}

const open = { text: 'See the northern lights', group: 'GETAWAYS', byUid: 'alice', doneOn: null }

await check('a member adds an item as themselves', async () => {
  await seed()
  await assertSucceeds(setDoc(item(as('alice')), open))
})

await check('nobody adds an item under the other name', async () => {
  await seed()
  await assertFails(setDoc(item(as('bob')), open))
})

await check('an item cannot be added already done', async () => {
  await seed()
  await assertFails(setDoc(item(as('alice')), { ...open, doneOn: '2026-09-13' }))
})

await check('an outsider cannot add or read', async () => {
  await seed({ existing: open })
  await assertFails(setDoc(item(as('mallory'), 'i2'), { ...open, byUid: 'mallory' }))
})

await check('either of you may tick it off', async () => {
  await seed({ existing: open })
  await assertSucceeds(updateDoc(item(as('bob')), { doneOn: '2026-09-13', doneBy: 'bob' }))
})

await check('a ticked item stays ticked', async () => {
  await seed({ existing: { ...open, doneOn: '2026-09-13' } })
  await assertFails(updateDoc(item(as('alice')), { doneOn: null }))
})

await check('the text cannot be rewritten', async () => {
  await seed({ existing: open })
  await assertFails(updateDoc(item(as('bob')), { text: 'Something else' }))
})

await check('only whoever added it may remove it, before it is done', async () => {
  await seed({ existing: open })
  await assertFails(deleteDoc(item(as('bob'))))
  await assertSucceeds(deleteDoc(item(as('alice'))))
})

await check('a done item cannot be removed', async () => {
  await seed({ existing: { ...open, doneOn: '2026-09-13' } })
  await assertFails(deleteDoc(item(as('alice'))))
})

await check('an ended couple cannot add to the list', async () => {
  await seed({ ended: new Date() })
  await assertFails(setDoc(item(as('alice')), open))
})

await env.cleanup()
for (const r of results) console.log(r[0].padEnd(5), r[1], r[2] ? '\n      ' + r[2] : '')
process.exit(results.some((r) => r[0] === 'FAIL') ? 1 : 0)
