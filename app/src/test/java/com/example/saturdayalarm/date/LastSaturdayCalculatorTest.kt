package com.example.saturdayalarm.date

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.YearMonth
import java.time.ZonedDateTime

class LastSaturdayCalculatorTest {
    @Test
    fun calculatesRequestedMonthsAndYearBoundary() {
        assertEquals(LocalDate.of(2026, 10, 31), LastSaturdayCalculator.getLastSaturday(2026, 10))
        assertEquals(LocalDate.of(2026, 11, 28), LastSaturdayCalculator.getLastSaturday(2026, 11))
        assertEquals(LocalDate.of(2026, 12, 26), LastSaturdayCalculator.getLastSaturday(2026, 12))
        assertEquals(LocalDate.of(2027, 1, 30), LastSaturdayCalculator.getLastSaturday(2027, 1))
    }

    @Test
    fun handlesMonthsWithDifferentLengthsAndLeapYears() {
        assertEquals(LocalDate.of(2026, 2, 28), LastSaturdayCalculator.getLastSaturday(2026, 2))
        assertEquals(LocalDate.of(2028, 2, 26), LastSaturdayCalculator.getLastSaturday(2028, 2))
        assertEquals(LocalDate.of(2026, 4, 25), LastSaturdayCalculator.getLastSaturday(2026, 4))
        assertEquals(LocalDate.of(2026, 7, 25), LastSaturdayCalculator.getLastSaturday(2026, 7))
    }

    @Test
    fun returnsMonthEndWhenItIsSaturday() {
        assertEquals(LocalDate.of(2026, 10, 31), LastSaturdayCalculator.getLastSaturday(2026, 10))
    }

    @Test
    fun validatesMonthRangeThroughYearMonth() {
        listOf(0, 13).forEach { month ->
            try {
                LastSaturdayCalculator.getLastSaturday(2026, month)
                throw AssertionError("Expected invalid month $month to be rejected")
            } catch (_: java.time.DateTimeException) {
                // YearMonth rejects invalid month values.
            }
        }
    }

    @Test
    fun nextAlarmUsesCurrentMonthOnlyWhenItsTimeIsStillAhead() {
        val zone = ZoneId.of("Asia/Shanghai")
        val before = ZonedDateTime.of(2026, 10, 1, 8, 0, 0, 0, zone)
        assertEquals(
            ZonedDateTime.of(2026, 10, 31, 7, 30, 0, 0, zone),
            NextAlarmCalculator.getNext(before, LocalTime.of(7, 30)),
        )

        val after = ZonedDateTime.of(2026, 10, 31, 7, 30, 0, 0, zone)
        assertEquals(
            ZonedDateTime.of(2026, 11, 28, 7, 30, 0, 0, zone),
            NextAlarmCalculator.getNext(after, LocalTime.of(7, 30)),
        )
    }

    @Test
    fun firedOccurrenceDoesNotScheduleAgainInTheSameMonthAfterClockMovesBack() {
        val zone = ZoneId.of("Asia/Shanghai")
        val clockMovedBack = ZonedDateTime.of(2026, 10, 1, 8, 0, 0, 0, zone)
        assertEquals(
            ZonedDateTime.of(2026, 11, 28, 7, 30, 0, 0, zone),
            NextAlarmCalculator.getNext(
                clockMovedBack,
                LocalTime.of(7, 30),
                afterMonthExclusive = YearMonth.of(2026, 10),
            ),
        )
    }
}
