// Pairing, against the real firestore.rules.
//
// Written because pairing was broken for a fortnight by a rule nobody had ever
// run: redeeming an invite writes the *inviter's* user document, and a user
// document was writable only by its owner. It compiled, it deployed, and it
// denied every redemption with "Could not join. Try again in a moment."
//
//   npm install
//   npx firebase emulators:exec --only firestore --project clickclack-rules //     "node rules-test/pairing.test.mjs"
//
import { readFileSync } from 'node:fs'
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing'
import { doc, setDoc, collection, runTransaction, serverTimestamp, Timestamp } from 'firebase/firestore'

const RULES = readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8')

const env = await initializeTestEnvironment({
  projectId: 'clickclack-rules',
  firestore: { rules: RULES, host: '127.0.0.1', port: 8080 },
})

const future = () => Timestamp.fromMillis(Date.now() + 30 * 60000)

async function seed({ code = 'ABCD2345', aliceCoupleId = undefined, expired = false } = {}) {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    const alice = { displayName: 'Alice', colorArgb: 4294901760 }
    if (code) {
      alice.inviteCode = code
      alice.inviteExpiresAt = expired ? Timestamp.fromMillis(Date.now() - 60000) : future()
    }
    if (aliceCoupleId !== undefined) alice.coupleId = aliceCoupleId
    await setDoc(doc(db, 'users/alice'), alice)
    await setDoc(doc(db, 'users/bob'), { displayName: 'Bob', colorArgb: 4278190335 })
    if (code) {
      await setDoc(doc(db, 'invites/' + code), {
        fromUid: 'alice', fromName: 'Alice',
        createdAt: serverTimestamp(), expiresAt: future(),
      })
    }
  })
}

// The same transaction FirestorePairingRepository.redeem runs.
function redeem(db, code, uid, displayName, extraOnTheirDoc = {}) {
  const inviteRef = doc(db, 'invites', code)
  const coupleRef = doc(collection(db, 'couples'))
  return runTransaction(db, async (t) => {
    const invite = await t.get(inviteRef)
    if (!invite.exists()) throw new Error('unknown code')
    const fromUid = invite.data().fromUid
    t.set(coupleRef, { members: [fromUid, uid], createdAt: serverTimestamp(), endedAt: null })
    t.set(doc(db, 'users', uid),
      { displayName, coupleId: coupleRef.id, inviteCode: null, inviteExpiresAt: null },
      { merge: true })
    t.set(doc(db, 'users', fromUid),
      { coupleId: coupleRef.id, inviteCode: null, inviteExpiresAt: null, ...extraOnTheirDoc },
      { merge: true })
    t.delete(inviteRef)
  })
}

const results = []
async function check(name, fn) {
  try { await fn(); results.push(['PASS', name]) }
  catch (e) { results.push(['FAIL', name, String(e && e.message || e)]) }
}

const bob = () => env.authenticatedContext('bob').firestore()
const mallory = () => env.authenticatedContext('mallory').firestore()

await check('bob redeems alice\u0027s code', async () => {
  await seed()
  await assertSucceeds(redeem(bob(), 'ABCD2345', 'bob', 'Bob'))
})

await check('works again after an unpair left coupleId null', async () => {
  await seed({ aliceCoupleId: null })
  await assertSucceeds(redeem(bob(), 'ABCD2345', 'bob', 'Bob'))
})

await check('a stranger cannot join someone who is not inviting', async () => {
  await seed({ code: null })
  await assertFails(setDoc(doc(mallory(), 'users/alice'), { coupleId: 'c_evil' }, { merge: true }))
})

await check('the joiner cannot rename you on the way in', async () => {
  await seed()
  await assertFails(redeem(bob(), 'ABCD2345', 'bob', 'Bob', { displayName: 'Mallory' }))
})

await check('the joiner cannot repaint you on the way in', async () => {
  await seed()
  await assertFails(redeem(bob(), 'ABCD2345', 'bob', 'Bob', { colorArgb: 1 }))
})

await check('cannot steal someone already paired', async () => {
  await seed({ aliceCoupleId: 'c_existing' })
  await assertFails(redeem(bob(), 'ABCD2345', 'bob', 'Bob'))
})

await check('cannot be pointed at a couple with a bogus code', async () => {
  await seed()
  await assertFails(setDoc(doc(mallory(), 'users/bob'), { coupleId: 'c_evil' }, { merge: true }))
})

await check('a stranger cannot join you once your code has expired', async () => {
  await seed({ expired: true })
  await assertFails(setDoc(doc(mallory(), 'users/alice'), { coupleId: 'c_evil' }, { merge: true }))
})

await check('you can still write your own document', async () => {
  await seed({ code: null })
  await assertSucceeds(setDoc(doc(bob(), 'users/bob'), { colorArgb: 7 }, { merge: true }))
})

await env.cleanup()
for (const r of results) console.log(r[0].padEnd(5), r[1], r[2] ? '\n      ' + r[2] : '')
process.exit(results.some((r) => r[0] === 'FAIL') ? 1 : 0)
