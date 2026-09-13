// Special dates and the profile fields, against the real firestore.rules.
//
//   npx firebase emulators:exec --only firestore --project clickclack-rules "node dates.test.mjs"
import { readFileSync } from 'node:fs'
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing'
import { doc, setDoc, updateDoc, deleteDoc } from 'firebase/firestore'

const RULES = readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8')
const env = await initializeTestEnvironment({
  projectId: 'clickclack-dates',
  firestore: { rules: RULES, host: '127.0.0.1', port: 8080 },
})

const CID = 'c1'
const date = (db, id = 'd1') => doc(db, 'couples', CID, 'dates', id)
const as = (uid) => env.authenticatedContext(uid).firestore()

async function seed(existing = null) {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    await setDoc(doc(db, 'couples', CID), { members: ['alice', 'bob'], endedAt: null })
    await setDoc(doc(db, 'users', 'alice'), { displayName: 'Alice' })
    await setDoc(doc(db, 'users', 'bob'), { displayName: 'Bob' })
    if (existing) await setDoc(date(db), existing)
  })
}

const results = []
async function check(name, fn) {
  try { await fn(); results.push(['PASS', name]) }
  catch (e) { results.push(['FAIL', name, String((e && e.message) || e)]) }
}

const first = { label: 'First date', date: '2012-04-20', byUid: 'alice' }

await check('a member adds a date as themselves', async () => {
  await seed()
  await assertSucceeds(setDoc(date(as('alice')), first))
})

await check('nobody adds a date under the other name', async () => {
  await seed()
  await assertFails(setDoc(date(as('bob')), first))
})

await check('an outsider cannot add a date', async () => {
  await seed()
  await assertFails(setDoc(date(as('mallory')), { ...first, byUid: 'mallory' }))
})

await check('a date is never edited in place', async () => {
  await seed(first)
  await assertFails(updateDoc(date(as('alice')), { label: 'Something else' }))
})

await check('either of you may remove a date', async () => {
  await seed(first)
  await assertSucceeds(deleteDoc(date(as('bob'))))
})

await check('you may set your own gender and birthday', async () => {
  await seed()
  await assertSucceeds(setDoc(doc(as('alice'), 'users', 'alice'), { gender: 'Woman', birthday: '1990-01-02' }, { merge: true }))
})

await check('you may not set theirs', async () => {
  await seed()
  await assertFails(setDoc(doc(as('alice'), 'users', 'bob'), { birthday: '1990-01-02' }, { merge: true }))
})

await check('either of you may set the relationship and your hidden topics', async () => {
  await seed()
  await assertSucceeds(setDoc(doc(as('bob'), 'couples', CID),
    { relationship: 'MARRIED', marriedOn: '2015-06-01', hiddenTopics: { bob: ['MONEY'] } }, { merge: true }))
})

await env.cleanup()
for (const r of results) console.log(r[0].padEnd(5), r[1], r[2] ? '\n      ' + r[2] : '')
process.exit(results.some((r) => r[0] === 'FAIL') ? 1 : 0)
