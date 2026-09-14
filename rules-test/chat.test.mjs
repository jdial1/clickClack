// Conversations and special-day photos, against the real firestore.rules.
//
//   npx firebase emulators:exec --only firestore --project clickclack-rules "node chat.test.mjs"
import { readFileSync } from 'node:fs'
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing'
import { doc, setDoc, updateDoc, deleteDoc, getDoc } from 'firebase/firestore'

const RULES = readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8')
const env = await initializeTestEnvironment({
  projectId: 'clickclack-chat',
  firestore: { rules: RULES, host: '127.0.0.1', port: 8080 },
})

const CID = 'c1'
const as = (uid) => env.authenticatedContext(uid).firestore()
const msg = (db, id = 'm1') => doc(db, 'couples', CID, 'chats', 'day_2026-09-14', 'messages', id)
const photo = (db, id = 'p1') => doc(db, 'couples', CID, 'photos', id)

async function seed({ ended = false, existing = null, pic = null } = {}) {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    await setDoc(doc(db, 'couples', CID), { members: ['alice', 'bob'], endedAt: ended ? new Date() : null })
    if (existing) await setDoc(msg(db), existing)
    if (pic) await setDoc(photo(db), pic)
  })
}

const results = []
async function check(name, fn) {
  try { await fn(); results.push(['PASS', name]) }
  catch (e) { results.push(['FAIL', name, String((e && e.message) || e)]) }
}

const hi = { byUid: 'alice', text: 'That was sweet', emote: null, at: new Date() }
const pic = { byUid: 'alice', date: '2026-09-14', label: 'Anniversary', data: 'abc' }

await check('a member sends a line as themselves', async () => {
  await seed()
  await assertSucceeds(setDoc(msg(as('alice')), hi))
})

await check('an emote needs no text', async () => {
  await seed()
  await assertSucceeds(setDoc(msg(as('bob')), { byUid: 'bob', text: null, emote: 'HEART', at: new Date() }))
})

await check('nobody writes under the other name', async () => {
  await seed()
  await assertFails(setDoc(msg(as('bob')), hi))
})

await check('an outsider can neither send nor read', async () => {
  await seed({ existing: hi })
  await assertFails(setDoc(msg(as('mallory'), 'm2'), { ...hi, byUid: 'mallory' }))
  await assertFails(getDoc(msg(as('mallory'))))
})

await check('a line is capped at 500 characters', async () => {
  await seed()
  await assertFails(setDoc(msg(as('alice')), { ...hi, text: 'x'.repeat(501) }))
})

await check('a sent line is never edited or deleted', async () => {
  await seed({ existing: hi })
  await assertFails(updateDoc(msg(as('alice')), { text: 'edited' }))
  await assertFails(deleteDoc(msg(as('alice'))))
})

await check('nothing new once the couple has ended', async () => {
  await seed({ ended: true })
  await assertFails(setDoc(msg(as('alice')), hi))
})

await check('a member keeps a photo as themselves', async () => {
  await seed()
  await assertSucceeds(setDoc(photo(as('alice')), pic))
  await assertSucceeds(getDoc(photo(as('bob'))))
})

await check('a photo must be small', async () => {
  await seed()
  await assertFails(setDoc(photo(as('alice')), { ...pic, data: 'x'.repeat(900000) }))
})

await check('only the author removes a photo', async () => {
  await seed({ pic })
  await assertFails(deleteDoc(photo(as('bob'))))
  await assertSucceeds(deleteDoc(photo(as('alice'))))
})

await env.cleanup()
for (const r of results) console.log(r[0].padEnd(5), r[1], r[2] ? '\n      ' + r[2] : '')
process.exit(results.some((r) => r[0] === 'FAIL') ? 1 : 0)
