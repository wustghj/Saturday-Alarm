package com.example.saturdayalarm.date

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Calculates the final Saturday of a calendar month without Android dependencies. */
object LastSaturdayCalculator {
    fun getLastSaturday(year: Int, month: Int): LocalDate {
        val lastDay = YearMonth.of(year, month).atEndOfMonth()
        val daysAfterSaturday = (lastDay.dayOfWeek.value - DayOfWeek.SATURDAY.value + 7) % 7
        return lastDay.minusDays(daysAfterSaturday.toLong())
    }
}
