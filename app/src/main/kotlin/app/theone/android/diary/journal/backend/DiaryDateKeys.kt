package app.theone.android.diary.journal.backend

import java.time.LocalDate

/** Date-key ("yyyy-MM-dd") helpers: the stored form of a diary entry's date. */
internal object DiaryDateKeys {
    fun todayKey(): String = formatDateKey(LocalDate.now())

    /** ISO-8601 local date, i.e. "yyyy-MM-dd". */
    fun formatDateKey(date: LocalDate): String = date.toString()

    fun parseDateKey(dateKey: String): LocalDate = LocalDate.parse(dateKey)
}
