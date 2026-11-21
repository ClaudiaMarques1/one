package app.theone.android.ui

/**
 * Avatar initials: first letters of the name's first and last words ("Ada Lovelace" -> "AL", "Ada" -> "A"),
 * else the email's first letter, else "?".
 */
fun profileInitials(name: String, email: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val fromName = when (words.size) {
        0 -> ""
        1 -> words.first().take(1)
        else -> words.first().take(1) + words.last().take(1)
    }
    return fromName.ifEmpty { email.trim().take(1) }.ifEmpty { "?" }.uppercase()
}
