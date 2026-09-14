// Decks: sealed by default, open when created with sealed: false.
//
//   npx firebase emulators:exec --only firestore --project clickclack-rules "node rules-test/deck.test.mjs"
import { readFileSync } from 'node:fs'
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing'
import { doc, getDoc, setDoc } from 'firebase/firestore'

const RULES = readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8')
const env = await initializeTestEnvironment({
  projectId: 'clickclack-decks',
  firestore: { rules: RULES, host: '127.0.0.1', port: 8080 },
})

const CID = 'c1'
const as = (uid) => env.authenticatedContext(uid).firestore()
const deck = (db) => doc(db, 'couples', CID, 'quizzes', 'd1')
const run = (db, uid) => doc(db, 'couples', CID, 'quizzes', 'd1', 'runs', uid)

async function seed(deckData) {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    await setDoc(doc(db, 'couples', CID), { members: ['alice', 'bob'], endedAt: null })
    await setDoc(deck(db), deckData)
    await setDoc(run(db, 'bob'), { answers: { q1: ['A'] } })
  })
}

const results = []
async function check(name, fn) {
  try { await fn(); results.push(['PASS', name]) }
  catch (e) { results.push(['FAIL', name, String((e && e.message) || e)]) }
}

await check('a sealed deck hides their run', async () => {
  await seed({ revealed: false })
  await assertFails(getDoc(run(as('alice'), 'bob')))
})

await check('an open deck shows their run before you play', async () => {
  await seed({ revealed: false, sealed: false })
  await assertSucceeds(getDoc(run(as('alice'), 'bob')))
})

await check('a sealed deck cannot be opened by flipping sealed', async () => {
  await seed({ revealed: false, sealed: true })
  await assertFails(setDoc(deck(as('alice')), { sealed: false }, { merge: true }))
})

await check('an open deck still takes its submitted list', async () => {
  await seed({ revealed: false, sealed: false })
  await assertSucceeds(setDoc(deck(as('alice')), { revealed: false, sealed: false, submitted: ['alice'] }, { merge: true }))
})

await env.cleanup()
for (const r of results) console.log(r[0].padEnd(5), r[1], r[2] ? '\n      ' + r[2] : '')
process.exit(results.some((r) => r[0] === 'FAIL') ? 1 : 0)
