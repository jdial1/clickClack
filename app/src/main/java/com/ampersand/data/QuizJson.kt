package com.ampersand.data

import android.content.Context
import com.ampersand.model.Depth
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Quiz
import com.ampersand.model.QuizTheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * The wire format for quiz data.
 *
 * Quizzes arrive as JSON files, so this parser is the actual contract and the
 * shape below is the thing to write pool content against.
 *
 * ```json
 * {
 *   "id": "meals",
 *   "title": "What's for dinner",
 *   "blurb": "Ranking the food you argue about.",
 *   "theme": "FOOD",
 *   "questions": [
 *     {
 *       "id": "m1",
 *       "text": "Put these in order, favorite first.",
 *       "type": "RANK",
 *       "category": "Food",
 *       "depth": 1,
 *       "options": ["Roast", "Curry", "Pasta", "Something on toast"]
 *     },
 *     {
 *       "id": "m2",
 *       "text": "Friday night is:",
 *       "type": "THIS_OR_THAT",
 *       "depth": 1,
 *       "options": ["Takeaway", "Cook together"]
 *     }
 *   ]
 * }
 * ```
 *
 * Rules the parser enforces, because a bad deck should fail loudly at load
 * rather than quietly render an unanswerable question:
 *
 * - `id`, `text` and `type` are required; everything else has a default.
 * - `depth` defaults to 1 and is clamped to 1..3.
 * - `category` defaults to the deck's theme label.
 * - Choice types must carry `options`; a `THIS_OR_THAT` needs exactly two.
 * - `REFLECT` and `GUESS_ME` must **not** carry options.
 *
 * `org.json` rather than a serialisation library: it is in the platform, the
 * format is a dozen fields, and a dependency here would buy nothing.
 */
object QuizJson {

    /** Every deck bundled in `assets/quizzes/`. */
    fun loadBundled(context: Context): List<Quiz> = loadFolder(context, "quizzes")

    /** Every deck in an asset folder, in file order. */
    fun loadFolder(context: Context, folder: String): List<Quiz> {
        val names = context.assets.list(folder).orEmpty().filter { it.endsWith(".json") }
        return names.sorted().map { name ->
            runCatching {
                parse(context.assets.open("$folder/$name").bufferedReader().readText())
            }.getOrElse {
                throw IllegalArgumentException("$folder/$name is not a valid deck", it)
            }
        }
    }

    fun parse(json: String): Quiz = fromObject(JSONObject(json))

    /** A file holding a bare array is read as several decks. */
    fun parseAll(json: String): List<Quiz> {
        val trimmed = json.trimStart()
        return if (trimmed.startsWith("[")) {
            JSONArray(trimmed).let { arr -> (0 until arr.length()).map { fromObject(arr.getJSONObject(it)) } }
        } else {
            listOf(parse(json))
        }
    }

    private fun fromObject(o: JSONObject): Quiz {
        val theme = o.optString("theme").ifBlank { "EVERYDAY" }.let { name ->
            QuizTheme.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("unknown theme \"$name\"")
        }
        val id = o.required("id")
        val questions = o.optJSONArray("questions") ?: JSONArray()
        require(questions.length() > 0) { "deck \"$id\" has no questions" }

        return Quiz(
            id = id,
            title = o.required("title"),
            blurb = o.optString("blurb"),
            theme = theme,
            questions = (0 until questions.length()).map {
                question(questions.getJSONObject(it), theme)
            },
        )
    }

    private fun question(o: JSONObject, theme: QuizTheme): Question {
        val id = o.required("id")
        val typeName = o.required("type")
        val type = QuestionType.entries.firstOrNull { it.name.equals(typeName, ignoreCase = true) }
            ?: throw IllegalArgumentException("question \"$id\" has unknown type \"$typeName\"")

        val options = o.optJSONArray("options")
            ?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }
            .orEmpty()

        when (type) {
            QuestionType.REFLECT, QuestionType.GUESS_ME ->
                require(options.isEmpty()) { "question \"$id\" is $type and cannot have options" }
            QuestionType.THIS_OR_THAT ->
                require(options.size == 2) { "question \"$id\" is THIS_OR_THAT and needs exactly 2 options" }
            QuestionType.SCALE ->
                require(options.isEmpty()) { "question \"$id\" is SCALE; use lowLabel/highLabel" }
            else ->
                require(options.size >= 2) { "question \"$id\" is $type and needs at least 2 options" }
        }
        require(options.distinct().size == options.size) {
            // AmpDragRank identifies rows by their text, so duplicates make a
            // reorderable list swap the wrong rows.
            "question \"$id\" has duplicate options"
        }

        val max = if (o.has("maxChoices")) o.getInt("maxChoices") else null
        require(max == null || max in 1..options.size) {
            "question \"$id\" caps choices at $max but offers ${options.size}"
        }

        return Question(
            id = id,
            text = o.required("text"),
            type = type,
            category = o.optString("category").ifBlank { theme.label },
            depth = Depth(o.optInt("depth", 1).coerceIn(1, 3)),
            options = options,
            maxChoices = max,
            lowLabel = o.optString("lowLabel").ifBlank { "Not at all" },
            highLabel = o.optString("highLabel").ifBlank { "Completely" },
            tags = o.optJSONArray("tags")
                ?.let { arr -> (0 until arr.length()).map { arr.getString(it).trim().lowercase() } }
                .orEmpty()
                .filter { it.isNotEmpty() }
                .toSet(),
        )
    }

    private fun JSONObject.required(key: String): String {
        val value = optString(key)
        require(value.isNotBlank()) { "missing \"$key\"" }
        return value
    }
}
