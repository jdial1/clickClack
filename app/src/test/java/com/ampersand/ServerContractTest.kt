package com.ampersand

import com.ampersand.model.Mood
import com.ampersand.model.Together
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The server is JavaScript (`functions/index.js`) and the rules are their own
 * language (`firestore.rules`), so a few app rules necessarily exist twice. These
 * checks make the app's copy the authority: change one side alone and this fails.
 */
class ServerContractTest {

    private fun repoFile(path: String): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, path) }
            .first { it.exists() }

    private val functions by lazy { repoFile("functions/index.js").readText() }
    private val rules by lazy { repoFile("firestore.rules").readText() }

    /** Collections only the server writes or keeps, with no client code. */
    private val serverOnly = setOf("outbox", "chats", "messages")

    private val schema: Set<String> = FirestoreSchema::class.java.declaredFields
        .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == String::class.java }
        .map { it.get(null) as String }
        .toSet()

    @Test
    fun `push text uses the app's mood labels`() {
        val block = Regex("""const MOOD_LABELS = \{(.*?)\};""", RegexOption.DOT_MATCHES_ALL)
            .find(functions)!!.groupValues[1]
        val js = Regex("""(\w+): "([^"]+)"""").findAll(block).associate { it.groupValues[1] to it.groupValues[2] }
        assertEquals(Mood.entries.associate { it.name to it.label }, js)
    }

    @Test
    fun `anniversary pushes use the app's anniversary names`() {
        val block = Regex("""const NAMED = \{(.*?)\};""").find(functions)!!.groupValues[1]
        val js = Regex("""(\d+): "(\w+)"""").findAll(block).associate { it.groupValues[1].toInt() to it.groupValues[2] }
        assertEquals(Together.Named, js)
    }

    @Test
    fun `the functions run where the app calls them`() {
        assertTrue(
            "functions/index.js sets a region; update AppConfig.FUNCTIONS_REGION to match",
            "region" !in functions || "region: \"${AppConfig.FUNCTIONS_REGION}\"" in functions,
        )
    }

    @Test
    fun `every collection the server touches is in FirestoreSchema or known server-only`() {
        val named = Regex("""collection\("(\w+)"\)""").findAll(functions).map { it.groupValues[1] } +
            Regex("""/(\w+)/\{\w+\}""").findAll(functions).map { it.groupValues[1] } +
            Regex("""/(\w+)/\$\{""").findAll(functions).map { it.groupValues[1] } +
            Regex("""`(\w+)/\$\{""").findAll(functions).map { it.groupValues[1] }
        assertTrue("the scan found the couples tree", setOf(FirestoreSchema.COUPLES, FirestoreSchema.DAYS, FirestoreSchema.PLATES).all { it in named.toSet() })
        val unknown = named.toSet() - schema - serverOnly
        assertTrue("collections missing from FirestoreSchema: $unknown", unknown.isEmpty())
    }

    @Test
    fun `every collection the rules guard is in FirestoreSchema or known server-only`() {
        val guarded = Regex("""match /(\w+)/\{""").findAll(rules).map { it.groupValues[1] }.toSet() - "databases"
        assertTrue("the scan found the rules", setOf(FirestoreSchema.USERS, FirestoreSchema.COUPLES, FirestoreSchema.DAYS).all { it in guarded })
        val unknown = guarded - schema - serverOnly
        assertTrue("collections missing from FirestoreSchema: $unknown", unknown.isEmpty())
    }
}
