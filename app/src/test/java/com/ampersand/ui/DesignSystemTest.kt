package com.ampersand.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * DESIGN_SYSTEM.md, pinned: gaps, corners, colours and type come from the theme.
 * The brick art draws to its own geometry and is out of scope, as is the theme
 * itself and the splash. A line that really must fit the art says `// course-fit`.
 */
class DesignSystemTest {

    private val root = listOf(File("src/main"), File("app/src/main")).first { it.exists() }

    private val lines: List<Pair<String, String>> =
        File(root, "java/com/ampersand/ui").walk()
            .filter { it.extension == "kt" }
            .filter { f -> listOf("theme", "brick", "splash").none { it == f.parentFile.name } }
            .filter { !it.name.endsWith("Art.kt") && it.name != "StudBoard.kt" }
            .flatMap { f -> f.readLines().map { "${f.name}: ${it.trim()}" to it } }
            .filter { (_, line) -> "// course-fit" !in line && !line.trim().startsWith("*") && !line.trim().startsWith("//") }
            .toList()

    private fun assertNone(what: String, pattern: String) {
        val regex = Regex(pattern)
        val bad = lines.filter { (_, line) -> regex.containsMatchIn(line) }.map { it.first }
        assertTrue("$what:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun `the scan is reading the UI`() {
        assertTrue("only ${lines.size} lines", lines.size > 5000)
    }

    @Test
    fun `gaps come from Space`() {
        assertNone("use Space", """Spacer\(Modifier\.(height|width)\([1-9]\d*\.dp""")
        assertNone("use Space", """spacedBy\([1-9]\d*\.dp""")
        assertNone("use Space", """padding\([1-9]\d*\.dp""")
        assertNone("use Space", """\b(horizontal|vertical) = [1-9]\d*\.dp""")
    }

    @Test
    fun `corners come from Radius`() {
        assertNone("use Radius or Corner", """RoundedCornerShape\(""")
        assertNone("use Corner", """\bcorner = [1-9]\d*\.dp""")
    }

    @Test
    fun `colours come from the theme`() {
        assertNone("use the theme's colours", """\bColor\(0x""")
    }

    @Test
    fun `text uses the type roles`() {
        assertNone("use a type role", """\bfontSize = """)
        assertNone(
            "not a type role in DESIGN_SYSTEM.md",
            """typography\.(displayLarge|displayMedium|headlineLarge|titleLarge|labelMedium)\b""",
        )
    }

    @Test
    fun `opacity comes from Alpha`() {
        assertNone("use Alpha", """\balpha\b.*\b0?\.\d+f""")
    }

    @Test
    fun `icon sizes come from IconSize`() {
        assertNone("use IconSize", """\bsize = [1-9]\d*\.dp""")
        assertNone("use IconSize or Space", """\.size\([1-9]\d*\.dp""")
    }

    @Test
    fun `panels come from the kit`() {
        assertNone("use Modifier.panel", """\.glass\(inkOnBrick\(structureSlug\(\)\)""")
        assertNone("use Emphasis", """\bstrength = \d""")
    }

    @Test
    fun `no screen file grows past 800 lines of real code`() {
        val big = File(root, "java/com/ampersand/ui").walk()
            .filter { it.extension == "kt" }
            .map { it.name to it.readLines().count { line -> !line.startsWith("import ") } }
            .filter { (_, n) -> n > 800 }
            .toList()
        assertTrue("split these by what the pieces are: $big", big.isEmpty())
    }

    @Test
    fun `controls come from the kit`() {
        assertNone("use BackAction or QuietAction", """\bTextButton\(""")
        assertNone("use AmpButton", """\b(Button|OutlinedButton|FilledTonalButton|ElevatedButton)\(""")
        assertNone("use BackAction", """IconAction\(\s*UiIcon\.BACK, "Back"""")
    }
}
