package com.ampersand.model

/** "1 day", "3 days": a count and its noun, for nouns whose plural just adds an s. */
fun counted(n: Int, noun: String): String = "$n ${if (n == 1) noun else "${noun}s"}"
