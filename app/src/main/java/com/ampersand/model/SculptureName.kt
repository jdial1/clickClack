package com.ampersand.model

import kotlin.random.Random

/** The two words one of you picked to name a finished week. */
data class NameWords(val describing: String, val thing: String)

/**
 * Naming a sealed week, fill-in-the-blanks style: each of you picks a
 * describing word and a thing, and the four are merged into one name that
 * neither of you sees until both have picked.
 */
object SculptureNames {

    val Describing = listOf(
        "Wobbly", "Sleepy", "Brave", "Sparkly", "Grumpy", "Tiny", "Giant", "Secret", "Lucky", "Fuzzy",
        "Noisy", "Quiet", "Wild", "Cozy", "Spicy", "Soggy", "Golden", "Clumsy", "Fancy", "Midnight",
        "Lopsided", "Glorious", "Crunchy", "Sneaky", "Dizzy", "Mighty", "Humble", "Frosty", "Electric", "Velvet",
        "Rusty", "Jolly", "Stubborn", "Tender", "Chaotic", "Pocket", "Haunted", "Cosmic", "Wandering", "Sunday",
    )

    val Things = listOf(
        "Castle", "Waffle", "Dragon", "Tower", "Pancake", "Robot", "Llama", "Rocket", "Garden", "Sock",
        "Lighthouse", "Noodle", "Volcano", "Penguin", "Couch", "Bridge", "Taco", "Moose", "Teapot", "Cactus",
        "Submarine", "Pickle", "Owl", "Throne", "Bagel", "Walrus", "Canyon", "Kazoo", "Blanket", "Meatball",
        "Comet", "Burrito", "Squirrel", "Fortress", "Pretzel", "Goose", "Hammock", "Trombone", "Nugget", "Bathtub",
    )

    /** The words offered to [uid] for [plateId]: six of each, different for each of you, stable per week. */
    fun offered(plateId: String, uid: String, words: List<String>): List<String> =
        words.shuffled(Random("$plateId/$uid".hashCode())).take(Offered)

    /**
     * The week's name, once both have picked. [first] is the lower uid's words,
     * so both phones build the same name; the template is picked by the week.
     */
    fun name(plateId: String, first: NameWords, second: NameWords): String {
        val (a, b) = first to second
        val templates = listOf(
            "The ${a.describing} ${b.thing} and the ${b.describing} ${a.thing}",
            "${a.describing} ${a.thing} Meets the ${b.describing} ${b.thing}",
            "The ${b.describing} ${a.thing} of ${a.describing} ${b.thing}",
            "Attack of the ${a.describing} ${b.thing} and ${b.describing} ${a.thing}",
            "A ${b.describing} ${b.thing} for the ${a.describing} ${a.thing}",
        )
        return templates[plateId.hashCode().mod(templates.size)]
    }

    private const val Offered = 6
}

/** The name of this week, when both of you have picked your words. */
fun BuildPlate.name(uidA: String, uidB: String): String? {
    val (first, second) = if (uidA <= uidB) uidA to uidB else uidB to uidA
    return SculptureNames.name(id, words[first] ?: return null, words[second] ?: return null)
}
