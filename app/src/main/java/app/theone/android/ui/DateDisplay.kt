package app.theone.android.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val displayDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

/**
 * Formats a stored "yyyy-MM-dd" date key for display as "dd-MM-yyyy". Storage stays ISO so keys sort
 * correctly as strings (Firestore `orderBy("date")`, calendar range filters) - convert only at the UI.
 * Returns [dateKey] unchanged if it isn't a valid ISO date.
 */
fun formatDisplayDate(dateKey: String): String = try {
    LocalDate.parse(dateKey).format(displayDateFormatter)
} catch (e: DateTimeParseException) {
    dateKey
}
