package ru.doshikologiya.nativepreview

import java.time.LocalDate

internal object DailyOracle {
    // JS concatenates unpadded month/day, not YYYYMMDD.
    fun seed(date: LocalDate) = "${date.year}${date.monthValue}${date.dayOfMonth}".toLong()
    fun index(date: LocalDate) = 55 + (seed(date) * 17 % 41).toInt()
    fun forecast(date: LocalDate) = (seed(date) % 4).toInt()
}
