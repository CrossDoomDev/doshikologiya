package ru.doshikologiya.nativepreview

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Android implementation of js/core/chronology.js.
 * Sort by actual publication instant. Recipes/news: later index wins on ties.
 * Patrons: earlier index wins on ties. Invalid dates always go last.
 */
internal object NativeChronology {
    private val legacy = Regex("""^(\d{2})\.(\d{2})\.(\d{4})(?:\s+(\d{1,2}):(\d{2}))?$""")
    private val iso = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d{1,3})?)?(?:Z|[+-]\d{2}:\d{2})$""")
    private val printer = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("ru-RU"))

    fun millis(raw: String): Long? {
        val value = raw.trim()
        legacy.matchEntire(value)?.let { m ->
            return try {
                val g = m.groupValues
                val year = g[3].toInt()
                if (year < 2000) return null
                LocalDateTime.of(
                    year, g[2].toInt(), g[1].toInt(),
                    g[4].ifBlank { "0" }.toInt(), g[5].ifBlank { "0" }.toInt()
                ).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: Exception) { null }
        }
        if (!iso.matches(value)) return null
        return try {
            java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) { null }
    }

    private fun <T> sorted(items: List<T>, newestOnTie: Boolean, date: (T) -> String): List<T> =
        items.withIndex().sortedWith(compareByDescending<IndexedValue<T>> {
            millis(date(it.value)) ?: Long.MIN_VALUE
        }.thenBy { if (newestOnTie) -it.index else it.index }).map { it.value }

    fun latestRecipes(items: List<Recipe>, limit: Int = 5) =
        sorted(items, true) { it.publishedAt }.take(limit)
    fun latestNews(items: List<NewsItem>, limit: Int = 3) =
        sorted(items, true) { it.date }.take(limit)
    fun newestPatrons(items: List<PatronItem>) =
        sorted(items, false) { it.date }

    fun formatDate(value: String): String {
        val timestamp = millis(value) ?: return "Дата не указана"
        return if (legacy.matches(value.trim())) {
            val groups = legacy.matchEntire(value.trim())!!.groupValues
            groups[1] + "." + groups[2] + "." + groups[3]
        } else {
            printer.withZone(java.time.ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(timestamp))
        }
    }
}
