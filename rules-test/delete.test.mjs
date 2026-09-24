// deleteAccount against the Firestore and Auth emulators.
//
//   npx firebase emulators:exec --only firestore,auth --project clickclack-rules "node delete.test.mjs"
import { createRequire } from 'node:module'

process.env.GCLOUD_PROJECT = 'clickclack-rules'
process.env.FIRESTORE_EMULATOR_HOST ||= '127.0.0.1:8080'
process.env.FIREBASE_AUTH_EMULATOR_HOST ||= '127.0.0.1:9099'
const require = createRequire(new URL('../functions/index.js', import.meta.url))
const fns = require('./index.js')
const { getFirestore } = require('firebase-admin/firestore')
const { getAuth } = require('firebase-admin/auth')
const db = getFirestore()

const results = []
const check = (name, ok) => results.push([ok ? 'PASS' : 'FAIL', name])

async function seed() {
  await getAuth().createUser({ uid: 'alice' }).catch(() => {})
  const c = db.doc('couples/c1')
  await c.set({ members: ['alice', 'bob'], endedAt: null, hiddenTopics: { alice: ['MONEY'], bob: [] } })
  await db.doc('users/alice').set({ displayName: 'Alice', coupleId: 'c1' })
  await db.doc('users/bob').set({ displayName: 'Bob', coupleId: 'c1' })
  await c.collection('days').doc('2026-09-14').set({ revealed: true, notes: { alice: 'hi', bob: 'yo' } })
  await c.collection('days').doc('2026-09-14').collection('answers').doc('alice').set({ text: 'a' })
  await c.collection('days').doc('2026-09-14').collection('answers').doc('bob').set({ text: 'b' })
  await c.collection('instruments').doc('type').set({ revealed: true })
  await c.collection('instruments').doc('type').collection('runs').doc('alice').set({ r: [1] })
  await c.collection('chats').doc('day_x').set({ lastBy: 'alice', lastText: 'secret' })
  await c.collection('chats').doc('day_x').collection('messages').doc('m1').set({ byUid: 'alice', text: 'secret' })
  await c.collection('chats').doc('day_x').collection('messages').doc('m2').set({ byUid: 'bob', text: 'mine' })
  await c.collection('photos').doc('p1').set({ byUid: 'alice', data: 'x' })
  await c.collection('plates').doc('w1').set({ completedOn: '2026-09-13' })
  await c.collection('plates/w1/words').doc('alice').set({ describing: 'Wobbly', thing: 'Castle' })
  await c.collection('plates/w1/words').doc('bob').set({ describing: 'Cosmic', thing: 'Pretzel' })
  await db.doc('invites/ABC').set({ fromUid: 'alice' })
}

await seed()
await fns.deleteAccount.run({ auth: { uid: 'alice' }, data: {} })

const c = db.doc('couples/c1')
check('profile gone', !(await db.doc('users/alice').get()).exists)
check('auth user gone', await getAuth().getUser('alice').then(() => false, () => true))
check('her answer gone', !(await c.collection('days/2026-09-14/answers').doc('alice').get()).exists)
check('his answer kept', (await c.collection('days/2026-09-14/answers').doc('bob').get()).exists)
const day = (await c.collection('days').doc('2026-09-14').get()).data()
check('her note gone, his kept', !day.notes.alice && day.notes.bob === 'yo')
check('her test run gone', !(await c.collection('instruments/type/runs').doc('alice').get()).exists)
check('her message gone, his kept', !(await c.collection('chats/day_x/messages').doc('m1').get()).exists
  && (await c.collection('chats/day_x/messages').doc('m2').get()).exists)
check('chat preview of her text gone', (await c.collection('chats').doc('day_x').get()).get('lastText') === undefined)
check('her photo gone', !(await c.collection('photos').doc('p1').get()).exists)
check('her words gone, his kept', !(await c.collection('plates/w1/words').doc('alice').get()).exists
  && (await c.collection('plates/w1/words').doc('bob').get()).exists)
check('her invite gone', !(await db.doc('invites/ABC').get()).exists)
const couple = (await c.get()).data()
check('couple ended, her key stripped', couple.endedAt != null && !('alice' in couple.hiddenTopics))

// Bob deletes too: nothing of the couple remains.
await getAuth().createUser({ uid: 'bob' }).catch(() => {})
await fns.deleteAccount.run({ auth: { uid: 'bob' }, data: {} })
check('couple deleted once both are gone', !(await c.get()).exists
  && (await c.collection('chats/day_x/messages').get()).empty)

let threw = false
try { await fns.deleteAccount.run({ data: {} }) } catch { threw = true }
check('refuses without sign-in', threw)

for (const r of results) console.log(r[0].padEnd(5), r[1])
process.exit(results.some((r) => r[0] === 'FAIL') ? 1 : 0)
