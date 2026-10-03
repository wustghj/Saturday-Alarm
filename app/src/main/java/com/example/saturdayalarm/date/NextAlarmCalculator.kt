package com.example.saturdayalarm.date

import java.time.LocalTime
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

object NextAlarmCalculator {
    /** Returns the next monthly alarm strictly after now, interpreted in now's current zone. */
    fun getNext(
        now: ZonedDateTime,
        time: LocalTime,
        afterMonthExclusive: YearMonth? = null,
    ): ZonedDateTime {
        val currentMonth = YearMonth.from(now)
        val firstMonth = afterMonthExclusive?.plusMonths(1)?.takeIf { it > currentMonth } ?: currentMonth
        val currentCandidate = atLocalAlarmTime(firstMonth, time, now.zone)
        if (currentCandidate.isAfter(now)) return currentCandidate

        return atLocalAlarmTime(firstMonth.plusMonths(1), time, now.zone)
    }

    private fun atLocalAlarmTime(
        month: YearMonth,
        time: LocalTime,
        zone: java.time.ZoneId,
    ): ZonedDateTime {
        val date = LastSaturdayCalculator.getLastSaturday(month.year, month.monthValue)
        // LocalDate.atTime(...).atZone(...) advances through a DST gap and picks the earlier
        // offset during an overlap, matching java.time's documented default resolution.
        return date.atTime(time).atZone(zone)
    }
}
